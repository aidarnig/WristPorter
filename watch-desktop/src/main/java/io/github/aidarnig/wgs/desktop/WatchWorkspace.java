package io.github.aidarnig.wgs.desktop;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import io.github.aidarnig.wgs.adb.WatchDevice;
import io.github.aidarnig.wgs.adb.WatchEndpoint;
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
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

/** Functional first iteration. Preview thumbnails and settings are tracked for M2/M4. */
public final class WatchWorkspace extends BorderPane {
    private final WatchTransferService service;
    private final ExecutorService worker;
    private final TextField adbPath = new TextField(defaultAdb());
    private final TextField pairEndpoint = new TextField();
    private final PasswordField pairCode = new PasswordField();
    private final TextField connectionEndpoint = new TextField();
    private final ComboBox<String> watchPicker = new ComboBox<>();
    private final Label deviceStatus = new Label("Нет соединения");
    private final Label activity = new Label("Добавьте фотографии и подключите часы");
    private final Label count = new Label("0 файлов");
    private final ProgressBar progress = new ProgressBar(0);
    private final ListView<Path> files = new ListView<>();
    private final ListView<String> history = new ListView<>();
    private final Button send = new Button("Передать на часы");
    private final Button cancelTransfer = new Button("Отменить");
    private final Button retryFailed = new Button("Повторить ошибки");
    private final Map<Path, TransferUpdate> latestResults = new LinkedHashMap<>();
    private Task<Void> activeTransfer;
    private String previousDevice;
    private boolean transferring;
    private final Preferences preferences = Preferences.userNodeForPackage(WatchWorkspace.class);
    private boolean lightTheme = preferences.getBoolean("appearance.light", false);

    public WatchWorkspace(WatchTransferService service, ExecutorService worker) {
        this.service = service;
        this.worker = worker;
        getStyleClass().add("app-shell");
        applyTheme();
        setTop(header());
        ScrollPane connectionScroll = new ScrollPane(connectionPanel());
        connectionScroll.setFitToWidth(true);
        connectionScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        connectionScroll.getStyleClass().add("connection-scroll");
        setLeft(connectionScroll);
        setCenter(imagesPanel());
        setBottom(footer());
        BorderPane.setMargin(getCenter(), new Insets(20, 24, 20, 24));
        updateCount();
    }

