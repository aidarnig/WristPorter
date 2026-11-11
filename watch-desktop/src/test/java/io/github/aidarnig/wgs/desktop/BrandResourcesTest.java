package io.github.aidarnig.wgs.desktop;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.util.Objects;
import static org.junit.jupiter.api.Assertions.*;

class BrandResourcesTest {
    @Test void brandingImagesAreBundledAndDecode() throws Exception {
        String[] names = {"logo-light.png", "logo-dark.png", "watch-icon.png"};
        for (String name : names) {
            try (var stream = Objects.requireNonNull(
                    BrandLogo.class.getResourceAsStream("/branding/" + name),
                    "Missing branding file: " + name)) {
                var image = ImageIO.read(stream);
                assertNotNull(image, "Could not decode " + name);
                assertTrue(image.getWidth() >= 512, "Image too small: " + name);
                assertTrue(image.getHeight() >= 256, "Image too small: " + name);
            }
        }
    }
}
