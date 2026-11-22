package io.github.aidarnig.wgs.desktop;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import io.github.aidarnig.wgs.adb.AdbExecutableLocator;
import io.github.aidarnig.wgs.adb.WatchDevice;
import io.github.aidarnig.wgs.adb.WatchEndpoint;
import io.github.aidarnig.wgs.core.TransferCoordinator;
import io.github.aidarnig.wgs.core.TransferUpdate;
import io.github.aidarnig.wgs.core.WatchTransferService;
import io.github.aidarnig.wgs.domain.PhotoFiles;
import io.github.aidarnig.wgs.domain.PhotoCollector;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.prefs.Preferences;

/** Functional first iteration. Preview thumbnails and settings are tracked for M2/M4. */
public final class WatchWorkspace extends BorderPane {
    private final WatchTransferService service;
    private final ExecutorService worker;
    private final TransferCoordinator coordinator;
    private long displayedJobId;
    private final TextField adbPath = new TextField(AdbExecutableLocator.detect());
    private final TextField pairEndpoint = new TextField();
    private final PasswordField pairCode = new PasswordField();
    private final TextField connectionEndpoint = new TextField();
    private final ComboBox<String> watchPicker = new ComboBox<>();
    private final Label deviceStatus = new Label(I18n.t("connect.no.connection"));
    private final Label activity = new Label(I18n.t("progress.ready"));
    private final Label count = new Label(I18n.t("files.count", 0));
    private final ProgressBar progress = new ProgressBar(0);
    private final Label progressHeadline = new Label(I18n.t("progress.title"));
    private final Label progressNumbers = new Label("0 / 0");
    private final Label progressDetails = new Label(I18n.t("progress.details", 0, 0));
    private final PhotoQueueView files = new PhotoQueueView();
    private final ListView<String> history = new ListView<>();
    private final Button send = new Button(I18n.t("progress.send"));
    private final Button cancelTransfer = new Button(I18n.t("progress.cancel.button"));
    private final Button retryFailed = new Button(I18n.t("progress.retry.button"));
    private final Preferences preferences = Preferences.userNodeForPackage(WatchWorkspace.class);
    private boolean hasCompletedTransfer;
    private boolean lightTheme = preferences.getBoolean("appearance.light", false);

    public WatchWorkspace(WatchTransferService service, ExecutorService worker,
                          TransferCoordinator coordinator) {
        this.service = service;
        this.worker = worker;
        this.coordinator = java.util.Objects.requireNonNull(coordinator, "coordinator");
        getStyleClass().add("app-shell");
        applyTheme();
        setTop(header());
        files.getItems().addListener((javafx.collections.ListChangeListener<Path>) change -> {
            coordinator.replaceQueue(TransferCoordinator.Owner.DESKTOP, List.copyOf(files.getItems()));
            updateCount();
        });
        connectionEndpoint.textProperty().addListener((obs, before, after) -> syncSelectedWatch());
        adbPath.textProperty().addListener((obs, before, after) -> syncSelectedWatch());
        ScrollPane connectionScroll = new ScrollPane(connectionPanel());
        connectionScroll.setFitToWidth(true);
        connectionScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        connectionScroll.getStyleClass().add("connection-scroll");
        setLeft(connectionScroll);
        setCenter(imagesPanel());
        BorderPane.setMargin(getCenter(), new Insets(20, 24, 20, 24));
        updateCount();
    }