    private HBox header() {
        Label brand = new Label("WATCH GALLERY SYNC");
        brand.getStyleClass().add("brand");
        Label subtitle = new Label("Фотографии на часах — без Android-смартфона");
        subtitle.getStyleClass().add("muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button theme = new Button(lightTheme ? "Тёмная тема" : "Светлая тема");
        theme.setOnAction(event -> {
            lightTheme = !lightTheme;
            applyTheme();
            theme.setText(lightTheme ? "Тёмная тема" : "Светлая тема");
        });
        HBox box = new HBox(20, brand, subtitle, spacer, theme);
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

    private VBox connectionPanel() {
        Label title = new Label("Подключение");
        title.getStyleClass().add("panel-heading");
        Label guidance = note("Часы и компьютер должны быть в одной сети Wi-Fi. Включите беспроводную отладку на часах.");
        adbPath.setPromptText("adb.exe или путь к Platform-Tools");
        Button browse = new Button("Указать adb.exe");
        browse.setMaxWidth(Double.MAX_VALUE);
        browse.setOnAction(event -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Укажите adb.exe");
            File result = chooser.showOpenDialog(getScene().getWindow());
            if (result != null) adbPath.setText(result.getAbsolutePath());
        });

        Label pairing = new Label("1  Первое сопряжение");
        pairing.getStyleClass().add("section-heading");
        pairEndpoint.setPromptText("IP:порт сопряжения");
        pairCode.setPromptText("Шестизначный код");
        Button pair = new Button("Сопрячь часы");
        pair.setMaxWidth(Double.MAX_VALUE);
        pair.setOnAction(event -> {
            String endpoint = pairEndpoint.getText().strip();
            String code = pairCode.getText().strip();
            String adb = adbPath.getText().strip();
            try {
                WatchEndpoint.parse(endpoint);
            } catch (IllegalArgumentException exception) {
                activity.setText(exception.getMessage());
                return;
            }
            if (!code.matches("[0-9]{6}")) {
                activity.setText("Введите шестизначный код с экрана часов.");
                return;
            }
            runOperation("Сопрягаем часы…", () -> service.pair(adb, endpoint, code), () -> {
                pairCode.clear();
                deviceStatus.setText("Сопряжено. Теперь укажите порт отладки.");
            });
        });

        Label connecting = new Label("2  Подключение");
        connecting.getStyleClass().add("section-heading");
        connectionEndpoint.setPromptText("IP:порт отладки (другой порт)");
        watchPicker.setPromptText("Выберите найденные часы");
        watchPicker.setMaxWidth(Double.MAX_VALUE);
        watchPicker.setDisable(true);
        watchPicker.setOnAction(event -> {
            if (watchPicker.getValue() != null) connectionEndpoint.setText(watchPicker.getValue());
        });
        Button connect = new Button("Подключиться");
        connect.getStyleClass().add("accent-button");
        connect.setMaxWidth(Double.MAX_VALUE);
        connect.setOnAction(event -> {
            String target = connectionEndpoint.getText().strip();
            String adb = adbPath.getText().strip();
            try {
                WatchEndpoint.parse(target);
            } catch (IllegalArgumentException exception) {
                activity.setText(exception.getMessage());
                return;
            }
            runOperation("Подключение…", () -> service.connect(adb, target),
                    () -> deviceStatus.setText("Подключено: " + target));
        });
        Button discover = new Button("Обнаружить устройства");
        discover.setMaxWidth(Double.MAX_VALUE);
        discover.setOnAction(event -> discoverDevices());
        deviceStatus.getStyleClass().add("connection-state");
        deviceStatus.setWrapText(true);
        VBox box = new VBox(10,
                title, guidance, label("Путь к ADB"), adbPath, browse,
                new Separator(), pairing, pairEndpoint, pairCode, pair,
                new Separator(), connecting, connectionEndpoint, connect, discover, watchPicker,
                deviceStatus, note("Порт сопряжения и порт подключения на часах различаются."));
        box.getStyleClass().add("side-panel");
        box.setPadding(new Insets(24));
        box.setPrefWidth(330);
        box.setMinWidth(310);
        return box;
    }

    private VBox imagesPanel() {
        Label title = new Label("Мои фотографии");
        title.getStyleClass().add("page-heading");
        Label explanation = note("Добавьте JPG или PNG. После копирования программа запрашивает обновление Samsung Gallery.");
        VBox drop = dropZone();
        Button add = new Button("Добавить файлы");
        add.getStyleClass().add("accent-button");
        add.setOnAction(event -> chooseFiles());
        Button folder = new Button("Добавить папку");
        folder.setOnAction(event -> chooseFolder());
        Button remove = new Button("Убрать выбранные");
        remove.setOnAction(event -> files.getItems().removeAll(List.copyOf(files.getSelectionModel().getSelectedItems())));
        Button clear = new Button("Очистить");
        clear.setOnAction(event -> files.getItems().clear());
        HBox actions = new HBox(8, add, folder, remove, clear);
        actions.setAlignment(Pos.CENTER_LEFT);
        Label queueTitle = new Label("Очередь передачи");
        queueTitle.getStyleClass().add("section-heading");
        count.getStyleClass().add("muted");
        HBox queueHeader = new HBox(14, queueTitle, count);
        queueHeader.setAlignment(Pos.CENTER_LEFT);
        files.setPlaceholder(new Label("Здесь появятся выбранные изображения"));
        files.setCellFactory(view -> new PhotoPreviewCell());
        files.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        files.getItems().addListener((javafx.collections.ListChangeListener<Path>) change -> updateCount());
        VBox.setVgrow(files, Priority.ALWAYS);
        history.setPrefHeight(105);
        history.setPlaceholder(new Label("Здесь будет история текущей передачи"));
        send.getStyleClass().add("accent-button");
        send.setOnAction(event -> sendImages());
        send.setPrefWidth(200);
        cancelTransfer.setDisable(true);
        cancelTransfer.setOnAction(event -> {
            if (activeTransfer != null) {
                cancelTransfer.setDisable(true);
                activity.setText("Отменяем передачу…");
                activeTransfer.cancel(true);
            }
        });
        retryFailed.setDisable(true);
        retryFailed.setOnAction(event -> retryFailures());
        HBox bottom = new HBox(10, retryFailed, cancelTransfer, send);
        bottom.setAlignment(Pos.CENTER_RIGHT);
        VBox box = new VBox(12, title, explanation, drop, actions, queueHeader, files,
                label("Результаты"), history, bottom);
        box.getStyleClass().add("workspace");
        return box;
    }

    private VBox dropZone() {
        Label icon = new Label("＋");
        icon.getStyleClass().add("drop-icon");
        Label title = new Label("Перетащите фотографии сюда");
        title.getStyleClass().add("section-heading");
        VBox box = new VBox(8, icon, title, note("JPEG, PNG или папка с изображениями"));
        box.getStyleClass().add("drop-zone");
        box.setPadding(new Insets(24));
        box.setAlignment(Pos.CENTER);
        box.setMinHeight(145);
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

    private HBox footer() {
        activity.getStyleClass().add("muted");
        activity.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(activity, Priority.ALWAYS);
        progress.setPrefWidth(210);
        HBox bar = new HBox(16, activity, progress);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 24, 14, 24));
        bar.getStyleClass().add("app-footer");
        return bar;
    }

