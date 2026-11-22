package io.github.aidarnig.wgs.web;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * W14-C: local-only, session-scoped raw-byte staging. No client paths, ADB,
 * image rewriting, public download route, or background desktop listener.
 */
public final class UploadStaging implements AutoCloseable {
    public static final long MAX_FILE_BYTES = 25L * 1024 * 1024;
    public static final int MAX_FILES = 20;
    public static final long MAX_SESSION_BYTES = 250L * 1024 * 1024;
    public static final long MAX_TOTAL_BYTES = 500L * 1024 * 1024;
    public static final long MAX_PIXELS = 50_000_000L;
    private static final int MAX_ACTIVE = 2;
    private static final Pattern OWN_FILE = Pattern.compile("wp-[0-9a-f]{32}\\.(?:part|jpg|png)");
    private static final Pattern ID = Pattern.compile("[0-9a-f]{32}");
    private static final byte[] PNG = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
    private static final byte[] PNG_END = {0, 0, 0, 0, 73, 69, 78, 68, (byte) 174, 66, 96, (byte) 130};

    /** Only trusted local Java code may access this path; never serialize the path to HTTP. */
    public record StagedUpload(String id, Path path, String mime, long size, int width, int height) {}
    public static final class Rejected extends IOException {
        private final int status;
        Rejected(int status, String safeMessage) { super(safeMessage); this.status = status; }
        public int status() { return status; }
    }

    // Injectable bounds allow tiny deterministic quota tests without writing 250 MiB.
    record Limits(long file, long session, long total, int files, int concurrent) {
        Limits {
            if (file <= 0 || session < file || total < session || files <= 0 || concurrent <= 0)
                throw new IllegalArgumentException("Invalid staging limits");
        }
    }
    private final Path root;
    private final SecureRandom random;
    private final Limits limits;
    private final Map<String, StagedUpload> staged = new LinkedHashMap<>();
    private volatile boolean open;
    private volatile int generation;
    private int active;
    private long reservedBytes;
    private long stagedBytes;

    public static Path defaultDirectory() {
        String local = System.getenv("LOCALAPPDATA");
        if (local != null && !local.isBlank()) {
            return Path.of(local, "WristPorter", "WebUpload", "staging");
        }
        // The production caller is Windows; this fallback also supports headless tests.
        return Path.of(System.getProperty("user.home"), ".wristporter", "web-upload", "staging");
    }

    public UploadStaging(Path root, SecureRandom random) {
        this(root, random, new Limits(MAX_FILE_BYTES, MAX_SESSION_BYTES, MAX_TOTAL_BYTES,
                MAX_FILES, MAX_ACTIVE));
    }

    UploadStaging(Path root, SecureRandom random, Limits limits) {
        this.root = Objects.requireNonNull(root).toAbsolutePath().normalize();
        this.random = Objects.requireNonNull(random);
        this.limits = Objects.requireNonNull(limits);
    }

    /** Sweep only our recognizable abandoned files; reject symlinked directories. */
    public synchronized void open() throws IOException {
        if (open) throw new IllegalStateException("Staging already open");
        checkParents();
        Files.createDirectories(root);
        checkParents();
        restrictAccess();
        sweep();
        staged.clear();
        stagedBytes = 0;
        reservedBytes = 0;
        active = 0;
        generation++;
        open = true;
    }

