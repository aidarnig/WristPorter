package io.github.aidarnig.wgs.desktop;

import io.github.aidarnig.wgs.core.TransferUpdate;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Keyboard-accessible, wrapping gallery that uses the available horizontal space.
 * Image thumbnails are decoded to a bounded size, never to original resolution.
 */
public final class PhotoQueueView extends ScrollPane {
    private static final double CARD_WIDTH = 164;
    private static final double CARD_HEIGHT = 162;
    private final ObservableList<Path> items = FXCollections.observableArrayList();
    private final Set<Path> selection = new LinkedHashSet<>();
    private final Map<Path, TransferUpdate> outcomes = new LinkedHashMap<>();
    private final TilePane tiles = new TilePane(12, 12);

    public PhotoQueueView() {
        getStyleClass().add("photo-queue");
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setFitToWidth(true);
        tiles.setAlignment(Pos.TOP_LEFT);
        tiles.setPadding(new Insets(12));
        tiles.setPrefTileWidth(CARD_WIDTH);
        tiles.setPrefTileHeight(CARD_HEIGHT);
        setContent(tiles);
        viewportBoundsProperty().addListener((observable, previous, next) ->
                tiles.setPrefWidth(next.getWidth()));
        items.addListener((ListChangeListener<Path>) change -> {
            selection.retainAll(items);
            outcomes.keySet().retainAll(items);
            refresh();
        });
        refresh();
    }

    public void refreshLanguage() { refresh(); }

    public ObservableList<Path> getItems() {
        return items;
    }

    public List<Path> getSelectedItems() {
        return List.copyOf(selection);
    }

    public void removeSelected() {
        items.removeAll(List.copyOf(selection));
    }

    public void addImages(Collection<Path> photos) {
        List<Path> unique = photos.stream().filter(photo -> !items.contains(photo)).toList();
        items.addAll(unique);
    }

    public void resetStatuses() {
        outcomes.clear();
        refresh();
    }

    public void showOutcome(TransferUpdate update) {
        outcomes.put(update.source(), update);
        // Updating a single cell avoids rebuilding every thumbnail during a batch.
        for (var node : tiles.getChildren()) {
            if (node instanceof ToggleButton card && update.source().equals(card.getUserData())) {
                if (card.getGraphic() instanceof VBox group
                        && group.getChildren().get(2) instanceof Label label) {
                    applyStatus(label, update);
                }
                break;
            }
        }
    }

    private void refresh() {
        tiles.getChildren().clear();
        if (items.isEmpty()) {
            Label empty = new Label(I18n.t("files.empty"));
            empty.getStyleClass().add("muted");
            tiles.getChildren().add(empty);
            return;
        }
        for (Path photo : items) tiles.getChildren().add(card(photo));
    }

    private ToggleButton card(Path photo) {
        Image thumbnail = new Image(photo.toUri().toString(), 142, 93, true, true, true);
        ImageView image = new ImageView(thumbnail);
        image.setFitWidth(142);
        image.setFitHeight(93);
        image.setPreserveRatio(true);
        VBox imageFrame = new VBox(image);
        imageFrame.setMinHeight(97);
        imageFrame.setAlignment(Pos.CENTER);
        imageFrame.getStyleClass().add("photo-image-frame");

        Label filename = new Label(photo.getFileName().toString());
        filename.setMaxWidth(145);
        filename.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        filename.getStyleClass().add("photo-card-name");
        Label status = new Label();
        status.getStyleClass().add("photo-card-status");
        applyStatus(status, outcomes.get(photo));

        VBox content = new VBox(5, imageFrame, filename, status);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(7));
        ToggleButton button = new ToggleButton();
        button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        button.setGraphic(content);
        button.setUserData(photo);
        button.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        button.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        button.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        button.setAlignment(Pos.CENTER);
        button.getStyleClass().add("photo-card");
        button.setSelected(selection.contains(photo));
        button.setAccessibleRole(AccessibleRole.TOGGLE_BUTTON);
        button.setAccessibleText(I18n.t("files.select", photo.getFileName()));
        button.selectedProperty().addListener((observable, before, selected) -> {
            if (selected) selection.add(photo);
            else selection.remove(photo);
        });
        return button;
    }

    private static void applyStatus(Label label, TransferUpdate update) {
        label.getStyleClass().removeAll("photo-card-success", "photo-card-error", "photo-card-warn");
        if (update == null) {
            label.setText(I18n.t("files.ready"));
            return;
        }
        switch (update.status()) {
            case COPIED_SCAN_REQUESTED -> {
                label.setText(I18n.t("files.sent"));
                label.getStyleClass().add("photo-card-success");
            }
            case COPIED_SCAN_FAILED -> {
                label.setText(I18n.t("files.scan.failed"));
                label.getStyleClass().add("photo-card-warn");
            }
            case FAILED -> {
                label.setText(I18n.t("files.push.failed"));
                label.getStyleClass().add("photo-card-error");
            }
        }
    }
}
