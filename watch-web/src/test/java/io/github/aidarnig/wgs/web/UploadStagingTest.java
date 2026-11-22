package io.github.aidarnig.wgs.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class UploadStagingTest {
    @TempDir Path temp;
    private final MutableClock clock = new MutableClock();
    private final AtomicReference<String> lease = new AtomicReference<>("selected-watch");
    private final HttpClient client = HttpClient.newHttpClient();
    private LanUploadGateway gateway;
    private LanUploadGateway.OwnerAccess access;
    private String cookie;
    private String csrf;

    @AfterEach void stop() {
        if (gateway != null) gateway.stop();
    }

    private void startAndPair() throws Exception {
        Path root = temp.resolve("staging");
        gateway = new LanUploadGateway(clock, addr -> addr.isLoopbackAddress(),
                new SecureRandom(), root);
        assertFalse(gateway.isRunning(), "OFF until explicit start");
        var request = new LanUploadGateway.StartRequest(
                (Inet4Address) InetAddress.getByName("127.0.0.1"),
                "selected-watch", lease::get, true, true, true);
        access = gateway.start(request);
        var paired = client.send(HttpRequest.newBuilder(URI.create(access.url() + "session"))
                .header("Origin", origin())
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(access.oneTimePin())).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, paired.statusCode());
        cookie = paired.headers().allValues("Set-Cookie").stream()
                .filter(v -> v.startsWith("WPSESSION=")).findFirst().orElseThrow();
        csrf = paired.headers().firstValue("X-WP-CSRF").orElseThrow();
    }

    private String origin() { return access.url().substring(0, access.url().length() - 1); }

    private HttpResponse<String> upload(byte[] bytes, String mime,
                                         String session, String token, String requestOrigin)
            throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(access.url() + "uploads"))
                .header("Content-Type", mime).POST(HttpRequest.BodyPublishers.ofByteArray(bytes));
        if (session != null) b.header("Cookie", session);
        if (token != null) b.header("X-WP-CSRF", token);
        if (requestOrigin != null) b.header("Origin", requestOrigin);
        return client.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(access.url() + path))
                .header("Origin", origin())
                .header("Cookie", cookie)
                .header("X-WP-CSRF", csrf)
                .POST(HttpRequest.BodyPublishers.noBody()).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private static String id(HttpResponse<String> response) {
        Matcher matcher = Pattern.compile("Staged ([0-9a-f]{32}) image/(?:png|jpeg)")
                .matcher(response.body());
        assertEquals(202, response.statusCode(), response.body());
        assertTrue(matcher.matches(), response.body());
        return matcher.group(1);
    }

    private static byte[] image(String format) throws IOException {
        BufferedImage img = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        img.setRGB(1, 1, 0x336699);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(img, format, out));
        return out.toByteArray();
    }

    @Test void exactRawBytesMetadataAndPrivateFileNames() throws Exception {
        startAndPair();
        for (String type : new String[]{"png", "jpeg"}) {
            byte[] raw = image(type);
            var sent = upload(raw, type.equals("png") ? "image/png" : "image/jpeg",
                    cookie, csrf, origin());
            String id = id(sent);
            var staged = gateway.staged(id).orElseThrow();
            assertArrayEquals(raw, Files.readAllBytes(staged.path()), "No recompression/EXIF stripping");
            assertEquals(raw.length, staged.size());
            assertEquals(3, staged.width());
            assertEquals(2, staged.height());
            assertEquals(type.equals("png") ? "image/png" : "image/jpeg", staged.mime());
            assertTrue(staged.path().getFileName().toString().matches(
                    "wp-[0-9a-f]{32}\\.(png|jpg)"));
            assertTrue(staged.path().startsWith(temp.resolve("staging")));
            assertTrue(gateway.completeUpload(id));
            assertFalse(Files.exists(staged.path()));
            assertFalse(gateway.completeUpload(id));
        }
        if (Files.getFileStore(temp).supportsFileAttributeView("posix")) {
            assertEquals(PosixFilePermissions.fromString("rwx------"),
                    Files.getPosixFilePermissions(temp.resolve("staging")));
        }
    }

    @Test void noAuthenticationNoOriginNoCsrfNoCrossOriginUploads() throws Exception {
        startAndPair();
        byte[] png = image("png");
        assertEquals(403, upload(png, "image/png", null, null, origin()).statusCode());
        assertEquals(403, upload(png, "image/png", cookie, null, origin()).statusCode());
        assertEquals(403, upload(png, "image/png", cookie, csrf, "http://attacker.invalid").statusCode());
        assertEquals(403, upload(png, "image/png", "WPSESSION=forged", csrf, origin()).statusCode());
        assertEquals(403, upload(png, "image/png", cookie, "forged", origin()).statusCode());
        assertEquals(0, stagedFiles());
    }

    @Test void rejectsSpoofedTypeBadSignatureHeicIncompleteImageAndOversize() throws Exception {
        startAndPair();
        byte[] png = image("png");
        assertEquals(415, upload(png, "image/jpeg", cookie, csrf, origin()).statusCode());
        assertEquals(415, upload(png, "image/heic", cookie, csrf, origin()).statusCode());
        assertEquals(415, upload(png, "multipart/form-data", cookie, csrf, origin()).statusCode());
        assertEquals(422, upload(new byte[]{1,2,3,4,5,6,7,8}, "image/png",
                cookie, csrf, origin()).statusCode());
        assertEquals(422, upload(Arrays.copyOf(png, png.length - 3), "image/png",
                cookie, csrf, origin()).statusCode());
        assertEquals(422, upload(oversizedPixelHeader(png), "image/png",
                cookie, csrf, origin()).statusCode());
        assertEquals(413, upload(new byte[(int) UploadStaging.MAX_FILE_BYTES + 1],
                "image/png", cookie, csrf, origin()).statusCode());
        assertEquals(0, stagedFiles());
        assertEquals(202, upload(png, "application/octet-stream",
                cookie, csrf, origin()).statusCode());
    }

    @Test void supportsChunkedBodyAndIgnoresClientFilename() throws Exception {
        startAndPair();
        byte[] png = image("png");
        var response = client.send(HttpRequest.newBuilder(URI.create(access.url() + "uploads"))
                        .header("Origin", origin())
                        .header("Cookie", cookie)
                        .header("X-WP-CSRF", csrf)
                        .header("Content-Type", "application/octet-stream")
                        .header("X-File-Name", "../../secrets.png")
                        .POST(HttpRequest.BodyPublishers.ofInputStream(
                                () -> new ByteArrayInputStream(png))).build(),
                HttpResponse.BodyHandlers.ofString());
        var staged = gateway.staged(id(response)).orElseThrow();
        assertArrayEquals(png, Files.readAllBytes(staged.path()));
        assertFalse(Files.exists(temp.resolve("secrets.png")));
        var escaped = client.send(HttpRequest.newBuilder(
                        URI.create(access.url() + "uploads/%2e%2e/secrets"))
                        .header("Origin", origin()).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(404, escaped.statusCode());
    }

    @Test void cancelLogoutLeaseRevocationAndStopDeleteEverything() throws Exception {
        startAndPair();
        String first = id(upload(image("png"), "image/png", cookie, csrf, origin()));
        var p = gateway.staged(first).orElseThrow().path();
        assertEquals(200, post("jobs/" + first + "/cancel").statusCode());
        assertFalse(Files.exists(p));
        assertEquals(404, post("jobs/" + first + "/cancel").statusCode());
        String second = id(upload(image("png"), "image/png", cookie, csrf, origin()));
        var q = gateway.staged(second).orElseThrow().path();
        assertEquals(200, post("session/logout").statusCode());
        assertFalse(Files.exists(q));
        assertEquals(403, upload(image("png"), "image/png", cookie, csrf, origin()).statusCode());
        gateway.stop();
        startAndPair();
        String third = id(upload(image("png"), "image/png", cookie, csrf, origin()));
        var t = gateway.staged(third).orElseThrow().path();
        lease.set("different-watch");
        assertEquals(403, upload(image("png"), "image/png", cookie, csrf, origin()).statusCode());
        assertFalse(Files.exists(t));
        lease.set("selected-watch");
        gateway.stop();
        assertEquals(0, stagedFiles());
    }

    @Test void idleSessionExpirationSweepsWithoutAnyIncomingRequest() throws Exception {
        startAndPair();
        String id = id(upload(image("png"), "image/png", cookie, csrf, origin()));
        Path path = gateway.staged(id).orElseThrow().path();
        clock.advanceSeconds(601);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (Files.exists(path) && System.nanoTime() < deadline) Thread.sleep(30);
        assertFalse(Files.exists(path), "The background janitor must purge idle expired sessions");
        assertTrue(gateway.staged(id).isEmpty());
    }

    @Test void restartSweepsOnlyOwnedOrphansAndFailsClosedOnSymlinkedRoot() throws Exception {
        Path root = temp.resolve("staging");
        Files.createDirectories(root);
        Path orphan = root.resolve("wp-" + "a".repeat(32) + ".part");
        Path privateOther = root.resolve("unrelated.txt");
        Files.writeString(orphan, "private image residue");
        Files.writeString(privateOther, "not ours");
        startAndPair();
        assertFalse(Files.exists(orphan));
        assertTrue(Files.exists(privateOther));
        gateway.stop();

        Path alias = temp.resolve("alias");
        try {
            Files.createSymbolicLink(alias, root);
        } catch (UnsupportedOperationException | IOException | SecurityException unavailable) {
            assumeTrue(false, "Symlink creation unavailable on this runner");
        }
        var stage = new UploadStaging(alias, new SecureRandom());
        assertThrows(IOException.class, stage::open, "Never follow a link into another staging dir");
        assertTrue(Files.exists(privateOther));
    }

    @Test void smallInjectedLimitsCoverFileCountAndByteQuota() throws Exception {
        byte[] png = image("png");
        var s = new UploadStaging(temp.resolve("quotas"), new SecureRandom(),
                new UploadStaging.Limits(png.length + 10, png.length * 2L + 1,
                        png.length * 2L + 1, 20, 2));
        s.open();
        try {
            var one = s.stage(new ByteArrayInputStream(png), "image/png", (long) png.length);
            var two = s.stage(new ByteArrayInputStream(png), "image/png", (long) png.length);
            UploadStaging.Rejected error = assertThrows(UploadStaging.Rejected.class,
                    () -> s.stage(new ByteArrayInputStream(png), "image/png", (long) png.length));
            assertEquals(413, error.status());
            assertTrue(s.release(one.id()));
            assertEquals(413, assertThrows(UploadStaging.Rejected.class,
                    () -> s.stage(new ByteArrayInputStream(png), "image/png",
                            (long) png.length)).status(), "Session quota is cumulative");
            s.clear();
            assertTrue(s.find(two.id()).isEmpty());
            assertNotNull(s.stage(new ByteArrayInputStream(png), "image/png",
                    (long) png.length));
        } finally {
            s.close();
        }
        var fileCount = new UploadStaging(temp.resolve("count"), new SecureRandom(),
                new UploadStaging.Limits(png.length + 10, png.length * 10L,
                        png.length * 10L, 1, 2));
        fileCount.open();
        try {
            fileCount.stage(new ByteArrayInputStream(png), "image/png", (long) png.length);
            assertEquals(413, assertThrows(UploadStaging.Rejected.class,
                    () -> fileCount.stage(new ByteArrayInputStream(png), "image/png",
                            (long) png.length)).status());
        } finally {
            fileCount.close();
        }
    }

    @Test void simultaneousUploadsAreBoundedAndCancellationInvalidatesInflight() throws Exception {
        byte[] png = image("png");
        var s = new UploadStaging(temp.resolve("parallel"), new SecureRandom());
        s.open();
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch resume = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Throwable> task = () -> {
                try {
                    InputStream input = new ByteArrayInputStream(png) {
                        private boolean waited;
                        @Override public synchronized int read(byte[] b, int off, int len) {
                            if (!waited) {
                                waited = true;
                                entered.countDown();
                                try { resume.await(5, TimeUnit.SECONDS); }
                                catch (InterruptedException e) { Thread.currentThread().interrupt(); return -1; }
                            }
                            return super.read(b, off, len);
                        }
                    };
                    s.stage(input, "image/png", (long) png.length);
                    return null;
                } catch (Throwable t) { return t; }
            };
            var first = pool.submit(task);
            var second = pool.submit(task);
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            var tooMany = assertThrows(UploadStaging.Rejected.class,
                    () -> s.stage(new ByteArrayInputStream(png), "image/png", (long) png.length));
            assertEquals(429, tooMany.status());
            s.clear();
            resume.countDown();
            assertInstanceOf(UploadStaging.Rejected.class, first.get(6, TimeUnit.SECONDS));
            assertInstanceOf(UploadStaging.Rejected.class, second.get(6, TimeUnit.SECONDS));
            try (var listing = Files.list(temp.resolve("parallel"))) {
                assertEquals(0, listing.count());
            }
        } finally {
            resume.countDown();
            pool.shutdownNow();
            s.close();
        }
    }

    private long stagedFiles() throws IOException {
        try (var list = Files.list(temp.resolve("staging"))) {
            return list.filter(path -> path.getFileName().toString().startsWith("wp-")).count();
        }
    }

    private static byte[] oversizedPixelHeader(byte[] png) {
        byte[] copy = png.clone();
        ByteBuffer.wrap(copy, 16, 8).putInt(10_000).putInt(10_000);
        CRC32 crc = new CRC32();
        crc.update(copy, 12, 17);
        ByteBuffer.wrap(copy, 29, 4).putInt((int) crc.getValue());
        return copy;
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-11-22T12:00:00Z");
        synchronized void advanceSeconds(long seconds) { instant = instant.plusSeconds(seconds); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public synchronized Instant instant() { return instant; }
    }
}