    private void checkParents() throws IOException {
        for (Path p = root; p != null; p = p.getParent()) {
            if (Files.isSymbolicLink(p)) throw new IOException("Unsafe staging path");
            if (Files.exists(p, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isDirectory(p, LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Unsafe staging path");
            }
        }
    }

    private void restrictAccess() throws IOException {
        if (Files.getFileStore(root).supportsFileAttributeView("posix")) {
            Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
        }
        AclFileAttributeView acl = Files.getFileAttributeView(
                root, AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (acl != null) {
            AclEntry ownerOnly = AclEntry.newBuilder()
                    .setType(AclEntryType.ALLOW)
                    .setPrincipal(Files.getOwner(root, LinkOption.NOFOLLOW_LINKS))
                    .setPermissions(AclEntryPermission.values())
                    .setFlags(AclEntryFlag.FILE_INHERIT, AclEntryFlag.DIRECTORY_INHERIT)
                    .build();
            acl.setAcl(java.util.List.of(ownerOnly));
        }
    }

    private void sweep() throws IOException {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(root)) {
            for (Path file : files) {
                if (Files.isSymbolicLink(file)
                        || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IOException("Unsafe staging directory entry");
                }
                if (OWN_FILE.matcher(file.getFileName().toString()).matches()) {
                    Files.delete(file);
                }
            }
        }
    }

    public StagedUpload stage(InputStream input, String declaredMime, Long declaredLength)
            throws IOException {
        Objects.requireNonNull(input);
        if (!Set.of("image/jpeg", "image/png", "application/octet-stream").contains(declaredMime)) {
            throw new Rejected(415, "Only JPEG or PNG is supported");
        }
        if (declaredLength != null && (declaredLength <= 0 || declaredLength > limits.file())) {
            throw new Rejected(413, "Image exceeds file limit");
        }
        long reserve = declaredLength == null ? limits.file() : declaredLength;
        final int myGeneration;
        synchronized (this) {
            if (!open) throw new Rejected(409, "Staging is not available");
            if (active >= limits.concurrent()) throw new Rejected(429, "Too many active uploads");
            if (staged.size() + active >= limits.files()
                    || stagedBytes + reservedBytes + reserve > limits.session()
                    || stagedBytes + reservedBytes + reserve > limits.total()) {
                throw new Rejected(413, "Staging quota exceeded");
            }
            active++;
            reservedBytes += reserve;
            myGeneration = generation;
        }

        String id = randomId();
        Path pending = root.resolve("wp-" + id + ".part");
        Path committedPath = null;
        boolean committed = false;
        try {
            // CREATE_NEW is atomic; NOFOLLOW prevents opening a planted link.
            try (OutputStream out = Channels.newOutputStream(Files.newByteChannel(
                    pending, Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE,
                            LinkOption.NOFOLLOW_LINKS)))) {
                byte[] buffer = new byte[32 * 1024];
                long size = 0;
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (count == 0) continue;
                    if (!open || generation != myGeneration) {
                        throw new Rejected(409, "Upload was cancelled");
                    }
                    size += count;
                    if (size > limits.file() || size > reserve) {
                        throw new Rejected(413, "Image exceeds file limit");
                    }
                    out.write(buffer, 0, count);
                }
            }
            long size = Files.size(pending);
            if (size == 0 || (declaredLength != null && size != declaredLength)) {
                throw new Rejected(422, "Incomplete image");
            }
            Format format = inspect(pending, declaredMime);
            committedPath = root.resolve("wp-" + id + format.extension());
            Files.move(pending, committedPath, StandardCopyOption.ATOMIC_MOVE);
            StagedUpload result = new StagedUpload(
                    id, committedPath, format.mime(), size, format.width(), format.height());
            synchronized (this) {
                if (!open || generation != myGeneration) {
                    throw new Rejected(409, "Upload was cancelled");
                }
                staged.put(id, result);
                stagedBytes += size;
                committed = true;
            }
            return result;
        } finally {
            synchronized (this) {
                active--;
                reservedBytes -= reserve;
            }
            Files.deleteIfExists(pending);
            if (!committed && committedPath != null) Files.deleteIfExists(committedPath);
        }
    }

    private record Format(String extension, String mime, int width, int height) {}

