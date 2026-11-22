package io.github.aidarnig.wgs.adb;

import com.android.apksig.ApkVerifier;
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import com.reandroid.arsc.chunk.xml.ResXmlAttribute;
import com.reandroid.arsc.chunk.xml.ResXmlElement;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Offline, local-only APK validation. Do not expose Path or raw parser exceptions to HTTP/logs. */
public final class ApkPreflight {
    public static final String BRIDGE_PACKAGE = "io.github.aidarnig.wristporter.bridge";
    private static final long MAX_BYTES = 150L * 1024 * 1024;
    private static final long MAX_MANIFEST_BYTES = 2L * 1024 * 1024;
    private static final int MAX_ZIP_ENTRIES = 4096;

    /** DEBUG is an explicitly approved *untrusted publisher* mode, not official identity. */
    public enum Mode { DEBUG, OFFICIAL }
    public enum Error { NOT_LOCAL_FILE, UNSUPPORTED_FORMAT, TOO_LARGE, MALFORMED_APK,
        UNSIGNED_OR_INVALID, WRONG_PACKAGE, UNTRUSTED_SIGNER, INVALID_VERSION, IO_ERROR }
    public static final class Rejected extends Exception {
        private final Error error;
        Rejected(Error error) { super(safeMessage(error)); this.error = error; }
        public Error error() { return error; }
    }
    public record Policy(Mode mode, Set<String> officialSignerSha256) {
        public Policy {
            Objects.requireNonNull(mode);
            officialSignerSha256 = Set.copyOf(Objects.requireNonNull(officialSignerSha256));
            if (mode == Mode.OFFICIAL && officialSignerSha256.isEmpty())
                throw new IllegalArgumentException("Official signer fingerprint must be approved and supplied");
            for (String fingerprint : officialSignerSha256) {
                if (!fingerprint.matches("[0-9A-Fa-f]{64}")) throw new IllegalArgumentException("Invalid fingerprint format");
            }
        }
        public static Policy explicitlyAllowDebug() { return new Policy(Mode.DEBUG, Set.of()); }
    }
    /** Immutable local contract. SHA-256 binds the snapshot used by the installer to inspected bytes. */
    public record Inspected(Path path, long size, String fileSha256, String packageName,
                            long versionCode, String versionName, int minSdk,
                            String signerSha256, boolean testOnly, Mode mode) {}

    public Inspected inspect(Path path, Policy policy) throws Rejected {
        Objects.requireNonNull(policy);
        if (path == null || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(path)) throw new Rejected(Error.NOT_LOCAL_FILE);
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".apk")) throw new Rejected(Error.UNSUPPORTED_FORMAT);
        try {
            long size = Files.size(path);
            if (size == 0 || size > MAX_BYTES) throw new Rejected(Error.TOO_LARGE);
            AndroidManifestBlock manifest;
            try (ZipFile zip = new ZipFile(path.toFile())) {
                if (zip.size() > MAX_ZIP_ENTRIES) throw new Rejected(Error.MALFORMED_APK);
                ZipEntry entry = zip.getEntry("AndroidManifest.xml");
                if (entry == null || entry.isDirectory() || entry.getSize() > MAX_MANIFEST_BYTES)
                    throw new Rejected(Error.MALFORMED_APK);
                // Bounded even if the ZIP declares a fraudulent uncompressed size.
                try (InputStream data = zip.getInputStream(entry)) {
                    byte[] bytes = data.readNBytes((int) MAX_MANIFEST_BYTES + 1);
                    if (bytes.length > MAX_MANIFEST_BYTES || bytes.length < 8)
                        throw new Rejected(Error.MALFORMED_APK);
                    manifest = AndroidManifestBlock.load(new java.io.ByteArrayInputStream(bytes));
                }
            }
            if (manifest.getManifestElement() == null || manifest.isSplit())
                throw new Rejected(Error.UNSUPPORTED_FORMAT);
            String packageName = manifest.getPackageName();
            if (!BRIDGE_PACKAGE.equals(packageName)) throw new Rejected(Error.WRONG_PACKAGE);
            Integer version = manifest.getVersionCode();
            Integer minSdk = manifest.getMinSdkVersion();
            if (version == null || version < 1 || minSdk == null || minSdk < 1)
                throw new Rejected(Error.INVALID_VERSION);
            // Verification is cryptographic and spans supported Android platform versions.
            ApkVerifier.Result verified = new ApkVerifier.Builder(path.toFile()).build().verify();
            if (!verified.isVerified() || verified.getSignerCertificates().size() != 1)
                throw new Rejected(Error.UNSIGNED_OR_INVALID);
            String signer = digest(verified.getSignerCertificates().getFirst().getEncoded());
            if (policy.mode() == Mode.OFFICIAL && policy.officialSignerSha256().stream()
                    .noneMatch(pin -> pin.equalsIgnoreCase(signer))) throw new Rejected(Error.UNTRUSTED_SIGNER);
            ResXmlElement application = manifest.getApplicationElement();
            ResXmlAttribute flag = application == null ? null : application.searchAttributeByName("testOnly");
            boolean testOnly = flag != null && flag.getValueAsBoolean();
            return new Inspected(path.toAbsolutePath().normalize(), size, sha256File(path), packageName,
                    version.longValue(), manifest.getVersionName(), minSdk, signer, testOnly, policy.mode());
        } catch (Rejected e) {
            throw e;
        } catch (Exception e) {
            // No private path, package parser content, or stack trace in user-facing errors.
            throw new Rejected(Error.MALFORMED_APK);
        }
    }

    static String sha256File(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            MessageDigest md = sha256();
            byte[] bytes = new byte[32 * 1024];
            for (int read; (read = input.read(bytes)) >= 0; ) if (read > 0) md.update(bytes, 0, read);
            return HexFormat.of().withUpperCase().formatHex(md.digest());
        }
    }
    private static String digest(byte[] bytes) {
        return HexFormat.of().withUpperCase().formatHex(sha256().digest(bytes));
    }
    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private static String safeMessage(Error error) {
        return switch (error) {
            case NOT_LOCAL_FILE -> "Select an existing local APK file";
            case UNSUPPORTED_FORMAT -> "Only a single base .apk is supported";
            case TOO_LARGE -> "APK is empty or exceeds size limit";
            case MALFORMED_APK -> "APK metadata or archive is invalid";
            case UNSIGNED_OR_INVALID -> "APK signature verification failed";
            case WRONG_PACKAGE -> "APK is not WristPorter Bridge";
            case UNTRUSTED_SIGNER -> "APK signer is not an approved release signer";
            case INVALID_VERSION -> "APK version or minimum Android version is invalid";
            case IO_ERROR -> "Could not read the APK";
        };
    }
}