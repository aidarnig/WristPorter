package io.github.aidarnig.wgs.desktop;

import atlantafx.base.theme.PrimerDark;
import io.github.aidarnig.wgs.core.WatchTransferService;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DesktopApplication extends Application {
    private ConfigurableApplicationContext context;
    private ExecutorService workers;

    @Override public void init() {
        context = new SpringApplicationBuilder(SpringDesktopConfiguration.class)
                .web(WebApplicationType.NONE).headless(false).run();
        workers = Executors.newSingleThreadExecutor(Thread.ofVirtual().factory());
    }

    @Override public void start(Stage stage) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        var workspace = new WatchWorkspace(context.getBean(WatchTransferService.class), workers);
        var scene = new Scene(workspace, 1160, 780);
        scene.getStylesheets().add(getClass().getResource("/watch-gallery.css").toExternalForm());
        stage.setTitle("WristPorter");
        stage.getIcons().add(BrandLogo.windowIcon());
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
    }

    @Override public void stop() {
        if (workers != null) workers.shutdownNow();
        if (context != null) context.close();
    }

    public static void main(String[] args) { launch(args); }
}
