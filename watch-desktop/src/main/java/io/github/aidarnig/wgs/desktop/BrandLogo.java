package io.github.aidarnig.wgs.desktop;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;

/**
 * A resolution-independent rendering of the selected WP smartwatch brand.
 * Theme changes alter only the wordmark, leaving the watch mark unchanged.
 */
public final class BrandLogo extends HBox {
    private final Text wrist = new Text("Wrist");

    public BrandLogo(boolean light) {
        super(10);
        getStyleClass().add("brand-logo");
        setAlignment(Pos.CENTER_LEFT);
        Group mark = watchMark();
        mark.getTransforms().add(new Scale(0.125, 0.125, 0, 0));
        StackPane icon = new StackPane(mark);
        icon.setPrefSize(44, 48);
        icon.setMinSize(44, 48);
        icon.setMaxSize(44, 48);

        wrist.setFont(Font.font("Segoe UI", FontWeight.BOLD, 23));
        Text porter = new Text("Porter");
        porter.setFont(Font.font("Segoe UI", FontWeight.BOLD, 23));
        porter.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#076afa")), new Stop(1, Color.web("#00cddb"))));
        setLightTheme(light);
        getChildren().addAll(icon, wrist, porter);
        setSpacing(0);
        HBox.setMargin(wrist, new javafx.geometry.Insets(0, 0, 0, 10));
    }

    /** Matching watch-only icon for the native window, including the taskbar. */
    public static javafx.scene.image.Image windowIcon() {
        javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setTransform(new Scale(0.22, 0.22, 0, 0));
        return watchMark().snapshot(params, null);
    }

    public void setLightTheme(boolean light) {
        wrist.setFill(Color.web(light ? "#071e3c" : "#f5f9ff"));
    }

    private static Group watchMark() {
        Group root = new Group();
        var blue = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#005fff")), new Stop(1, Color.web("#01cdf4")));
        var bezel = new LinearGradient(0, 1, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0061f4")), new Stop(1, Color.web("#00e6ed")));
        var letters = new LinearGradient(0, 1, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#eefaff")), new Stop(1, Color.web("#77c8ff")));
        var cyan = new LinearGradient(0, 1, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0767ff")), new Stop(1, Color.web("#00f4e5")));

        root.getChildren().addAll(
                svg("M85 90 105 16Q109 3 125 3H215Q231 3 236 17L256 90Z", blue),
                svg("M85 272H256L237 351Q235 365 219 368H123Q108 368 104 353Z", blue));
        Rectangle body = new Rectangle(26, 77, 288, 228);
        body.setArcWidth(130);
        body.setArcHeight(130);
        body.setFill(bezel);
        Rectangle face = new Rectangle(38, 89, 265, 203);
        face.setArcWidth(104);
        face.setArcHeight(104);
        face.setFill(Color.web("#03142b"));
        Rectangle crown = new Rectangle(312, 149, 19, 77);
        crown.setArcWidth(16);
        crown.setArcHeight(16);
        crown.setFill(blue);
        root.getChildren().addAll(body, face, crown);

        root.getChildren().addAll(
                stroke("M71 140 96 226 121 173 148 226 172 140", letters, 24),
                stroke("M187 212V143H228Q260 143 260 171Q260 198 229 198H188", cyan, 24),
                stroke("M120 240V265Q120 276 134 276H259Q270 276 270 264V226",
                        Color.web("#f1faff"), 10),
                svg("M145 268 179 238 209 259 229 244 256 268Z", cyan));
        javafx.scene.shape.Circle sun = new javafx.scene.shape.Circle(247, 229, 9, Color.web("#0ce7ec"));
        root.getChildren().add(sun);
        return root;
    }

    private static SVGPath svg(String shape, javafx.scene.paint.Paint fill) {
        SVGPath path = new SVGPath();
        path.setContent(shape);
        path.setFill(fill);
        return path;
    }

    private static SVGPath stroke(String shape, javafx.scene.paint.Paint color, double width) {
        SVGPath path = new SVGPath();
        path.setContent(shape);
        path.setFill(Color.TRANSPARENT);
        path.setStroke(color);
        path.setStrokeWidth(width);
        path.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        path.setStrokeLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        return path;
    }
}
