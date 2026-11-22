package io.github.aidarnig.wgs.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class LanUploadGatewayTest {
    private final MutableClock clock = new MutableClock();
    private final HttpClient client = HttpClient.newHttpClient();
    private final AtomicReference<String> lease = new AtomicReference<>("watch-lease-1");
    private LanUploadGateway gateway;
    private LanUploadGateway.OwnerAccess access;

    private static Inet4Address loopback() throws Exception {
        return (Inet4Address) InetAddress.getByName("127.0.0.1");
    }

    private void start() throws Exception {
        gateway = new LanUploadGateway(clock, address -> address.isLoopbackAddress(), new SecureRandom());
        access = gateway.start(request(true, true));
    }

    private LanUploadGateway.StartRequest request(boolean trusted, boolean plaintext) throws Exception {
        return new LanUploadGateway.StartRequest(loopback(), "watch-lease-1", lease::get,
                true, trusted, plaintext);
    }

    private HttpResponse<String> get(String route) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(access.url() + route))
                .GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String route, String body, String origin,
                                       String cookie, String csrf) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(access.url() + route))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (origin != null) b.header("Origin", origin);
        if (cookie != null) b.header("Cookie", cookie);
        if (csrf != null) b.header("X-WP-CSRF", csrf);
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postForm(String body, String cookie, String requestOrigin,
                                          String fetchSite) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(access.url() + "session"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (cookie != null) b.header("Cookie", cookie);
        if (requestOrigin != null) b.header("Origin", requestOrigin);
        if (fetchSite != null) b.header("Sec-Fetch-Site", fetchSite);
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String formNonce(HttpResponse<String> page) {
        Matcher found = Pattern.compile("name=\\\"formNonce\\\" value=\\\"([0-9a-f]{32})\\\"")
                .matcher(page.body());
        assertTrue(found.find(), "Expected nonce in the genuine pairing page");
        return found.group(1);
    }

    private String origin() {
        return access.url().substring(0, access.url().length() - 1);
    }

    @AfterEach void cleanup() {
        if (gateway != null) gateway.stop();
    }

    @Test void offByDefaultAndExplicitPrivateNetworkConsent() throws Exception {
        gateway = new LanUploadGateway(clock, address -> address.isLoopbackAddress(), new SecureRandom());
        assertFalse(gateway.isRunning());
        assertThrows(IllegalArgumentException.class, () -> gateway.start(request(false, true)));
        assertThrows(IllegalArgumentException.class, () -> gateway.start(request(true, false)));
        assertFalse(gateway.isRunning());
        assertThrows(IllegalArgumentException.class, () -> new LanUploadGateway().start(request(true, true)),
                "Production policy must reject localhost even when user consents");
        lease.set("different-watch");
        assertThrows(IllegalArgumentException.class, () -> gateway.start(request(true, true)));
        lease.set("watch-lease-1");
        access = gateway.start(request(true, true));
        assertTrue(gateway.isRunning());
        assertEquals(200, get("status").statusCode());
        assertTrue(access.plaintextWarning().contains("UNENCRYPTED"));
        assertFalse(access.url().contains(access.oneTimePin()));
    }

    @Test void exactRoutesAndSecurityHeadersDoNotExposePrivateData() throws Exception {
        start();
        HttpResponse<String> root = get("");
        assertEquals(200, root.statusCode());
        assertTrue(root.body().contains("test gateway only"));
        assertFalse(root.body().contains(access.oneTimePin()));
        assertFalse(root.body().contains("watch-lease"));
        assertEquals("no-store", root.headers().firstValue("Cache-Control").orElseThrow());
        assertEquals("nosniff", root.headers().firstValue("X-Content-Type-Options").orElseThrow());
        assertTrue(root.headers().firstValue("Content-Security-Policy").orElseThrow()
                .contains("default-src 'none'"));
        assertEquals(404, get("uploads").statusCode());
        assertEquals(404, get("session/fake").statusCode());
        assertEquals(404, get("status/extra").statusCode());
        assertEquals(400, get("status?token=123").statusCode());
        assertEquals(405, client.send(
                HttpRequest.newBuilder(URI.create(access.url() + "status"))
                        .POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString()).statusCode());
    }

    @Test void hostAndOriginAreStrict() throws Exception {
        start();
        URI uri = URI.create(access.url());
        try (Socket socket = new Socket(uri.getHost(), uri.getPort())) {
            socket.setSoTimeout(2500);
            OutputStream out = socket.getOutputStream();
            out.write(("GET / HTTP/1.1\r\nHost: attacker.example\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
            out.flush();
            String status = new BufferedReader(new InputStreamReader(socket.getInputStream())).readLine();
            assertNotNull(status);
            assertTrue(status.contains("421"), status);
        }
        assertEquals(403, post("session", access.oneTimePin(), null, null, null).statusCode());
        assertEquals(403, post("session", access.oneTimePin(), "https://attacker.example",
                null, null).statusCode());
        assertEquals(200, post("session", access.oneTimePin(), origin(), null, null).statusCode());
    }

    @Test void safariNativeFormWithoutOriginUsesCookieAndNonce() throws Exception {
        start();
        HttpResponse<String> page = get("");
        String nonce = formNonce(page);
        String cookie = page.headers().firstValue("Set-Cookie").orElseThrow();
        assertTrue(cookie.startsWith("WPFORM="));
        assertTrue(cookie.contains("HttpOnly; SameSite=Strict"));
        assertFalse(page.body().contains(access.oneTimePin()));

        String form = "pin=" + access.oneTimePin() + "&formNonce=" + nonce;
        assertEquals(403, postForm(form, null, null, "same-origin").statusCode(),
                "No form cookie, even with a valid hidden token");
        assertEquals(403, postForm("pin=" + access.oneTimePin()
                + "&formNonce=" + "0".repeat(32), cookie, null, "same-origin").statusCode());
        assertEquals(403, postForm(form, cookie, "http://evil.example", "same-origin").statusCode());
        assertEquals(403, postForm(form, cookie, "null", "same-origin").statusCode());
        assertEquals(403, postForm(form, cookie, null, "cross-site").statusCode());
        HttpResponse<String> paired = postForm(form, cookie, null, "same-origin");
        assertEquals(200, paired.statusCode());
        assertTrue(paired.body().contains("Paired (test only)"));
        assertEquals(403, postForm(form, cookie, null, "same-origin").statusCode(),
                "Successful pairing consumes PIN and form nonce");
        assertTrue(paired.headers().allValues("Set-Cookie").stream()
                .anyMatch(value -> value.startsWith("WPSESSION=")));
    }

    @Test void missingOriginCannotBypassExpiredFormOrAuthenticatedCsrf() throws Exception {
        start();
        HttpResponse<String> page = get("");
        String token = formNonce(page);
        String cookie = page.headers().firstValue("Set-Cookie").orElseThrow();
        clock.advanceSeconds(121);
        assertEquals(403, postForm("pin=" + access.oneTimePin()
                + "&formNonce=" + token, cookie, null, "same-origin").statusCode());

        gateway.stop();
        start();
        page = get("");
        token = formNonce(page);
        cookie = page.headers().firstValue("Set-Cookie").orElseThrow();
        HttpResponse<String> paired = postForm("pin=" + access.oneTimePin()
                + "&formNonce=" + token, cookie, null, null);
        assertEquals(200, paired.statusCode(), "Legacy Safari without Fetch Metadata still pairs");
        String sessionCookie = paired.headers().allValues("Set-Cookie").stream()
                .filter(value -> value.startsWith("WPSESSION=")).findFirst().orElseThrow();
        String csrf = paired.headers().firstValue("X-WP-CSRF").orElseThrow();
        assertEquals(403, post("session/logout", "", null, sessionCookie, null).statusCode());
        assertEquals(200, post("session/logout", "", null, sessionCookie, csrf).statusCode());
    }

    @Test void oneTimePinAndSessionCookieCsrfLogoutAndReplay() throws Exception {
        start();
        HttpResponse<String> paired = post("session", access.oneTimePin(), origin(), null, null);
        assertEquals(200, paired.statusCode());
        String cookie = paired.headers().firstValue("Set-Cookie").orElseThrow();
        String csrf = paired.headers().firstValue("X-WP-CSRF").orElseThrow();
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Strict"));
        assertFalse(cookie.contains("Secure"), "HTTP does not provide a secure channel");
        assertFalse(cookie.contains(access.oneTimePin()));
        assertEquals(403, post("session", access.oneTimePin(), origin(), null, null).statusCode());
        assertEquals(403, post("session/logout", "", origin(), cookie, null).statusCode());
        assertEquals(403, post("session/logout", "", origin(), cookie, "forged").statusCode());
        assertEquals(403, post("session/logout", "", "https://attacker.example", cookie, csrf).statusCode());
        assertEquals(200, post("session/logout", "", origin(), cookie, csrf).statusCode());
        assertEquals(403, post("session/logout", "", origin(), cookie, csrf).statusCode());
    }

    @Test void pinExpiresFailuresLockOutAndSessionExpires() throws Exception {
        start();
        clock.advanceSeconds(121);
        assertEquals(403, post("session", access.oneTimePin(), origin(), null, null).statusCode());
        gateway.stop();
        start();
        for (int i = 0; i < 3; i++) {
            assertEquals(403, post("session", "AAAAAAAA", origin(), null, null).statusCode());
        }
        assertEquals(403, post("session", access.oneTimePin(), origin(), null, null).statusCode());
        gateway.stop();
        start();
        HttpResponse<String> paired = post("session", access.oneTimePin(), origin(), null, null);
        String cookie = paired.headers().firstValue("Set-Cookie").orElseThrow();
        String csrf = paired.headers().firstValue("X-WP-CSRF").orElseThrow();
        clock.advanceSeconds(601);
        assertEquals(403, post("session/logout", "", origin(), cookie, csrf).statusCode());
    }

    @Test void forgedCookieOversizedBodyAndWatchLeaseChangeFailClosed() throws Exception {
        start();
        assertEquals(413, post("session", "A".repeat(65), origin(), null, null).statusCode());
        HttpResponse<String> paired = post("session", access.oneTimePin(), origin(), null, null);
        assertEquals(200, paired.statusCode());
        String csrf = paired.headers().firstValue("X-WP-CSRF").orElseThrow();
        assertEquals(403, post("session/logout", "", origin(), "WPSESSION=forged", csrf).statusCode());
        lease.set("another-watch");
        assertEquals(403, get("status").statusCode());
        assertEquals(403, post("session/logout", "", origin(),
                paired.headers().firstValue("Set-Cookie").orElseThrow(), csrf).statusCode());
    }

    @Test void stopReleasesPortAndInvalidatesAllAccess() throws Exception {
        start();
        URI uri = URI.create(access.url());
        assertEquals(200, get("status").statusCode());
        gateway.stop();
        assertFalse(gateway.isRunning());
        assertThrows(Exception.class, () -> get("status"));
        try (Socket socket = new Socket()) {
            assertThrows(Exception.class, () -> socket.connect(
                    new java.net.InetSocketAddress(uri.getHost(), uri.getPort()), 800));
        }
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-22T12:00:00Z");

        void advanceSeconds(long seconds) { now = now.plusSeconds(seconds); }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