    private HBox header() {
        BrandLogo brand = new BrandLogo(lightTheme);
        Label subtitle = new Label(I18n.t("header.subtitle"));
        subtitle.getStyleClass().add("header-subtitle");
        subtitle.setAlignment(Pos.CENTER_LEFT);
        subtitle.setMaxWidth(390);
        subtitle.setWrapText(true);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button theme = new Button(lightTheme ? I18n.t("header.theme.dark") : I18n.t("header.theme.light"));
        theme.setOnAction(event -> {
            lightTheme = !lightTheme;
            applyTheme();
            setTop(header());
        });
        ComboBox<String> language = new ComboBox<>();
        language.getItems().addAll("Русский", "English");
        language.setValue(I18n.language().equals("ru") ? "Русский" : "English");
        language.setTooltip(new Tooltip(I18n.t("header.language")));
        language.setOnAction(event -> {
            String code = "Русский".equals(language.getValue()) ? "ru" : "en";
            if (!code.equals(I18n.language())) changeLanguage(code);
        });
        HBox box = new HBox(18, brand, subtitle, spacer, language, theme);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(19, 26, 19, 26));
        box.getStyleClass().add("app-header");
        return box;
    }

    private void applyTheme() {
        getStyleClass().remove("wgs-light");
        if (lightTheme) getStyleClass().add("wgs-light");
        Application.setUserAgentStylesheet(lightTheme
                ? new PrimerLight().getUserAgentStylesheet()
                : new PrimerDark().getUserAgentStylesheet());
        preferences.putBoolean("appearance.light", lightTheme);
    }

    private void changeLanguage(String code) {
        if (coordinator.isBusy()) return;
        I18n.setLanguage(code);
        // Preserve watch endpoints, device picker, queue, results, and theme.
        setTop(null);
        setLeft(null);
        setCenter(null);
        setTop(header());
        ScrollPane sidebar = new ScrollPane(connectionPanel());
        sidebar.setFitToWidth(true);
        sidebar.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebar.getStyleClass().add("connection-scroll");
        setLeft(sidebar);
        files.refreshLanguage();
        setCenter(imagesPanel());
        BorderPane.setMargin(getCenter(), new Insets(20, 24, 20, 24));
        if (watchPicker.getValue() != null) {
            deviceStatus.setText(I18n.t("connect.found", watchPicker.getValue()));
        } else {
            deviceStatus.setText(I18n.t("connect.no.connection"));
        }
        history.getItems().setAll(coordinator.outcomes(TransferCoordinator.Owner.DESKTOP).values().stream()
                .map(update -> update.source().getFileName() + " — " + resultText(update))
                .toList());
        activity.setText(hasCompletedTransfer
                ? I18n.t("progress.completed.details") : I18n.t("progress.ready"));
        progressHeadline.setText(I18n.t(hasCompletedTransfer
                ? "progress.completed" : "progress.title"));
        updateCount();
    }

    private String resultText(TransferUpdate update) {
        return switch (update.status()) {
            case COPIED_SCAN_REQUESTED -> I18n.t("progress.copied");
            case COPIED_SCAN_FAILED -> I18n.t("progress.scan.failed");
            case FAILED -> I18n.t("progress.copy.failed");
        };
    }

    private VBox connectionPanel() {
        Label title = new Label(I18n.t("connection.title"));
        title.getStyleClass().add("panel-heading");
        Label guidance = note(I18n.t("connection.help"));
        adbPath.setPromptText(I18n.t("adb.path.hint"));
        Button browse = new Button(I18n.t("adb.browse"));
        browse.setMaxWidth(Double.MAX_VALUE);
        browse.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(I18n.t("adb.browse.title"));
            File result = chooser.showOpenDialog(getScene().getWindow());
            if (result != null) adbPath.setText(result.getAbsolutePath());
        });

        VBox advancedFields = new VBox(8, label(I18n.t("adb.path")), adbPath, browse);
        advancedFields.setVisible(false);
        advancedFields.setManaged(false);
        Button advancedAdb = new Button(I18n.t("adb.advanced"));
        advancedAdb.setMaxWidth(Double.MAX_VALUE);
        advancedAdb.setOnAction(event -> {
            boolean expanded = !advancedFields.isVisible();
            advancedFields.setVisible(expanded);
            advancedFields.setManaged(expanded);
        });
        Label adbInfo = note(Files.isRegularFile(Path.of(adbPath.getText()))
                ? I18n.t("adb.found")
                : I18n.t("adb.fallback"));

        Label pairing = new Label(I18n.t("pair.title"));
        pairing.getStyleClass().add("section-heading");
        pairEndpoint.setPromptText(I18n.t("pair.address"));
        pairCode.setPromptText(I18n.t("pair.code"));
        Button pair = new Button(I18n.t("pair.button"));
        pair.setMaxWidth(Double.MAX_VALUE);
        pair.setOnAction(event -> {
            String endpoint = pairEndpoint.getText().strip();
            String code = pairCode.getText().strip();
            String adb = adbPath.getText().strip();
            try {
                WatchEndpoint.parse(endpoint);
            } catch (IllegalArgumentException exception) {
                activity.setText(I18n.t("connect.invalid.endpoint"));
                return;
            }
            if (!code.matches("[0-9]{6}")) {
                activity.setText(I18n.t("pair.invalid.code"));
                return;
            }
            runOperation(I18n.t("pair.in.progress"), () -> service.pair(adb, endpoint, code), () -> {
                pairCode.clear();
                deviceStatus.setText(I18n.t("pair.success"));
            });
        });

        Label connecting = new Label(I18n.t("connect.title"));
        connecting.getStyleClass().add("section-heading");
        connectionEndpoint.setPromptText(I18n.t("connect.address"));
        watchPicker.setPromptText(I18n.t("connect.select"));
        watchPicker.setMaxWidth(Double.MAX_VALUE);
        watchPicker.setDisable(true);
        watchPicker.setOnAction(event -> {
            if (watchPicker.getValue() != null) connectionEndpoint.setText(watchPicker.getValue());
        });
        Button connect = new Button(I18n.t("connect.button"));
        connect.getStyleClass().add("accent-button");
        connect.setMaxWidth(Double.MAX_VALUE);
        connect.setOnAction(event -> {
            String target = connectionEndpoint.getText().strip();
            String adb = adbPath.getText().strip();
            try {
                WatchEndpoint.parse(target);
            } catch (IllegalArgumentException exception) {
                activity.setText(I18n.t("connect.invalid.endpoint"));
                return;
            }
            runOperation(I18n.t("connect.in.progress"), () -> service.connect(adb, target),
                    () -> deviceStatus.setText(I18n.t("connect.success", target)));
        });
        Button discover = new Button(I18n.t("connect.detect"));
        discover.setMaxWidth(Double.MAX_VALUE);
        discover.setOnAction(event -> discoverDevices());
        deviceStatus.getStyleClass().add("connection-state");
        deviceStatus.setWrapText(true);
        VBox box = new VBox(10,
                title, guidance, adbInfo, advancedAdb, advancedFields,
                new Separator(), pairing, pairEndpoint, pairCode, pair,
                new Separator(), connecting, connectionEndpoint, connect, discover, watchPicker,
                deviceStatus, note(I18n.t("connect.port.note")));
        box.getStyleClass().add("side-panel");
        box.setPadding(new Insets(24));
        box.setPrefWidth(330);
        box.setMinWidth(310);
        return box;
    }

    private VBox imagesPanel() {
        Label title = new Label(I18n.t("files.title"));
        title.getStyleClass().add("page-heading");
        Label explanation = note(I18n.t("files.description"));
        VBox drop = dropZone();
        Button add = new Button(I18n.t("files.add"));
        add.getStyleClass().add("accent-button");
        add.setOnAction(event -> chooseFiles());
        Button folder = new Button(I18n.t("files.folder"));
        folder.setOnAction(event -> chooseFolder());
        Button remove = new Button(I18n.t("files.remove"));
        remove.setOnAction(event -> {
            if (!coordinator.isBusy()) files.removeSelected();
        });
        Button clear = new Button(I18n.t("files.clear"));
        clear.setOnAction(event -> {
            if (!coordinator.isBusy()) files.getItems().clear();
        });
        HBox actions = new HBox(8, add, folder, remove, clear);
        actions.setAlignment(Pos.CENTER_LEFT);

        Label queueTitle = new Label(I18n.t("files.queue"));
        queueTitle.getStyleClass().add("section-heading");
        count.getStyleClass().add("muted");
        HBox queueHeader = new HBox(14, queueTitle, count);
        queueHeader.setAlignment(Pos.CENTER_LEFT);
        files.setMinHeight(150);

        Label historyTitle = new Label(I18n.t("history.title"));
        historyTitle.getStyleClass().add("section-heading");
        history.setMinHeight(95);
        history.setPlaceholder(new Label(I18n.t("history.empty")));
        VBox resultsPanel = new VBox(8, historyTitle, history);
        VBox.setVgrow(history, Priority.ALWAYS);
        resultsPanel.setMinHeight(125);

        SplitPane split = new SplitPane(files, resultsPanel);
        split.setOrientation(javafx.geometry.Orientation.VERTICAL);
        split.setDividerPositions(0.70);
        split.getStyleClass().add("queue-split");
        VBox.setVgrow(split, Priority.ALWAYS);

        send.getStyleClass().add("accent-button");
        send.setOnAction(event -> sendImages());
        send.setPrefWidth(215);
        cancelTransfer.setDisable(true);
        cancelTransfer.setOnAction(event -> {
            if (coordinator.cancel(TransferCoordinator.Owner.DESKTOP)) {
                cancelTransfer.setDisable(true);
                activity.setText(I18n.t("progress.cancel.message"));
            }
        });
        retryFailed.setDisable(true);
        retryFailed.setOnAction(event -> retryFailures());
        HBox bottom = new HBox(10, retryFailed, cancelTransfer, send);
        bottom.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox(12, title, explanation, drop, actions, queueHeader,
                split, progressPanel(), bottom);
        box.getStyleClass().add("workspace");
        return box;
    }

    private VBox progressPanel() {
        progressHeadline.getStyleClass().add("progress-headline");
        progressNumbers.getStyleClass().add("progress-numbers");
        progressDetails.getStyleClass().add("progress-details");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox summary = new HBox(12, progressHeadline, spacer, progressNumbers);
        summary.setAlignment(Pos.CENTER_LEFT);
        progress.setMaxWidth(Double.MAX_VALUE);
        progress.setPrefHeight(16);
        progress.getStyleClass().add("transfer-progress");
        activity.getStyleClass().add("muted");
        activity.setWrapText(true);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox details = new HBox(12, activity, gap, progressDetails);
        details.setAlignment(Pos.CENTER_LEFT);
        VBox panel = new VBox(9, summary, progress, details);
        panel.setPadding(new Insets(15, 18, 15, 18));
        panel.getStyleClass().add("transfer-panel");
        return panel;
    }

        private VBox dropZone() {
        Label icon = new Label("＋");
        icon.getStyleClass().add("drop-icon");
        Label title = new Label(I18n.t("files.drop"));
        title.getStyleClass().add("section-heading");
        VBox box = new VBox(8, icon, title, note(I18n.t("files.drop.note")));
        box.getStyleClass().add("drop-zone");
        box.setPadding(new Insets(24));
        box.setAlignment(Pos.CENTER);
        box.setMinHeight(108);
        box.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) event.acceptTransferModes(TransferMode.COPY);
            event.consume();
        });
        box.setOnDragDropped(event -> {
            Dragboard drag = event.getDragboard();
            boolean accepted = drag.hasFiles();
            if (accepted) drag.getFiles().forEach(file -> importPath(file.toPath()));
            event.setDropCompleted(accepted);
            event.consume();
        });
        return box;
    }

    private void discoverDevices() {
        String executable = adbPath.getText().strip();
        activity.setText(I18n.t("connect.searching"));
        Task<List<WatchDevice>> task = new Task<>() {
            @Override protected List<WatchDevice> call() throws Exception {
                return service.devices(executable);
            }
        };
        task.setOnSucceeded(event -> {
            if (coordinator.isBusy()) return; // Never retarget a live transfer from a stale scan.
            List<WatchDevice> ready = task.getValue().stream().filter(WatchDevice::ready).toList();
            List<String> serials = ready.stream().map(WatchDevice::serial).toList();
            watchPicker.getItems().setAll(serials);
            watchPicker.setDisable(serials.isEmpty());
            if (serials.size() == 1) {
                watchPicker.getSelectionModel().selectFirst();
                connectionEndpoint.setText(serials.getFirst());
                deviceStatus.setText(I18n.t("connect.found", serials.getFirst()));
            } else if (serials.isEmpty()) {
                deviceStatus.setText(I18n.t("connect.none"));
            } else {
                connectionEndpoint.clear();
                deviceStatus.setText(I18n.t("connect.multiple", serials.size()));
            }
            activity.setText(I18n.t("connect.search.complete"));
        });
        task.setOnFailed(event -> failure(I18n.t("connect.search.error"), task.getException()));
        worker.submit(task);
    }

    /** Only the Windows UI may select the watch; Web clients receive leases, never choose serials. */
    private void syncSelectedWatch() {
        coordinator.selectWatch(adbPath.getText(), connectionEndpoint.getText());
        updateCount();
    }

    private void sendImages() {
        if (coordinator.isBusy() || files.getItems().isEmpty()) return;
        transfer(false);
    }

    private void retryFailures() {
        if (coordinator.isBusy()) return;
        syncSelectedWatch();
        if (!coordinator.canRetry(TransferCoordinator.Owner.DESKTOP)) {
            activity.setText(I18n.t("progress.retry.device"));
            return;
        }
        transfer(true);
    }

    private void transfer(boolean retry) {
        syncSelectedWatch();
        TransferCoordinator.WatchLease lease = coordinator.selection();
        if (lease == null) {
            activity.setText(I18n.t("progress.no.connection"));
            return;
        }
        int total = retry
                ? (int) coordinator.outcomes(TransferCoordinator.Owner.DESKTOP).values().stream()
                    .filter(TransferUpdate::retryable).count()
                : files.getItems().size();
        if (total == 0) return;
        try {
            displayedJobId = retry
                    ? coordinator.retry(TransferCoordinator.Owner.DESKTOP, lease, this::acceptTransferEvent)
                    : coordinator.send(TransferCoordinator.Owner.DESKTOP, lease,
                            List.copyOf(files.getItems()), this::acceptTransferEvent);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            failure(I18n.t("operation.transfer.error"), exception);
            return;
        }
        hasCompletedTransfer = false;
        if (!retry) {
            files.resetStatuses();
        }
        history.getItems().clear();
        progress.setProgress(0);
        progressHeadline.setText(I18n.t(retry ? "progress.retry" : "progress.title"));
        progressNumbers.setText("0 / " + total);
        progressDetails.setText(I18n.t("progress.details", 0, 0));
        activity.setText(I18n.t("progress.started"));
        cancelTransfer.setDisable(false);
        setTransferControls(true);
        updateCount();
    }

    /** Coordinator callbacks run off the FX thread; no JavaFX state is touched there. */
    private void acceptTransferEvent(TransferCoordinator.Event event) {
        Platform.runLater(() -> {
            if (event.jobId() != displayedJobId) return;
            if (event.phase() == TransferCoordinator.Phase.PROGRESS) {
                TransferUpdate update = event.update();
                files.showOutcome(update);
                progress.setProgress((double) event.completed() / event.total());
                progressNumbers.setText(event.completed() + " / " + event.total());
                progressDetails.setText(I18n.t("progress.details",
                        event.completed() - event.errors(), event.errors()));
                String result = resultText(update);
                history.getItems().add(update.source().getFileName() + " — " + result);
                activity.setText(I18n.t("progress.current",
                        event.completed(), event.total(), result));
            } else if (event.phase() == TransferCoordinator.Phase.COMPLETED) {
                finishTransfer();
                hasCompletedTransfer = true;
                activity.setText(I18n.t("progress.completed.details"));
                progressHeadline.setText(I18n.t("progress.completed"));
                progressNumbers.setText(event.total() + " / " + event.total());
                progressDetails.setText(I18n.t("progress.details",
                        event.completed() - event.errors(), event.errors()));
            } else if (event.phase() == TransferCoordinator.Phase.FAILED) {
                finishTransfer();
                failure(I18n.t("operation.transfer.error"), event.failure());
            } else if (event.phase() == TransferCoordinator.Phase.CANCELLED) {
                finishTransfer();
                progressHeadline.setText(I18n.t("progress.stopped"));
                activity.setText(I18n.t("progress.stopped"));
            }
            updateCount();
        });
    }

    private void setTransferControls(boolean busy) {
        adbPath.setDisable(busy);
        connectionEndpoint.setDisable(busy);
        watchPicker.setDisable(busy || watchPicker.getItems().isEmpty());
    }

    private void runOperation(String message, Operation operation, Runnable success) {
        activity.setText(message);
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception { operation.run(); return null; }
        };
        task.setOnSucceeded(event -> { success.run(); activity.setText(I18n.t("operation.done")); });
        task.setOnFailed(event -> failure(I18n.t("operation.adb.error"), task.getException()));
        worker.submit(task);
    }

    private void chooseFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.t("files.dialog"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                I18n.t("files.filetypes"), "*.jpg", "*.jpeg", "*.png", "*.JPG", "*.JPEG", "*.PNG"));
        List<File> result = chooser.showOpenMultipleDialog(getScene().getWindow());
        if (result != null && !coordinator.isBusy()) {
            files.addImages(result.stream().map(File::toPath)
                    .filter(PhotoFiles::isReadableImage).toList());
        }
    }

    private void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(I18n.t("files.folder.dialog"));
        File result = chooser.showDialog(getScene().getWindow());
        if (result != null && !coordinator.isBusy()) importPath(result.toPath());
    }

    private void importPath(Path path) {
        if (coordinator.isBusy()) return;
        if (!Files.isDirectory(path)) {
            addImage(path);
            return;
        }
        activity.setText(I18n.t("files.scanning"));
        Task<List<Path>> scan = new Task<>() {
            @Override protected List<Path> call() throws Exception {
                return PhotoCollector.collect(path);
            }
        };
        scan.setOnSucceeded(event -> {
            if (coordinator.isBusy()) return;
            int before = files.getItems().size();
            files.addImages(scan.getValue());
            activity.setText(I18n.t("files.added", files.getItems().size() - before));
        });
        scan.setOnFailed(event -> failure(I18n.t("files.folder.error"), scan.getException()));
        worker.submit(scan);
    }

    private void addImage(Path image) {
        if (PhotoFiles.isReadableImage(image) && !files.getItems().contains(image)) {
            files.addImages(List.of(image));
        }
    }

    private void updateCount() {
        count.setText(I18n.t("files.count", files.getItems().size()));
        send.setDisable(coordinator.isBusy() || files.getItems().isEmpty());
        retryFailed.setDisable(coordinator.isBusy()
                || !coordinator.canRetry(TransferCoordinator.Owner.DESKTOP));
    }

    private void finishTransfer() {
        cancelTransfer.setDisable(true);
        setTransferControls(false);
        updateCount();
    }

    private void failure(String label, Throwable cause) {
        activity.setText(label + ": " + (cause == null ? I18n.t("operation.unknown") : cause.getMessage()));
    }

    private static Label label(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-heading");
        return label;
    }

    private static Label note(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("muted");
        return label;
    }

    @FunctionalInterface
    private interface Operation { void run() throws Exception; }
}
