package io.github.aidarnig.wgs.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PhotoFilesTest {
    @TempDir Path temp;

    @Test void recognizesImagesWithoutCaseSensitivity() {
        assertTrue(PhotoFiles.isSupported(Path.of("photo.JPG")));
        assertTrue(PhotoFiles.isSupported(Path.of("photo.png")));
        assertFalse(PhotoFiles.isSupported(Path.of("photo.gif")));
        assertFalse(PhotoFiles.isSupported(Path.of("photo.png.exe")));
    }

    @Test void preventsUnsafeOrCollidingNames() {
        Path path = Path.of("some special ! имя.PNG");
        String first = PhotoFiles.remoteName(path);
        String second = PhotoFiles.remoteName(path);
        assertNotEquals(first, second);
        assertTrue(first.matches("wgs-[0-9a-f-]{36}\\.png"));
    }

    @Test void rejectsUnreadableAndUnsupportedPaths() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> PhotoFiles.requireReadable(temp.resolve("absent.png")));
        Path file = Files.write(temp.resolve("good.png"), new byte[] {(byte)137, 80, 78, 71, 13, 10, 26, 10});
        assertDoesNotThrow(() -> PhotoFiles.requireReadable(file));
        Path spoofed = Files.writeString(temp.resolve("spoofed.png"), "this is not an image");
        assertFalse(PhotoFiles.isReadableImage(spoofed));
        assertThrows(IllegalArgumentException.class, () -> PhotoFiles.requireReadable(spoofed));
        assertThrows(IllegalArgumentException.class, () -> PhotoFiles.requireReadable(temp.resolve("bad.txt")));
    }
}
