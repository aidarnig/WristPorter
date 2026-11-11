package io.github.aidarnig.wgs.desktop;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Lazy 64px previews; full-size images are never decoded into the queue. */
final class PhotoPreviewCell extends ListCell<Path> {
    private static final int SIZE = 64;
    private static final int MAX_CACHED_IMAGES = 160;
    private static final Map<Path, Image> PREVIEWS = new LinkedHashMap<>(128, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<Path, Image> eldest) {
            return size() > MAX_CACHED_IMAGES;
        }
    };

    @Override protected void updateItem(Path file, boolean empty) {
        super.updateItem(file, empty);
        if (empty || file == null) {
            setText(null);
            setGraphic(null);
            return;
        }

        Image image = PREVIEWS.computeIfAbsent(file, path ->
                new Image(path.toUri().toString(), SIZE, SIZE, true, true, true));
        ImageView preview = new ImageView(image);
        preview.setFitHeight(SIZE);
        preview.setFitWidth(SIZE);
        preview.setPreserveRatio(true);
        preview.getStyleClass().add("photo-thumbnail");

        Label filename = new Label(file.getFileName().toString());
        filename.getStyleClass().add("photo-name");
        filename.setMaxWidth(Double.MAX_VALUE);
        Label format = new Label(file.getFileName().toString()
                .replaceAll("^.*\\.", "").toUpperCase(Locale.ROOT) + " • Готово к передаче");
        format.getStyleClass().add("photo-extension");
        VBox detail = new VBox(4, filename, format);
        HBox line = new HBox(14, preview, detail);
        line.setAlignment(Pos.CENTER_LEFT);
        line.getStyleClass().add("photo-preview");
        setText(null);
        setGraphic(line);
    }
}
