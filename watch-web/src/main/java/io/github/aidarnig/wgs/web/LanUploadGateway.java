package io.github.aidarnig.wgs.web;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * W14-B: isolated, manually started HTTP authorization probe. Not wired into Desktop or ADB.
 * Deliberately has no upload, filesystem, watch selection or management routes.
 *
 * HTTP is PLAINTEXT: tokens, PINs and later photos are observable on the local network.
 * The Windows owner must confirm a trusted private LAN before invoking start().
 */
public final class LanUploadGateway implements AutoCloseable {
    public static final String PLAINTEXT_WARNING =
            "UNENCRYPTED HTTP: anyone who can observe this network may read or change PINs, "
            + "sessions and future photographs. Use only a private network you trust.";
    private static final Duration PIN_TTL = Duration.ofMinutes(2);
    private static final Duration SESSION_TTL = Duration.ofMinutes(10);
    private static final Duration FAIL_WINDOW = Duration.ofMinutes(5);
    private static final int MAX_FAILURES = 3;
    private static final int MAX_PENDING_FORMS = 32;
    private static final Pattern PIN_FORM = Pattern.compile("pin=([A-Za-z2-7]{8})&formNonce=([0-9a-f]{32})");
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final String HTML = """
            <!doctype html><html lang="en"><head><meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <title>WristPorter Gateway Test</title></head><body>
            <h1>WristPorter — test gateway only</h1>
            <p>No photo upload or device controls are available in this build.</p>
            <p>HTTP traffic is not encrypted. Only use a trusted private LAN.</p>
            <form method="post" action="/session">
            <label>One-time PIN shown on Windows <input name="pin" minlength="8"
              maxlength="8" autocomplete="off" required></label>
            <input type="hidden" name="formNonce" value="__FORM_NONCE__">
            <button type="submit">Pair (test only)</button></form></body></html>
            """;

    // HttpServer reads these JVM properties on initialization. No server is constructed
    // during normal Desktop startup. They are defense-in-depth, not a substitute for W16
    // slow-client/connection-flood verification on the actual bundled JDK.
    static {
        System.setProperty("jdk.httpserver.maxConnections", "16");
        System.setProperty("sun.net.httpserver.maxReqTime", "10");
        System.setProperty("sun.net.httpserver.maxRspTime", "10");
        System.setProperty("sun.net.httpserver.maxIdleConnections", "4");
        System.setProperty("sun.net.httpserver.maxReqHeaders", "40");
        System.setProperty("sun.net.httpserver.maxHeaderSize", "8192");
    }

    /** A lease supplier must be thread-safe. Its result is never sent to a web client. */
    public record StartRequest(Inet4Address bindAddress, String selectedWatchLease,
                               Supplier<String> currentWatchLease,
                               boolean watchReady, boolean trustedLanConfirmed,
                               boolean plaintextWarningAccepted) {}

    /** Show the PIN only on the Windows owner surface; encode ONLY url in any QR. */
    public record OwnerAccess(String url, String oneTimePin, Instant pinExpiresAt,
                              String plaintextWarning) {}

    private record Session(String secret, String csrf, Instant expiresAt, String watchLease) {}

    private final Clock clock;
    private final Predicate<Inet4Address> addressPolicy;
    private final SecureRandom random;
    private final Map<String, List<Instant>> failuresByPeer = new HashMap<>();
    private final List<Instant> globalFailures = new ArrayList<>();
    private final Map<String, Instant> pendingForms = new LinkedHashMap<>();

    private HttpServer server;
    private ThreadPoolExecutor executor;
    private StartRequest startedWith;
    private String url;
    private String pin;
    private Instant pinExpiresAt;
    private Session session;

    public LanUploadGateway() {
        this(Clock.systemUTC(), LanUploadGateway::allowedPrivateAdapter, new SecureRandom());
    }

    // Package-private loopback test seam; production constructor always applies adapter checks.
    LanUploadGateway(Clock clock, Predicate<Inet4Address> addressPolicy, SecureRandom random) {
        this.clock = Objects.requireNonNull(clock);
        this.addressPolicy = Objects.requireNonNull(addressPolicy);
        this.random = Objects.requireNonNull(random);
    }