    private void discoverDevices() {
        String executable = adbPath.getText().strip();
        activity.setText("Поиск ADB-устройств…");
        Task<List<WatchDevice>> task = new Task<>() {
            @Override protected List<WatchDevice> call() throws Exception {
                return service.devices(executable);
            }
        };
        task.setOnSucceeded(event -> {
            List<WatchDevice> ready = task.getValue().stream().filter(WatchDevice::ready).toList();
            List<String> serials = ready.stream().map(WatchDevice::serial).toList();
            watchPicker.getItems().setAll(serials);
            watchPicker.setDisable(serials.isEmpty());
            if (serials.size() == 1) {
                watchPicker.getSelectionModel().selectFirst();
                connectionEndpoint.setText(serials.getFirst());
                deviceStatus.setText("Найдено: " + serials.getFirst());
            } else if (serials.isEmpty()) {
                deviceStatus.setText("Часы не обнаружены. Проверьте Wi-Fi и беспроводную отладку.");
            } else {
                connectionEndpoint.clear();
                deviceStatus.setText("Найдено " + serials.size() + " устройств. Выберите нужные часы.");
            }
            activity.setText("Поиск завершён");
        });
        task.setOnFailed(event -> failure("Ошибка поиска", task.getException()));
        worker.submit(task);
    }

    private void sendImages() {
        if (transferring || files.getItems().isEmpty()) return;
        transfer(List.copyOf(files.getItems()), null);
    }

    private void retryFailures() {
        if (transferring) return;
        String serial = connectionEndpoint.getText().strip();
        if (previousDevice == null || !previousDevice.equals(serial)) {
            activity.setText("Для повтора выберите те же часы, что использовались при отправке.");
            return;
        }
        List<TransferUpdate> pending = latestResults.values().stream()
                .filter(TransferUpdate::retryable)
                .filter(update -> files.getItems().contains(update.source()))
                .toList();
        if (!pending.isEmpty()) transfer(List.of(), pending);
    }

