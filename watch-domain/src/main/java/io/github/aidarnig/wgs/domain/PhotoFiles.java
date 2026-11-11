package io.github.aidarnig.wgs.domain;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Local photo validation and safe, collision-resistant remote filenames. */
public final class PhotoFiles {
    private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private PhotoFiles() {}

    public static boolean isSupported(Path path) {
        if (path == null || path.getFileName() == null) return false;
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    public static void requireReadable(Path path) {
        Objects.requireNonNull(path, "file");
        if (!isSupported(path)) throw new IllegalArgumentException("Only JPEG and PNG files are supported");
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException("Image is missing or unreadable: " + path);
        }
    }

    public static String remoteName(Path path) {
        if (!isSupported(path)) throw new IllegalArgumentException("Unsupported image: " + path);
        String filename = path.getFileName().toString();
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return "wgs-" + UUID.randomUUID() + "." + ext;
    }
}
