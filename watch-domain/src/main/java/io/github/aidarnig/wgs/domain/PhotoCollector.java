package io.github.aidarnig.wgs.domain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Recursively discovers images without following directory symlinks. */
public final class PhotoCollector {
    private static final int MAX_FILES = 10_000;

    private PhotoCollector() {}

    public static List<Path> collect(Path root) throws IOException {
        Objects.requireNonNull(root, "root");
        if (!Files.exists(root)) throw new IOException("Folder or file does not exist: " + root);
        try (Stream<Path> paths = Files.walk(root)) {
            List<Path> found = paths.filter(Files::isRegularFile)
                    .filter(PhotoFiles::isReadableImage)
                    .limit(MAX_FILES + 1L)
                    .sorted(Comparator.comparing(Path::toString, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            if (found.size() > MAX_FILES) {
                throw new IOException("More than " + MAX_FILES + " photos; choose a smaller folder");
            }
            return found;
        }
    }
}
