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
        Path one = Files.writeString(root.resolve("a.JPG"), "image");
        Path two = Files.writeString(subfolder.resolve("b.png"), "image");
        Files.writeString(subfolder.resolve("notes.txt"), "do not import");
        List<Path> photos = PhotoCollector.collect(root);
        assertEquals(2, photos.size());
        assertTrue(photos.contains(one));
        assertTrue(photos.contains(two));
    }

    @Test void acceptsSingleFileAndReportsMissingPaths() throws Exception {
        Path photo = Files.writeString(root.resolve("watch.png"), "image");
        assertEquals(List.of(photo), PhotoCollector.collect(photo));
        assertThrows(java.io.IOException.class, () -> PhotoCollector.collect(root.resolve("missing")));
    }
}