    private void transfer(List<Path> selected, List<TransferUpdate> retryRequests) {
        String executable = adbPath.getText().strip();
        String serial = connectionEndpoint.getText().strip();
        if (serial.isBlank()) {
            activity.setText("Сначала подключите часы и укажите порт отладки.");
            return;
        }
        boolean isRetry = retryRequests != null;
        int total = isRetry ? retryRequests.size() : selected.size();
        if (total == 0) return;
        if (!isRetry) {
            latestResults.clear();
            previousDevice = serial;
        }
        transferring = true;
        activeTransfer = null;
        cancelTransfer.setDisable(false);
        updateCount();
        history.getItems().clear();
        progress.setProgress(0);
        AtomicInteger errors = new AtomicInteger();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                Consumer<TransferUpdate> report = update -> {
                    if (update.status() != TransferUpdate.Status.COPIED_SCAN_REQUESTED) {
                        errors.incrementAndGet();
                    }
                    Platform.runLater(() -> {
                        latestResults.put(update.source(), update);
                        progress.setProgress((double) update.position() / update.total());
                        String result = switch (update.status()) {
                            case COPIED_SCAN_REQUESTED -> "Скопировано, индексация запрошена";
                            case COPIED_SCAN_FAILED -> "Скопировано, ошибка индексации";
                            case FAILED -> "Не удалось скопировать";
                        };
                        history.getItems().add(update.source().getFileName() + " — " + result);
                        activity.setText(update.position() + "/" + update.total() + " · " + result);
                        updateCount();
                    });
                };
                if (isRetry) service.retry(executable, serial, retryRequests, report);
                else service.send(executable, serial, selected, report);
                return null;
            }
        };
        task.setOnSucceeded(event -> {
            finishTransfer();
            activity.setText("Передача завершена: " + (total - errors.get())
                    + " успешно, " + errors.get() + " с ошибками");
        });
        task.setOnFailed(event -> {
            finishTransfer();
            failure("Передача прервана", task.getException());
        });
        task.setOnCancelled(event -> {
            cancelTransfer.setDisable(true);
            activity.setText("Отмена запрошена. Завершаем ADB-процесс…");
        });
        activeTransfer = task;
        // A canceled JavaFX Task can emit onCancelled before its subprocess exits.
        // Keep the UI locked until the actual worker has returned.
        worker.submit(() -> {
            try {
                task.run();
            } finally {
                Platform.runLater(() -> {
                    if (activeTransfer == task && transferring) finishTransfer();
                    if (task.isCancelled()) activity.setText("Передача остановлена");
                });
            }
        });
    }

    private void runOperation(String message, Operation operation, Runnable success) {
        activity.setText(message);
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception { operation.run(); return null; }
        };
        task.setOnSucceeded(event -> { success.run(); activity.setText("Готово"); });
        task.setOnFailed(event -> failure("Ошибка ADB", task.getException()));
        worker.submit(task);
    }

    private void chooseFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите фотографии");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "JPEG и PNG", "*.jpg", "*.jpeg", "*.png", "*.JPG", "*.JPEG", "*.PNG"));
        List<File> result = chooser.showOpenMultipleDialog(getScene().getWindow());
        if (result != null) result.forEach(file -> importPath(file.toPath()));
    }

    private void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Выберите папку");
        File result = chooser.showDialog(getScene().getWindow());
        if (result != null) importPath(result.toPath());
    }

    private void importPath(Path path) {
        if (!Files.isDirectory(path)) {
            addImage(path);
            return;
        }
        activity.setText("Ищем фотографии в папке…");
        Task<List<Path>> scan = new Task<>() {
            @Override protected List<Path> call() throws Exception {
                return PhotoCollector.collect(path);
            }
        };
        scan.setOnSucceeded(event -> {
            int before = files.getItems().size();
            scan.getValue().forEach(this::addImage);
            activity.setText("Добавлено " + (files.getItems().size() - before) + " фотографий");
        });
        scan.setOnFailed(event -> failure("Не удалось прочитать папку", scan.getException()));
        worker.submit(scan);
    }

    private void addImage(Path image) {
        if (PhotoFiles.isReadableImage(image) && !files.getItems().contains(image)) {
            files.getItems().add(image);
        }
    }

    private void updateCount() {
        count.setText(files.getItems().size() + " файлов");
        send.setDisable(transferring || files.getItems().isEmpty());
        retryFailed.setDisable(transferring || previousDevice == null
                || latestResults.values().stream().noneMatch(update ->
                        update.retryable() && files.getItems().contains(update.source())));
    }

    private void finishTransfer() {
        transferring = false;
        activeTransfer = null;
        cancelTransfer.setDisable(true);
        updateCount();
    }

    private void failure(String label, Throwable cause) {
        activity.setText(label + ": " + (cause == null ? "Неизвестная ошибка" : cause.getMessage()));
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

    private static String defaultAdb() {
        String env = System.getenv("WGS_ADB");
        if (env != null && !env.isBlank()) return env;
        String name = System.getProperty("os.name", "").toLowerCase().contains("win") ? "adb.exe" : "adb";
        // In a jpackage app image, java.home points inside the bundled runtime.
        // A Platform-Tools folder alongside runtime/ is a portable, install-free option.
        Path imageRoot = Path.of(System.getProperty("java.home")).toAbsolutePath().getParent();
        if (imageRoot != null) {
            Path bundledTools = imageRoot.resolve("platform-tools").resolve(name);
            if (Files.isRegularFile(bundledTools)) return bundledTools.toString();
        }
        Path documentsTools = Path.of(System.getProperty("user.home"), "Documents", "platform-tools", name);
        return Files.isRegularFile(documentsTools) ? documentsTools.toString() : name;
    }

    @FunctionalInterface
    private interface Operation { void run() throws Exception; }
}
