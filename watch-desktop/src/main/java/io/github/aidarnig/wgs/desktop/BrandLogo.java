package io.github.aidarnig.wgs.desktop;

import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.Objects;

/**
 * Canonical watch + WP wordmark, rendered from the same exported artwork
 * as the GitHub social preview. The images are bundled on the classpath.
 */
public final class BrandLogo extends HBox {
    private static final Image LIGHT = load("/branding/logo-light.png");
    private static final Image DARK = load("/branding/logo-dark.png");
    private static final Image WATCH = load("/branding/watch-icon.png");

    private final ImageView wordmark = new ImageView();

    public BrandLogo(boolean lightTheme) {
        setAlignment(Pos.CENTER_LEFT);
        getStyleClass().add("brand-logo");
        wordmark.setFitWidth(252);
        wordmark.setPreserveRatio(true);
        wordmark.setSmooth(true);
        wordmark.setMouseTransparent(true);
        setLightTheme(lightTheme);
        getChildren().add(wordmark);
    }

    public void setLightTheme(boolean lightTheme) {
        wordmark.setImage(lightTheme ? LIGHT : DARK);
    }

    public static Image windowIcon() {
        return WATCH;
    }

    private static Image load(String name) {
        Image image = new Image(Objects.requireNonNull(
                BrandLogo.class.getResource(name), "Missing branding resource: " + name)
                .toExternalForm(), false);
        if (image.isError()) {
            throw new IllegalStateException("Cannot decode branding resource: " + name,
                    image.getException());
        }
        return image;
    }
}