    private static Format inspect(Path path, String claimedMime) throws IOException {
        byte[] first;
        try (InputStream in = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            first = in.readNBytes(8);
        }
        boolean isPng = Arrays.equals(first, PNG);
        boolean isJpeg = first.length >= 3 && (first[0] & 255) == 255
                && (first[1] & 255) == 216 && (first[2] & 255) == 255;
        if (!isPng && !isJpeg) throw new Rejected(422, "Invalid image signature");
        String detected = isPng ? "image/png" : "image/jpeg";
        if (!claimedMime.equals("application/octet-stream") && !detected.equals(claimedMime)) {
            throw new Rejected(415, "Image MIME does not match content");
        }
        // Ensure the encoded stream has an end marker, not just a spoofed header.
        long length = Files.size(path);
        byte[] tail = new byte[isPng ? PNG_END.length : 2];
        if (length < first.length + tail.length) throw new Rejected(422, "Incomplete image");
        try (var in = java.nio.channels.FileChannel.open(path, StandardOpenOption.READ,
                LinkOption.NOFOLLOW_LINKS)) {
            in.position(length - tail.length);
            java.nio.ByteBuffer b = java.nio.ByteBuffer.wrap(tail);
            while (b.hasRemaining() && in.read(b) > 0) {}
            if (b.hasRemaining()) throw new Rejected(422, "Incomplete image");
        }
        if (isPng ? !Arrays.equals(tail, PNG_END)
                : (tail[0] & 255) != 255 || (tail[1] & 255) != 217) {
            throw new Rejected(422, "Incomplete image");
        }

        try (ImageInputStream source = new FileImageInputStream(path.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(source);
            if (!readers.hasNext()) throw new Rejected(422, "Image cannot be decoded");
            ImageReader reader = readers.next();
            try {
                reader.setInput(source, false, true);
                String readerFormat = reader.getFormatName();
                if (!(isPng ? "png".equalsIgnoreCase(readerFormat)
                        : "jpeg".equalsIgnoreCase(readerFormat))) {
                    throw new Rejected(422, "Image decoder format mismatch");
                }
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw new Rejected(422, "Image dimensions exceed safety limit");
                }
                // Validate actual image data with bounded output pixels, no rewriting.
                ImageReadParam params = reader.getDefaultReadParam();
                int stride = Math.max(1, (int) Math.ceil(
                        Math.sqrt((double) width * height / 1_000_000)));
                params.setSourceSubsampling(stride, stride, 0, 0);
                if (reader.read(0, params) == null) {
                    throw new Rejected(422, "Image cannot be decoded");
                }
                return new Format(isPng ? ".png" : ".jpg", detected, width, height);
            } finally {
                reader.dispose();
            }
        } catch (Rejected e) {
            throw e;
        } catch (RuntimeException | IOException invalid) {
            throw new Rejected(422, "Malformed image body");
        }
    }

    /** Internal Java API only, for the future transfer coordinator. */
    public synchronized Optional<StagedUpload> find(String id) {
        if (!open || id == null || !ID.matcher(id).matches()) return Optional.empty();
        return Optional.ofNullable(staged.get(id));
    }

    /** Called after downstream success or explicit cancel; idempotent. */
    public synchronized boolean release(String id) throws IOException {
        StagedUpload item = find(id).orElse(null);
        if (item == null) return false;
        Files.delete(item.path());
        staged.remove(id);
        stagedBytes -= item.size();
        return true;
    }

    /** Revoke the session, invalidate in-flight requests and delete staged files. */
    public synchronized void clear() throws IOException {
        generation++;
        IOException failed = null;
        for (StagedUpload item : staged.values()) {
            try { Files.deleteIfExists(item.path()); }
            catch (IOException e) { failed = e; }
        }
        staged.clear();
        stagedBytes = 0;
        if (failed != null) {
            open = false;  // fail closed rather than exceed a silent disk quota
            throw new IOException("Unable to clean staging", failed);
        }
    }

    @Override public synchronized void close() throws IOException {
        try { clear(); }
        finally { open = false; }
    }

    private String randomId() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return java.util.HexFormat.of().formatHex(bytes);
    }
}
