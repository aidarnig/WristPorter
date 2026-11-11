package io.github.aidarnig.wgs.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PhotoCollectorTest {
    @TempDir Path root;

    @Test void importsNestedFoldersAndIgnoresUnsupportedFiles() throws Exception {
        Path subfolder = Files.createDirectories(root.resolve("trip/day 1"));
        Path one = Files.write(root.resolve("a.JPG"), new byte[] {(byte)255, (byte)216, (byte)255, 0, 0, 0, 0, 0});
        Path two = Files.write(subfolder.resolve("b.png"), new byte[] {(byte)137, 80, 78, 71, 13, 10, 26, 10});
        Files.writeString(subfolder.resolve("notes.txt"), "do not import");
        List<Path> photos = PhotoCollector.collect(root);
        assertEquals(2, photos.size());
        assertTrue(photos.contains(one));
        assertTrue(photos.contains(two));
    }

    @Test void acceptsSingleFileAndReportsMissingPaths() throws Exception {
        Path photo = Files.write(root.resolve("watch.png"), new byte[] {(byte)137, 80, 78, 71, 13, 10, 26, 10});
        assertEquals(List.of(photo), PhotoCollector.collect(photo));
        assertThrows(java.io.IOException.class, () -> PhotoCollector.collect(root.resolve("missing")));
    }
}
