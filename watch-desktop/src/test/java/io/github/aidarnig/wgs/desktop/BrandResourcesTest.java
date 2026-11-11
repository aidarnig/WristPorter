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

    @Test void logoHasSafeTransparentRightMargin() throws Exception {
        for (String name : new String[] {"logo-light.png", "logo-dark.png"}) {
            try (var input = Objects.requireNonNull(
                    BrandLogo.class.getResourceAsStream("/branding/" + name))) {
                var image = ImageIO.read(input);
                assertEquals(1380, image.getWidth(), "Unexpected SVG export width");
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = image.getWidth() - 24; x < image.getWidth(); x++) {
                        assertEquals(0, (image.getRGB(x, y) >>> 24),
                                "Brand text touches the right margin: " + name);
                    }
                }
            }
        }
    }

}