    public synchronized OwnerAccess start(StartRequest request) throws IOException {
        Objects.requireNonNull(request, "request");
        if (server != null) throw new IllegalStateException("Gateway already started");
        if (request.bindAddress() == null || !addressPolicy.test(request.bindAddress())) {
            throw new IllegalArgumentException("Select an active, non-VPN private IPv4 adapter");
        }
        if (!request.trustedLanConfirmed() || !request.plaintextWarningAccepted()) {
            throw new IllegalArgumentException("Explicit trusted-LAN and plaintext consent required");
        }
        if (!request.watchReady() || request.selectedWatchLease() == null
                || request.selectedWatchLease().isBlank()
                || request.currentWatchLease() == null
                || !request.selectedWatchLease().equals(request.currentWatchLease().get())) {
            throw new IllegalArgumentException("A ready and unchanged Windows-selected watch is required");
        }

        HttpServer created = HttpServer.create(new InetSocketAddress(request.bindAddress(), 0), 0);
        ThreadPoolExecutor workers = new ThreadPoolExecutor(
                2, 2, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(8), task -> {
                    Thread thread = new Thread(task, "wristporter-web-gateway");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
        try {
            created.createContext("/", this::handle);
            created.setExecutor(workers);
            startedWith = request;
            url = "http://" + request.bindAddress().getHostAddress() + ":" + created.getAddress().getPort();
            pin = randomBase32(8);
            pinExpiresAt = clock.instant().plus(PIN_TTL);
            session = null;
            failuresByPeer.clear();
            globalFailures.clear();
            pendingForms.clear();
            created.start();
            executor = workers;
            server = created;
            return new OwnerAccess(url + "/", pin, pinExpiresAt, PLAINTEXT_WARNING);
        } catch (RuntimeException failure) {
            created.stop(0);
            workers.shutdownNow();
            clearSecrets();
            throw failure;
        }
    }

    public synchronized boolean isRunning() {
        return server != null;
    }

    /** Revokes access immediately. Call when adapter/watch selection or desktop session changes. */
    public void stop() {
        HttpServer toStop;
        ThreadPoolExecutor workers;
        synchronized (this) {
            toStop = server;
            workers = executor;
            server = null;
            executor = null;
            clearSecrets();
        }
        if (toStop != null) toStop.stop(0);
        if (workers != null) workers.shutdownNow();
    }

    @Override public void close() {
        stop();
    }

    private synchronized void clearSecrets() {
        startedWith = null;
        url = null;
        pin = null;
        pinExpiresAt = null;
        session = null;
        globalFailures.clear();
        failuresByPeer.clear();
        pendingForms.clear();
    }

    private static boolean allowedPrivateAdapter(Inet4Address address) {
        byte[] b = address.getAddress();
        int first = b[0] & 255, second = b[1] & 255;
        boolean rfc1918 = first == 10 || (first == 172 && second >= 16 && second <= 31)
                || (first == 192 && second == 168);
        if (!rfc1918 || address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isMulticastAddress()) return false;
        try {
            NetworkInterface nic = NetworkInterface.getByInetAddress(address);
            if (nic == null || !nic.isUp() || nic.isLoopback() || nic.isVirtual()) return false;
            String name = (nic.getName() + " " + nic.getDisplayName()).toLowerCase(Locale.ROOT);
            for (String blocked : List.of("tun", "tap", "vpn", "wireguard", "tailscale",
                    "zerotier", "docker", "veth", "vmware", "virtualbox", "vbox", "wsl", "hyper-v")) {
                if (name.contains(blocked)) return false;
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean watchLeaseValid() {
        StartRequest snapshot;
        synchronized (this) { snapshot = startedWith; }
        boolean valid = snapshot != null && addressPolicy.test(snapshot.bindAddress())
                && snapshot.selectedWatchLease().equals(snapshot.currentWatchLease().get());
        if (!valid) {
            synchronized (this) { session = null; pin = null; }
        }
        return valid;
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            addSecurityHeaders(exchange.getResponseHeaders());
            if (!validHost(exchange)) { reply(exchange, 421, "Invalid host"); return; }
            if (!watchLeaseValid()) { reply(exchange, 403, "Sharing revoked"); return; }
            if (exchange.getRequestURI().getRawQuery() != null) {
                reply(exchange, 400, "Query strings are forbidden"); return;
            }
            String path = exchange.getRequestURI().getRawPath();
            String method = exchange.getRequestMethod();
            if ("/".equals(path) && "GET".equals(method)) {
                String formNonce = issueFormNonce(exchange.getResponseHeaders());
                reply(exchange, 200, HTML.replace("__FORM_NONCE__", formNonce),
                        "text/html; charset=utf-8");
            } else if ("/status".equals(path) && "GET".equals(method)) {
                reply(exchange, 200, "test-only: ready\n");
            } else if ("/session".equals(path) && "POST".equals(method)) {
                authenticate(exchange);
            } else if ("/session/logout".equals(path) && "POST".equals(method)) {
                if (!originNotForged(exchange) || !authorized(exchange, true)) {
                    reply(exchange, 403, "Unauthorized"); return;
                }
                synchronized (this) { session = null; }
                exchange.getResponseHeaders().add("Set-Cookie",
                        "WPSESSION=; Max-Age=0; HttpOnly; SameSite=Strict; Path=/");
                reply(exchange, 200, "Logged out");
            } else if (List.of("/", "/status", "/session", "/session/logout").contains(path)) {
                reply(exchange, 405, "Method not allowed");
            } else {
                reply(exchange, 404, "Not found");
            }
        } catch (RuntimeException failure) {
            // Never include sensitive data or exception details in network responses.
            reply(exchange, 500, "Internal error");
        } finally {
            exchange.close();
        }
    }

    private boolean validHost(HttpExchange exchange) {
        String expected;
        synchronized (this) { expected = url == null ? "" : url.substring("http://".length()); }
        return oneHeaderEquals(exchange.getRequestHeaders(), "Host", expected);
    }

    // A missing Origin is possible for some Safari HTML form submissions.
    // Explicit mismatched or opaque Origin is NEVER accepted, even with a valid nonce.
    // A missing Origin requires the short-lived one-page cookie + hidden form token.
    private boolean originNotForged(HttpExchange exchange) {
        List<String> values = exchange.getRequestHeaders().get("Origin");
        String expected;
        synchronized (this) { expected = url; }
        if (expected == null) return false;
        if (values != null) return values.size() == 1 && expected.equals(values.getFirst());
        List<String> fetchSite = exchange.getRequestHeaders().get("Sec-Fetch-Site");
        return fetchSite == null || (fetchSite.size() == 1
                && ("same-origin".equals(fetchSite.getFirst()) || "none".equals(fetchSite.getFirst())));
    }

    private boolean hasMatchingOrigin(HttpExchange exchange) {
        String expected;
        synchronized (this) { expected = url; }
        return expected != null && oneHeaderEquals(exchange.getRequestHeaders(), "Origin", expected);
    }

    // A pre-auth form cookie and nonce are never put in the QR, URL or logs.
    // Both are required on requests without Origin. The token is page-scoped
    // and short-lived; invalid-origin requests fail before pairing is attempted.
    private synchronized String issueFormNonce(Headers headers) {
        Instant now = clock.instant();
        pendingForms.entrySet().removeIf(entry -> !now.isBefore(entry.getValue()));
        if (pendingForms.size() >= MAX_PENDING_FORMS) {
            String oldest = pendingForms.keySet().iterator().next();
            pendingForms.remove(oldest);
        }
        String nonce = randomHex(16);
        pendingForms.put(nonce, now.plus(PIN_TTL));
        headers.add("Set-Cookie", "WPFORM=" + nonce + "; HttpOnly; SameSite=Strict; Path=/");
        return nonce;
    }

    private synchronized boolean validFormNonce(HttpExchange exchange, String nonce) {
        if (nonce == null) return false;
        String cookie = readCookie(exchange.getRequestHeaders(), "WPFORM");
        Instant expiry = pendingForms.get(nonce);
        return cookie != null && expiry != null && clock.instant().isBefore(expiry)
                && constantTimeEquals(cookie, nonce);
    }

    private static String readCookie(Headers headers, String name) {
        List<String> all = headers.get("Cookie");
        if (all == null || all.size() != 1) return null;
        String found = null;
        for (String element : all.getFirst().split(";")) {
            String entry = element.trim();
            if (entry.startsWith(name + "=")) {
                if (found != null) return null;
                found = entry.substring(name.length() + 1);
            }
        }
        return found;
    }

    private static boolean oneHeaderEquals(Headers headers, String name, String expected) {
        List<String> values = headers.get(name);
        return values != null && values.size() == 1 && expected.equals(values.getFirst());
    }

    private void authenticate(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getRequestHeaders();
        String type = headers.getFirst("Content-Type");
        if (type == null || headers.get("Content-Type").size() != 1
                || (!type.equalsIgnoreCase("text/plain")
                && !type.toLowerCase(Locale.ROOT).matches("application/x-www-form-urlencoded(?:;\\s*charset=utf-8)?"))) {
            reply(exchange, 415, "Unsupported request"); return;
        }
        String length = headers.getFirst("Content-Length");
        if (length != null && headers.get("Content-Length").size() != 1) {
            reply(exchange, 400, "Invalid request length"); return;
        }
        if (length != null) {
            try {
                if (Long.parseLong(length) < 0 || Long.parseLong(length) > 64) { reply(exchange, 413, "PIN body too large"); return; }
            } catch (NumberFormatException bad) {
                reply(exchange, 400, "Invalid request length"); return;
            }
        }
        byte[] bytes = exchange.getRequestBody().readNBytes(65);
        if (bytes.length > 64) { reply(exchange, 413, "PIN body too large"); return; }
        String submitted = new String(bytes, StandardCharsets.UTF_8);
        String formNonce = null;
        if (type.toLowerCase(Locale.ROOT).startsWith("application/x-www-form-urlencoded")) {
            Matcher parsed = PIN_FORM.matcher(submitted);
            if (!parsed.matches()) { reply(exchange, 400, "Malformed PIN form"); return; }
            submitted = parsed.group(1);
            formNonce = parsed.group(2);
        }
        if (!originNotForged(exchange)) {
            // Test-only non-sensitive diagnostic: classify the failing policy,
            // never echo actual header values, URLs, PINs or cookies.
            List<String> origins = exchange.getRequestHeaders().get("Origin");
            String reason;
            if (origins == null) reason = "FETCH_METADATA";
            else if (origins.size() != 1) reason = "DUPLICATE_ORIGIN";
            else if ("null".equals(origins.getFirst())) reason = "OPAQUE_ORIGIN";
            else reason = "ORIGIN_MISMATCH";
            reply(exchange, 403, "Origin rejected (" + reason + ")"); return;
        }
        if (!hasMatchingOrigin(exchange) && !validFormNonce(exchange, formNonce)) {
            reply(exchange, 403, "Origin rejected (FORM_PROOF)"); return;
        }
        String peer = exchange.getRemoteAddress().getAddress().getHostAddress();
        boolean granted = false;
        String csrf = null, cookie = null;
        synchronized (this) {
            Instant now = clock.instant();
            globalFailures.removeIf(instant -> !instant.plus(FAIL_WINDOW).isAfter(now));
            failuresByPeer.computeIfAbsent(peer, key -> new ArrayList<>())
                    .removeIf(instant -> !instant.plus(FAIL_WINDOW).isAfter(now));
            if (failuresByPeer.size() > 256) failuresByPeer.clear();
            List<Instant> peerFailures = failuresByPeer.get(peer);
            boolean locked = globalFailures.size() >= MAX_FAILURES
                    || peerFailures.size() >= MAX_FAILURES;
            if (!locked && session == null && pin != null && now.isBefore(pinExpiresAt)
                    && submitted.matches("[A-Z2-7]{8}")
                    && constantTimeEquals(pin, submitted)) {
                cookie = randomHex(32);
                csrf = randomHex(32);
                session = new Session(cookie, csrf, now.plus(SESSION_TTL),
                        startedWith.selectedWatchLease());
                pin = null; // consumed: cannot pair a second browser
                if (formNonce != null) pendingForms.remove(formNonce);
                granted = true;
            } else if (!locked) {
                globalFailures.add(now);
                peerFailures.add(now);
                if (globalFailures.size() >= MAX_FAILURES) pin = null; // fail closed
            }
        }
        if (!granted) {
            reply(exchange, 403, "Invalid, expired or locked PIN"); return;
        }
        exchange.getResponseHeaders().add("Set-Cookie",
                "WPSESSION=" + cookie + "; HttpOnly; SameSite=Strict; Path=/");
        exchange.getResponseHeaders().add("Set-Cookie",
                "WPFORM=; Max-Age=0; HttpOnly; SameSite=Strict; Path=/");
        exchange.getResponseHeaders().add("X-WP-CSRF", csrf);
        reply(exchange, 200, "Paired (test only); no upload endpoint is enabled");
    }

    private boolean authorized(HttpExchange exchange, boolean requireCsrf) {
        List<String> cookies = exchange.getRequestHeaders().get("Cookie");
        if (cookies == null || cookies.size() != 1) return false;
        String candidate = null;
        for (String part : cookies.getFirst().split(";")) {
            String value = part.trim();
            if (value.startsWith("WPSESSION=")) {
                if (candidate != null) return false;
                candidate = value.substring("WPSESSION=".length());
            }
        }
        Session snapshot;
        synchronized (this) {
            snapshot = session;
            if (snapshot != null && !clock.instant().isBefore(snapshot.expiresAt())) {
                session = null;
                snapshot = null;
            }
        }
        if (candidate == null || snapshot == null
                || !constantTimeEquals(candidate, snapshot.secret())) return false;
        if (requireCsrf) {
            List<String> tokens = exchange.getRequestHeaders().get("X-WP-CSRF");
            return tokens != null && tokens.size() == 1
                    && constantTimeEquals(tokens.getFirst(), snapshot.csrf());
        }
        return true;
    }

    private String randomBase32(int count) {
        byte[] b = new byte[count];
        random.nextBytes(b);
        StringBuilder sb = new StringBuilder(count);
        for (byte value : b) sb.append(ALPHABET.charAt((value & 255) & 31));
        Arrays.fill(b, (byte) 0);
        return sb.toString();
    }

    private String randomHex(int countBytes) {
        byte[] b = new byte[countBytes];
        random.nextBytes(b);
        StringBuilder sb = new StringBuilder(countBytes * 2);
        for (byte value : b) sb.append(String.format("%02x", value & 255));
        Arrays.fill(b, (byte) 0);
        return sb.toString();
    }

    private static boolean constantTimeEquals(String actual, String expected) {
        return MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    private static void addSecurityHeaders(Headers h) {
        h.set("Cache-Control", "no-store");
        h.set("X-Content-Type-Options", "nosniff");
        // For native HTML form POST, no-referrer can turn Origin into literal
        // "null" on Safari (Fetch/referrer-policy semantics). same-origin sends
        // referrer information only to this same server; no cross-site leakage.
        h.set("Referrer-Policy", "same-origin");
        h.set("X-Frame-Options", "DENY");
        h.set("Cross-Origin-Resource-Policy", "same-origin");
        h.set("Content-Security-Policy",
                "default-src 'none'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'");
    }

    private static void reply(HttpExchange exchange, int code, String body) throws IOException {
        reply(exchange, code, body, "text/plain; charset=utf-8");
    }

    private static void reply(HttpExchange exchange, int code, String body, String type)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", type);
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
