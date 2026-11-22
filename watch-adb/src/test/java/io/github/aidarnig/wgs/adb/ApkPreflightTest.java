package io.github.aidarnig.wgs.adb;

import com.reandroid.arsc.chunk.xml.AndroidManifestBlock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.jupiter.api.Assertions.*;

/** Generated synthetic package/certificate only: never contains user data or release secrets. */
class ApkPreflightTest {
    @TempDir Path dir;
    private final ApkPreflight preflight = new ApkPreflight();

    private Path fixture(String pkg, String file) throws Exception {
        AndroidManifestBlock manifest = new AndroidManifestBlock();
        manifest.setPackageName(pkg);
        manifest.setVersionCode(3);
        manifest.setVersionName("0.0.3-test");
        manifest.setMinSdkVersion(21);
        manifest.getOrCreateApplicationElement();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        manifest.writeBytes(bytes);
        Path apk = dir.resolve(file);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(apk))) {
            zip.putNextEntry(new ZipEntry("AndroidManifest.xml"));
            zip.write(bytes.toByteArray());
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("classes.dex"));
            zip.write(new byte[]{0x64, 0x65, 0x78, 0x0a});
            zip.closeEntry();
        }
        return apk;
    }
    private Path signedFixture(String pkg, String file) throws Exception {
        Path apk = fixture(pkg, file);
        Path keyStore = dir.resolve("test-key.jks");
        Path javaBin = Path.of(System.getProperty("java.home"), "bin");
        Process keytool = new ProcessBuilder(javaBin.resolve("keytool").toString(), "-genkeypair",
                "-alias", "temporary-test-key", "-keyalg", "RSA", "-keysize", "2048", "-validity", "2",
                "-keystore", keyStore.toString(), "-storepass", "changeit", "-keypass", "changeit",
                "-dname", "CN=Synthetic test only", "-noprompt").redirectErrorStream(true).start();
        assertEquals(0, keytool.waitFor(), new String(keytool.getInputStream().readAllBytes()));
        Process signer = new ProcessBuilder(javaBin.resolve("jarsigner").toString(), "-keystore", keyStore.toString(),
                "-storepass", "changeit", "-keypass", "changeit", apk.toString(), "temporary-test-key")
                .redirectErrorStream(true).start();
        assertEquals(0, signer.waitFor(), new String(signer.getInputStream().readAllBytes()));
        return apk;
    }
    @Test void signedSyntheticManifestAndCertificateAreVerified() throws Exception {
        Path signed = signedFixture(ApkPreflight.BRIDGE_PACKAGE, "test app 日本語.apk");
        ApkPreflight.Inspected result = preflight.inspect(signed, ApkPreflight.Policy.explicitlyAllowDebug());
        assertEquals(ApkPreflight.BRIDGE_PACKAGE, result.packageName());
        assertEquals(3L, result.versionCode());
        assertEquals(21, result.minSdk());
        assertEquals(64, result.signerSha256().length());
        assertEquals(64, result.fileSha256().length());
        assertEquals(ApkPreflight.Mode.DEBUG, result.mode());
        assertEquals(result.signerSha256(), preflight.inspect(signed,
                new ApkPreflight.Policy(ApkPreflight.Mode.OFFICIAL, Set.of(result.signerSha256()))).signerSha256());
        assertEquals(ApkPreflight.Error.UNTRUSTED_SIGNER,
                assertThrows(ApkPreflight.Rejected.class, () -> preflight.inspect(signed,
                        new ApkPreflight.Policy(ApkPreflight.Mode.OFFICIAL, Set.of("0".repeat(64))))).error());
    }
    @Test void wrongPackageRejectedAfterParsing() throws Exception {
        Path signed = signedFixture("org.example.notourapp", "wrong.apk");
        assertEquals(ApkPreflight.Error.WRONG_PACKAGE,
                assertThrows(ApkPreflight.Rejected.class, () -> preflight.inspect(signed,
                        ApkPreflight.Policy.explicitlyAllowDebug())).error());
    }
    @Test void unsignedArchiveAndWrongExtensionsAreRejected() throws Exception {
        Path unsigned = fixture(ApkPreflight.BRIDGE_PACKAGE, "unsigned.apk");
        assertEquals(ApkPreflight.Error.UNSIGNED_OR_INVALID,
                assertThrows(ApkPreflight.Rejected.class, () -> preflight.inspect(unsigned,
                        ApkPreflight.Policy.explicitlyAllowDebug())).error());
        for (String ext : new String[]{".apks", ".aab", ".zip"}) {
            Path copied = Files.copy(unsigned, dir.resolve("sample" + ext));
            assertEquals(ApkPreflight.Error.UNSUPPORTED_FORMAT,
                    assertThrows(ApkPreflight.Rejected.class, () -> preflight.inspect(copied,
                            ApkPreflight.Policy.explicitlyAllowDebug())).error());
        }
    }
    @Test void malformedAndSymlinkAreRejected() throws Exception {
        Path invalid = Files.writeString(dir.resolve("bad.apk"), "not an APK");
        assertEquals(ApkPreflight.Error.MALFORMED_APK,
                assertThrows(ApkPreflight.Rejected.class, () -> preflight.inspect(invalid,
                        ApkPreflight.Policy.explicitlyAllowDebug())).error());
        Path symlink = dir.resolve("linked.apk");
        Files.createSymbolicLink(symlink, invalid);
        assertEquals(ApkPreflight.Error.NOT_LOCAL_FILE,
                assertThrows(ApkPreflight.Rejected.class, () -> preflight.inspect(symlink,
                        ApkPreflight.Policy.explicitlyAllowDebug())).error());
    }
}