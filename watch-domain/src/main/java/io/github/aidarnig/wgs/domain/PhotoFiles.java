package io.github.aidarnig.wgs.domain;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Local file rules and collision-resistant remote filenames. */
public final class PhotoFiles {
    private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final byte[] PNG_HEADER = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};

    private PhotoFiles() {}

    public static boolean isSupported(Path path) {
        if (path == null || path.getFileName() == null) return false;
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    public static boolean isReadableImage(Path path) {
        try {
            requireReadable(path);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /** Rejects a non-image file masquerading as JPEG or PNG based on its header. */
    public static void requireReadable(Path path) {
        Objects.requireNonNull(path, "file");
        if (!isSupported(path)) throw new IllegalArgumentException("Only JPEG and PNG files are supported");
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException("Image is missing or unreadable: " + path);
        }
        String extension = path.getFileName().toString().toLowerCase(Locale.ROOT);
        try (InputStream input = Files.newInputStream(path)) {
            byte[] header = input.readNBytes(8);
            boolean valid = extension.endsWith(".png") ? Arrays.equals(header, PNG_HEADER)
                    : header.length >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF;
            if (!valid) throw new IllegalArgumentException("Invalid JPEG/PNG signature: " + path);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read image header: " + path, exception);
        }
    }

    public static String remoteName(Path path) {
        if (!isSupported(path)) throw new IllegalArgumentException("Unsupported image: " + path);
        String name = path.getFileName().toString();
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return "wgs-" + UUID.randomUUID() + "." + extension;
    }
}
