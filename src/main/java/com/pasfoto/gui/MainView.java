package com.pasfoto.gui;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.pasfoto.controller.PhotoController;
import com.pasfoto.export.PhotoExporter;
import com.pasfoto.model.BackgroundColor;
import com.pasfoto.model.PhotoSize;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainView {

    private final PhotoController controller;
    private final Stage stage;

    private final StackPane mainRoot = new StackPane();
    private final BorderPane baseLayout = new BorderPane();

    private final VBox loadingOverlay = new VBox(15);
    private final Label loadingText = new Label("Sedang memproses...");

    private final ImageView originalPreview = new ImageView();
    private final ImageView resultPreview = new ImageView();
    private final Label statusLabel = new Label("Siap. Pilih mode dan masukkan foto.");
    private File selectedFile;
    private BufferedImage lastResult;

    private final TextField customWidth = new TextField("30");
    private final TextField customHeight = new TextField("40");
    private final ColorPicker customColorPicker = new ColorPicker(javafx.scene.paint.Color.WHITE);

    private final ComboBox<String> methodCombo = new ComboBox<>(FXCollections.observableArrayList("Threshold", "GrabCut", "API"));
    private final ComboBox<PhotoSize> sizeCombo = new ComboBox<>(FXCollections.observableArrayList(PhotoSize.values()));
    private final ComboBox<BackgroundColor> colorCombo = new ComboBox<>(FXCollections.observableArrayList(BackgroundColor.values()));

    private final CheckBox autoCenterCheck = new CheckBox("Auto-center wajah (Fallback)");

    class BatchItem {

        File file;
        String status;
        Image thumbnail;
        BufferedImage processedImage;

        BatchItem(File file) {
            this.file = file;
            this.status = "Menunggu";
            this.thumbnail = new Image(file.toURI().toString(), 50, 50, true, true, true);
            this.processedImage = null;
        }
    }

    private final ObservableList<BatchItem> batchItems = FXCollections.observableArrayList();
    private final ListView<BatchItem> batchListView = new ListView<>(batchItems);

    private final Button btnProcessBatch = new Button("✨ PROSES AI BATCH");
    private final Button btnSaveBatch = new Button("💾 SIMPAN HASIL");
    private final Button btnPrintBatchPdf = new Button("🖨️ CETAK BATCH 4R");

    public MainView(PhotoController controller, Stage stage) {
        this.controller = controller;
        this.stage = stage;
        buildLayout();
    }

    public Parent getRoot() {
        return mainRoot;
    }

    private void buildLayout() {
        methodCombo.setValue("Threshold");
        sizeCombo.setValue(PhotoSize.CUSTOM);
        colorCombo.setValue(BackgroundColor.NONE);

        autoCenterCheck.setVisible(false);
        autoCenterCheck.setManaged(false);
        autoCenterCheck.setSelected(true);

        customWidth.setPrefWidth(45);
        customHeight.setPrefWidth(45);
        HBox customSizeBox = new HBox(5, customWidth, new Label("x"), customHeight, new Label("mm"));
        customSizeBox.setAlignment(Pos.CENTER_LEFT);

        sizeCombo.setOnAction(e -> {
            boolean isCustom = (sizeCombo.getValue() == PhotoSize.CUSTOM);
            customSizeBox.setVisible(isCustom);
            customSizeBox.setManaged(isCustom);
        });

        customColorPicker.setVisible(false);
        customColorPicker.setManaged(false);
        colorCombo.setOnAction(e -> {
            boolean isCustom = (colorCombo.getValue() == BackgroundColor.CUSTOM);
            customColorPicker.setVisible(isCustom);
            customColorPicker.setManaged(isCustom);
        });

        HBox settingsBar = new HBox(15,
                new Label("Metode AI:"), methodCombo,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                new Label("Ukuran:"), sizeCombo, customSizeBox,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                new Label("Background:"), colorCombo, customColorPicker,
                new Separator(javafx.geometry.Orientation.VERTICAL),
                autoCenterCheck
        );
        settingsBar.setAlignment(Pos.CENTER_LEFT);
        settingsBar.setPadding(new Insets(15));
        settingsBar.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");

        // ==========================================
        // TAB SINGLE MODE
        // ==========================================
        Button uploadButton = new Button("📸 Upload 1 Foto");
        Button saveButton = new Button("💾 Simpan Hasil");
        Button printPdfButton = new Button("🖨️ Cetak Layout 4R (PDF)");
        Button processButton = new Button("✨ PROSES FOTO INI");
        processButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold;");

        uploadButton.setOnAction(event -> uploadImage());
        saveButton.setOnAction(event -> saveResult());
        printPdfButton.setOnAction(event -> printLayoutPdf());
        processButton.setOnAction(event -> {
            applyCustomSettings(sizeCombo.getValue(), colorCombo.getValue(), customWidth, customHeight, customColorPicker);
            boolean useAutoCenter = autoCenterCheck.isVisible() && autoCenterCheck.isSelected();
            processImage(sizeCombo.getValue(), colorCombo.getValue(), useAutoCenter, methodCombo.getValue());
        });

        HBox singleActionBar = new HBox(10, uploadButton, processButton, new Separator(javafx.geometry.Orientation.VERTICAL), saveButton, printPdfButton);
        singleActionBar.setAlignment(Pos.CENTER_LEFT);
        singleActionBar.setPadding(new Insets(10));

        originalPreview.setPreserveRatio(true);
        originalPreview.setFitHeight(450);
        originalPreview.setFitWidth(350);
        resultPreview.setPreserveRatio(true);
        resultPreview.setFitHeight(450);
        resultPreview.setFitWidth(350);
        VBox originalBox = createPreviewBox("Foto Asli", originalPreview);
        VBox resultBox = createPreviewBox("Hasil Pas Foto", resultPreview);

        HBox singlePreviews = new HBox(20, originalBox, resultBox);
        singlePreviews.setAlignment(Pos.CENTER);
        singlePreviews.setPadding(new Insets(10));

        VBox singleTabContent = new VBox(10, singleActionBar, singlePreviews);
        Tab singleTab = new Tab("Mode Single", singleTabContent);
        singleTab.setClosable(false);

        // ==========================================
        // TAB BATCH MODE 
        // ==========================================
        batchListView.setCellFactory(param -> new ListCell<BatchItem>() {
            private final ImageView imageView = new ImageView();
            private final Label textLabel = new Label();
            private final Button btnDelete = new Button("❌");
            private final Pane spacer = new Pane();
            private final HBox layout = new HBox(10);

            {
                btnDelete.setStyle("-fx-background-color: transparent; -fx-text-fill: red; -fx-cursor: hand; -fx-font-weight: bold;");
                btnDelete.setOnAction(e -> {
                    BatchItem item = getItem();
                    if (item != null) {
                        batchItems.remove(item);
                        statusLabel.setText("Foto dihapus: " + item.file.getName());
                    }
                });
                HBox.setHgrow(spacer, Priority.ALWAYS);
                layout.setAlignment(Pos.CENTER_LEFT);
                layout.getChildren().addAll(imageView, textLabel, spacer, btnDelete);
            }

            @Override
            protected void updateItem(BatchItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    imageView.setImage(item.thumbnail);
                    textLabel.setText(String.format("Status: %s\nFile: %s", item.status, item.file.getName()));
                    setGraphic(layout);
                }
            }
        });

        Button btnAddBatch = new Button("➕ Tambah Banyak Foto");
        Button btnClearBatch = new Button("🗑️ Kosongkan Semua");

        btnProcessBatch.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px;");
        btnSaveBatch.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px;");
        btnPrintBatchPdf.setStyle("-fx-background-color: #ffc107; -fx-text-fill: black; -fx-font-weight: bold; -fx-padding: 10px;");

        btnAddBatch.setOnAction(e -> addBatchPhotos());
        btnClearBatch.setOnAction(e -> {
            batchItems.clear();
            statusLabel.setText("Semua antrean dibersihkan.");
        });

        btnProcessBatch.setOnAction(event -> {
            if (batchItems.isEmpty()) {
                statusLabel.setText("Tambahkan foto batch terlebih dahulu!");
                return;
            }
            applyCustomSettings(sizeCombo.getValue(), colorCombo.getValue(), customWidth, customHeight, customColorPicker);
            processBatchList(sizeCombo.getValue(), colorCombo.getValue(), autoCenterCheck.isSelected(), methodCombo.getValue());
        });

        btnSaveBatch.setOnAction(event -> saveBatchList());
        btnPrintBatchPdf.setOnAction(event -> {
            if (batchItems.isEmpty()) {
                statusLabel.setText("Daftar antrean kosong!");
                return;
            }
            startBatchPrintWorkflow();
        });

        HBox batchActionBox = new HBox(10, btnAddBatch, btnClearBatch);
        batchActionBox.setPadding(new Insets(10));

        HBox batchBottomActions = new HBox(10, btnProcessBatch, btnSaveBatch, btnPrintBatchPdf);
        batchBottomActions.setAlignment(Pos.CENTER);
        HBox.setHgrow(btnProcessBatch, Priority.ALWAYS);
        HBox.setHgrow(btnSaveBatch, Priority.ALWAYS);
        HBox.setHgrow(btnPrintBatchPdf, Priority.ALWAYS);
        btnProcessBatch.setMaxWidth(Double.MAX_VALUE);
        btnSaveBatch.setMaxWidth(Double.MAX_VALUE);
        btnPrintBatchPdf.setMaxWidth(Double.MAX_VALUE);

        VBox batchTabContent = new VBox(10, batchActionBox, batchListView, batchBottomActions);
        batchTabContent.setPadding(new Insets(10));
        VBox.setVgrow(batchListView, Priority.ALWAYS);

        Tab batchTab = new Tab("Mode Batch (Banyak Foto)", batchTabContent);
        batchTab.setClosable(false);

        TabPane tabPane = new TabPane(singleTab, batchTab);
        tabPane.setStyle("-fx-tab-min-width: 150px; -fx-tab-min-height: 30px;");

        // ==========================================
        // OVERLAY LOADING
        // ==========================================
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(80, 80);
        spinner.setStyle("-fx-progress-color: #007bff;");

        loadingText.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");

        loadingOverlay.getChildren().addAll(spinner, loadingText);
        loadingOverlay.setAlignment(Pos.CENTER);
        loadingOverlay.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
        loadingOverlay.setVisible(false);

        statusLabel.setPadding(new Insets(10));
        baseLayout.setTop(settingsBar);
        baseLayout.setCenter(tabPane);
        baseLayout.setBottom(statusLabel);

        mainRoot.getChildren().addAll(baseLayout, loadingOverlay);

        // =====================================================================
        // FITUR CERDAS: DRAG AND DROP (SMART CONTEXTUAL ROUTING)
        // =====================================================================
        mainRoot.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                boolean hasImage = event.getDragboard().getFiles().stream().anyMatch(this::isImageFile);
                if (hasImage) {
                    event.acceptTransferModes(TransferMode.COPY);
                }
            }
            event.consume();
        });

        mainRoot.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                List<File> validFiles = db.getFiles().stream().filter(this::isImageFile).toList();
                if (!validFiles.isEmpty()) {
                    handleDroppedFiles(validFiles, tabPane, singleTab, batchTab);
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    // --- HELPER UNTUK DRAG AND DROP ---
    private boolean isImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".bmp");
    }

    private void handleDroppedFiles(List<File> files, TabPane tabPane, Tab singleTab, Tab batchTab) {
        if (files.size() > 1) {
            // Jika LEBIH DARI 1 file: Selalu paksa masuk ke mode Batch
            tabPane.getSelectionModel().select(batchTab);
            for (File f : files) {
                batchItems.add(new BatchItem(f));
            }
            statusLabel.setText(files.size() + " foto ditambahkan ke antrean Batch (Drag & Drop).");
        } else if (files.size() == 1) {
            // Jika HANYA 1 file
            File file = files.get(0);

            // Cek apakah user sedang membuka Tab Batch DAN isi batch tidak kosong
            if (tabPane.getSelectionModel().getSelectedItem() == batchTab && !batchItems.isEmpty()) {
                // Berarti user berniat menambahkan 1 foto ini ke antrean batch yang sedang berjalan
                batchItems.add(new BatchItem(file));
                statusLabel.setText("Foto ditambahkan ke antrean Batch: " + file.getName());
            } else {
                // Di luar kondisi di atas, paksa masuk ke mode Single
                tabPane.getSelectionModel().select(singleTab);
                loadSingleImageFromFile(file);
            }
        }
    }
    // ----------------------------------

    private void showLoading(boolean show, String message) {
        Platform.runLater(() -> {
            loadingText.setText(message);
            loadingOverlay.setVisible(show);
            baseLayout.setDisable(show);
        });
    }

    private void applyCustomSettings(PhotoSize size, BackgroundColor color, TextField wBox, TextField hBox, ColorPicker picker) {
        if (size == PhotoSize.CUSTOM) {
            try {
                double w = Double.parseDouble(wBox.getText());
                double h = Double.parseDouble(hBox.getText());
                size.setDimensions(w, h);
            } catch (NumberFormatException e) {
            }
        }
        if (color == BackgroundColor.CUSTOM) {
            javafx.scene.paint.Color fxColor = picker.getValue();
            color.setAwtColor(new java.awt.Color((float) fxColor.getRed(), (float) fxColor.getGreen(), (float) fxColor.getBlue(), (float) fxColor.getOpacity()));
        }
    }

    private VBox createPreviewBox(String title, ImageView imageView) {
        Label label = new Label(title);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        VBox box = new VBox(10, label, imageView);
        box.setPadding(new Insets(10));
        box.setAlignment(Pos.TOP_CENTER);
        box.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 8; -fx-background-radius: 8;");
        return box;
    }

    private BufferedImage forceCenterObject(BufferedImage formattedImage) {
        int width = formattedImage.getWidth();
        int height = formattedImage.getHeight();
        int minX = width, minY = height, maxX = 0, maxY = 0;
        boolean found = false;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = (formattedImage.getRGB(x, y) >> 24) & 0xff;
                if (alpha > 0) {
                    if (x < minX) {
                        minX = x;
                    }
                    if (x > maxX) {
                        maxX = x;
                    }
                    if (y < minY) {
                        minY = y;
                    }
                    if (y > maxY) {
                        maxY = y;
                    }
                    found = true;
                }
            }
        }
        if (!found) {
            return formattedImage;
        }

        BufferedImage croppedObject = formattedImage.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
        BufferedImage centeredOutput = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = centeredOutput.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        int drawX = (width - croppedObject.getWidth()) / 2;
        int drawY = height - croppedObject.getHeight();
        if (drawY < 0) {
            drawY = 0;
        }

        g.drawImage(croppedObject, drawX, drawY, null);
        g.dispose();

        return centeredOutput;
    }

    // --- FUNGSI BARU: Dipisahkan agar bisa dipanggil oleh Upload Button & Drag and Drop ---
    private void loadSingleImageFromFile(File file) {
        try {
            selectedFile = file;
            BufferedImage image = controller.loadImage(file);
            originalPreview.setImage(SwingFXUtils.toFXImage(image, null));
            resultPreview.setImage(null);
            lastResult = null;
            statusLabel.setText("Foto dipilih: " + file.getName());

            boolean wajahDitemukan = true; // <-- TAUTKAN DENGAN FACEDETECTOR-MU

            if (wajahDitemukan) {
                autoCenterCheck.setVisible(true);
                autoCenterCheck.setManaged(true);
                statusLabel.setText(statusLabel.getText() + " | Wajah terdeteksi! Opsi Fallback Auto-Center diaktifkan.");
            } else {
                autoCenterCheck.setVisible(false);
                autoCenterCheck.setManaged(false);
                statusLabel.setText(statusLabel.getText() + " | (Tidak ada wajah)");
            }

            int originalWidthMm = (int) Math.round((image.getWidth() * 25.4) / 300.0);
            int originalHeightMm = (int) Math.round((image.getHeight() * 25.4) / 300.0);
            customWidth.setText(String.valueOf(originalWidthMm));
            customHeight.setText(String.valueOf(originalHeightMm));
        } catch (Exception e) {
            showError("Gagal membuka foto", e);
        }
    }

    private void uploadImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Pilih Foto");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png"));
        File file = chooser.showOpenDialog(stage);

        if (file != null) {
            loadSingleImageFromFile(file);
        }
    }

    private void processImage(PhotoSize size, BackgroundColor color, boolean autoCenter, String method) {
        if (selectedFile == null) {
            return;
        }
        showLoading(true, "Memotong background... Menggunakan " + method);
        new Thread(() -> {
            try {
                BufferedImage result = controller.processSingle(selectedFile, size, color, autoCenter, method);
                if (autoCenter && color == BackgroundColor.NONE) {
                    result = forceCenterObject(result);
                }
                final BufferedImage finalRes = result;
                Platform.runLater(() -> {
                    lastResult = finalRes;
                    resultPreview.setImage(SwingFXUtils.toFXImage(lastResult, null));
                    statusLabel.setText("Selesai diproses. Silakan simpan hasil.");
                    showLoading(false, "");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Gagal memproses foto", e);
                    showLoading(false, "");
                });
            }
        }).start();
    }

    private void saveResult() {
        if (lastResult == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Simpan Pas Foto");
        chooser.setInitialFileName("pasfoto.jpg");
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("JPEG Image", "*.jpg", "*.jpeg"), new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        File output = chooser.showSaveDialog(stage);
        if (output != null) {
            try {
                controller.exportImage(lastResult, Path.of(output.toURI()));
                statusLabel.setText("Berhasil disimpan: " + output.getAbsolutePath());
            } catch (Exception e) {
                showError("Gagal menyimpan hasil", e);
            }
        }
    }

    private void printLayoutPdf() {
        if (lastResult == null) {
            statusLabel.setText("Proses foto terlebih dahulu sebelum mencetak!");
            return;
        }
        List<BufferedImage> singleList = new ArrayList<>();
        singleList.add(lastResult);
        openAdvancedBatchPreviewWindow(singleList, false);
    }

    private void addBatchPhotos() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih Beberapa Foto (Batch)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png"));
        List<File> files = fileChooser.showOpenMultipleDialog(stage);
        if (files != null && !files.isEmpty()) {
            for (File f : files) {
                batchItems.add(new BatchItem(f));
            }
            statusLabel.setText(files.size() + " foto ditambahkan ke antrean.");
        }
    }

    private void processBatchList(PhotoSize size, BackgroundColor color, boolean autoCenter, String method) {
        showLoading(true, "Menganalisis dan Memotong AI " + batchItems.size() + " foto...");
        new Thread(() -> {
            for (BatchItem item : batchItems) {
                Platform.runLater(() -> {
                    item.status = "Proses AI...";
                    batchListView.refresh();
                });
                try {
                    boolean useAutoCenter = autoCenterCheck.isVisible() && autoCenterCheck.isSelected();
                    BufferedImage res = controller.processSingle(item.file, size, color, useAutoCenter, method);
                    if (useAutoCenter && color == BackgroundColor.NONE) {
                        res = forceCenterObject(res);
                    }
                    item.processedImage = res;
                    Platform.runLater(() -> {
                        item.status = "Selesai Diproses";
                        batchListView.refresh();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        item.status = "Gagal AI";
                        batchListView.refresh();
                    });
                }
            }
            Platform.runLater(() -> {
                statusLabel.setText("Proses AI selesai! Silakan 'Simpan Hasil' atau 'Cetak 4R'.");
                showLoading(false, "");
            });
        }).start();
    }

    private void saveBatchList() {
        boolean hasProcessed = batchItems.stream().anyMatch(item -> item.processedImage != null);
        if (!hasProcessed) {
            statusLabel.setText("Belum ada foto yang diproses! Klik PROSES AI BATCH terlebih dahulu.");
            return;
        }

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Pilih Folder Penyimpanan File JPEG");
        File outputDir = dirChooser.showDialog(stage);
        if (outputDir == null) {
            return;
        }

        showLoading(true, "Menyimpan seluruh foto ke folder...");
        new Thread(() -> {
            for (BatchItem item : batchItems) {
                if (item.processedImage != null) {
                    try {
                        String baseName = item.file.getName();
                        int dot = baseName.lastIndexOf('.');
                        if (dot > 0) {
                            baseName = baseName.substring(0, dot);
                        }
                        Path outputPath = outputDir.toPath().resolve(baseName + "_pasfoto.jpg");
                        controller.exportImage(item.processedImage, outputPath);
                        Platform.runLater(() -> {
                            item.status = "Tersimpan";
                            batchListView.refresh();
                        });
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                            item.status = "Gagal Simpan";
                            batchListView.refresh();
                        });
                    }
                }
            }
            Platform.runLater(() -> {
                statusLabel.setText("Foto disimpan di: " + outputDir.getAbsolutePath());
                showLoading(false, "");
            });
        }).start();
    }

    private void startBatchPrintWorkflow() {
        List<BufferedImage> processedList = new ArrayList<>();
        for (BatchItem item : batchItems) {
            if (item.processedImage != null) {
                processedList.add(item.processedImage);
            }
        }

        if (processedList.isEmpty()) {
            statusLabel.setText("Belum ada foto siap cetak! Klik PROSES AI BATCH dulu.");
            return;
        }

        List<String> opsi = List.of("Gabung (Foto campur dalam lembar 4R yang sama)", "Pisah (1 lembar penuh duplikat 1 foto yang sama)");
        ChoiceDialog<String> dialog = new ChoiceDialog<>(opsi.get(0), opsi);
        dialog.setTitle("Pilihan Konfigurasi Cetak Batch");
        dialog.setHeaderText("Bagaimana Anda ingin menyusun foto di kertas 4R?");

        var resultOption = dialog.showAndWait();
        if (resultOption.isEmpty()) {
            return;
        }

        boolean isCombined = resultOption.get().startsWith("Gabung");
        openAdvancedBatchPreviewWindow(processedList, isCombined);
    }

    class MoveRecord {

        final StackPane node;
        final double prevX, prevY;
        final double prevRotate;

        MoveRecord(StackPane node, double prevX, double prevY, double prevRotate) {
            this.node = node;
            this.prevX = prevX;
            this.prevY = prevY;
            this.prevRotate = prevRotate;
        }
    }

    class PageData {

        Pane canvas;
        Stack<MoveRecord> undoHistory = new Stack<>();
        Map<StackPane, double[]> initialPositions = new HashMap<>();
        boolean isSelectedToPrint = true;
    }

    private StackPane createInteractiveImageNode(Image img, double w, double h, double startX, double startY, PageData pd, ComboBox<String> toolMode, BooleanProperty showPrintBorder) {

        ImageView imageView = new ImageView(img);
        imageView.setFitWidth(w);
        imageView.setFitHeight(h);

        Rectangle printBorder = new Rectangle(w, h);
        printBorder.setFill(Color.TRANSPARENT);
        printBorder.setStroke(Color.BLACK);
        printBorder.setStrokeWidth(1.0);
        printBorder.visibleProperty().bind(showPrintBorder);

        Rectangle dragGuide = new Rectangle(w, h);
        dragGuide.setFill(Color.TRANSPARENT);
        dragGuide.setStroke(Color.GRAY);
        dragGuide.setStrokeWidth(2.0);
        dragGuide.getStrokeDashArray().addAll(5d, 5d);
        dragGuide.setVisible(false);

        StackPane wrapper = new StackPane(imageView, printBorder, dragGuide);
        wrapper.setLayoutX(startX);
        wrapper.setLayoutY(startY);

        final double[] dragDelta = new double[2];
        final double[] dragStartPos = new double[3];

        wrapper.setOnMousePressed(event -> {
            String mode = toolMode.getValue();

            if (mode.contains("Hapus")) {
                pd.canvas.getChildren().remove(wrapper);
                return;
            }

            if (mode.contains("Kloning")) {
                StackPane clone = createInteractiveImageNode(img, w, h, wrapper.getLayoutX(), wrapper.getLayoutY(), pd, toolMode, showPrintBorder);
                pd.canvas.getChildren().add(clone);

                dragDelta[0] = wrapper.getLayoutX() - event.getSceneX();
                dragDelta[1] = wrapper.getLayoutY() - event.getSceneY();
                wrapper.toFront();

                dragGuide.setVisible(true);
                return;
            }

            dragStartPos[0] = wrapper.getLayoutX();
            dragStartPos[1] = wrapper.getLayoutY();
            dragStartPos[2] = wrapper.getRotate();

            if (event.getButton() == MouseButton.SECONDARY) {
                wrapper.setRotate(wrapper.getRotate() + 90);
                pd.undoHistory.push(new MoveRecord(wrapper, dragStartPos[0], dragStartPos[1], dragStartPos[2]));
                return;
            }

            dragDelta[0] = wrapper.getLayoutX() - event.getSceneX();
            dragDelta[1] = wrapper.getLayoutY() - event.getSceneY();
            wrapper.setCursor(javafx.scene.Cursor.CLOSED_HAND);
            wrapper.toFront();

            dragGuide.setVisible(true);
        });

        wrapper.setOnMouseDragged(event -> {
            String mode = toolMode.getValue();
            if ((mode.contains("Geser") || mode.contains("Kloning")) && event.getButton() == MouseButton.PRIMARY) {
                wrapper.setLayoutX(event.getSceneX() + dragDelta[0]);
                wrapper.setLayoutY(event.getSceneY() + dragDelta[1]);
            }
        });

        wrapper.setOnMouseReleased(event -> {
            wrapper.setCursor(javafx.scene.Cursor.DEFAULT);
            dragGuide.setVisible(false);

            String mode = toolMode.getValue();
            if ((mode.contains("Geser") || mode.contains("Kloning")) && event.getButton() == MouseButton.PRIMARY) {
                if (dragStartPos[0] != wrapper.getLayoutX() || dragStartPos[1] != wrapper.getLayoutY()) {
                    pd.undoHistory.push(new MoveRecord(wrapper, dragStartPos[0], dragStartPos[1], dragStartPos[2]));
                }
            }
        });

        return wrapper;
    }

    private void openAdvancedBatchPreviewWindow(List<BufferedImage> images, boolean isCombined) {
        Stage previewStage = new Stage();
        previewStage.initModality(Modality.APPLICATION_MODAL);
        previewStage.setTitle("Studio Cetak 4R Profesional");

        double previewW = images.get(0).getWidth() / 3.0;
        double previewH = images.get(0).getHeight() / 3.0;

        int margin = 15;
        int gap = 10;
        int cols = Math.max(1, (int) ((600 - margin * 2 + gap) / (previewW + gap)));
        int rows = Math.max(1, (int) ((400 - margin * 2 + gap) / (previewH + gap)));
        int capacity = cols * rows;

        int totalPages = isCombined ? (int) Math.ceil((double) images.size() / capacity) : images.size();

        List<PageData> pages = new ArrayList<>();
        StackPane canvasContainer = new StackPane();
        canvasContainer.setStyle("-fx-border-color: black; -fx-border-width: 2px;");

        ComboBox<String> toolMode = new ComboBox<>(FXCollections.observableArrayList(
                "🖐️ Geser & Rotasi", "➕ Kloning Objek (Tarik)", "❌ Hapus Objek (Klik)"
        ));
        toolMode.setValue("🖐️ Geser & Rotasi");
        toolMode.setStyle("-fx-font-weight: bold; -fx-background-color: #e2e3e5;");

        BooleanProperty showPrintBorderProperty = new SimpleBooleanProperty(true);
        CheckBox chkShowPrintBorder = new CheckBox("🔲 Cetak dengan Garis Tepi (Border)");
        chkShowPrintBorder.setSelected(true);
        chkShowPrintBorder.selectedProperty().bindBidirectional(showPrintBorderProperty);

        for (int p = 0; p < totalPages; p++) {
            PageData pd = new PageData();
            pd.canvas = new Pane();
            pd.canvas.setPrefSize(600, 400);
            pd.canvas.setStyle("-fx-background-color: white;");

            Rectangle canvasClip = new Rectangle(600, 400);
            pd.canvas.setClip(canvasClip);

            double startX = Math.max(margin, (600 - (cols * previewW + (cols - 1) * gap)) / 2);
            double startY = Math.max(margin, (400 - (rows * previewH + (rows - 1) * gap)) / 2);

            int imgStartIndex = p * capacity;
            for (int i = 0; i < capacity; i++) {
                int imgIndex = isCombined ? (imgStartIndex + i) : p;
                if (imgIndex >= images.size()) {
                    break;
                }

                Image fxImg = SwingFXUtils.toFXImage(images.get(imgIndex), null);
                double x = startX + (i % cols) * (previewW + gap);
                double y = startY + (i / cols) * (previewH + gap);

                StackPane imageNode = createInteractiveImageNode(fxImg, previewW, previewH, x, y, pd, toolMode, showPrintBorderProperty);
                pd.initialPositions.put(imageNode, new double[]{x, y});
                pd.canvas.getChildren().add(imageNode);
            }
            pages.add(pd);
            canvasContainer.getChildren().add(pd.canvas);
        }

        int[] currentPage = {0};
        Label lblPageIndicator = new Label();
        lblPageIndicator.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        CheckBox chkSelectPage = new CheckBox("Cetak Lembar Ini");
        chkSelectPage.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #28a745;");

        Button btnPrev = new Button("◀ Sebelumnya");
        Button btnNext = new Button("Selanjutnya ▶");

        Label lblOrientation = new Label("Orientasi: Landscape");
        lblOrientation.setStyle("-fx-font-weight: bold; -fx-text-fill: #17a2b8; -fx-font-size: 14px;");

        Button btnRotateCanvas = new Button("📄 Putar Kertas");
        btnRotateCanvas.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-weight: bold;");

        Runnable updateView = () -> {
            for (int i = 0; i < pages.size(); i++) {
                pages.get(i).canvas.setVisible(i == currentPage[0]);
            }
            lblPageIndicator.setText("Halaman " + (currentPage[0] + 1) + " dari " + totalPages);
            chkSelectPage.setSelected(pages.get(currentPage[0]).isSelectedToPrint);

            btnPrev.setDisable(currentPage[0] == 0);
            btnNext.setDisable(currentPage[0] == totalPages - 1);

            Pane activeCanvas = pages.get(currentPage[0]).canvas;
            if (activeCanvas.getPrefWidth() > activeCanvas.getPrefHeight()) {
                lblOrientation.setText("Orientasi: Landscape (6x4 inci)");
            } else {
                lblOrientation.setText("Orientasi: Portrait (4x6 inci)");
            }

            canvasContainer.setPrefSize(activeCanvas.getPrefWidth(), activeCanvas.getPrefHeight());
            previewStage.sizeToScene();
        };

        chkSelectPage.setOnAction(e -> pages.get(currentPage[0]).isSelectedToPrint = chkSelectPage.isSelected());
        btnPrev.setOnAction(e -> {
            currentPage[0]--;
            updateView.run();
        });
        btnNext.setOnAction(e -> {
            currentPage[0]++;
            updateView.run();
        });

        btnRotateCanvas.setOnAction(e -> {
            Pane currentCanvas = pages.get(currentPage[0]).canvas;
            double w = currentCanvas.getPrefWidth();
            double h = currentCanvas.getPrefHeight();
            currentCanvas.setPrefSize(h, w);

            Rectangle clip = (Rectangle) currentCanvas.getClip();
            if (clip != null) {
                clip.setWidth(h);
                clip.setHeight(w);
            }
            updateView.run();
        });

        Button btnUndo = new Button("↩ Undo");
        btnUndo.setStyle("-fx-background-color: #ffc107; -fx-font-weight: bold;");
        btnUndo.setOnAction(e -> {
            PageData pd = pages.get(currentPage[0]);
            if (!pd.undoHistory.isEmpty()) {
                MoveRecord lastMove = pd.undoHistory.pop();
                lastMove.node.setLayoutX(lastMove.prevX);
                lastMove.node.setLayoutY(lastMove.prevY);
                lastMove.node.setRotate(lastMove.prevRotate);
            }
        });

        Button btnReset = new Button("↻ Reset Layout");
        btnReset.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; -fx-font-weight: bold;");
        btnReset.setOnAction(e -> {
            PageData pd = pages.get(currentPage[0]);
            pd.canvas.getChildren().removeIf(node -> !pd.initialPositions.containsKey(node));
            for (Map.Entry<StackPane, double[]> entry : pd.initialPositions.entrySet()) {
                entry.getKey().setLayoutX(entry.getValue()[0]);
                entry.getKey().setLayoutY(entry.getValue()[1]);
                entry.getKey().setRotate(0);
            }
            pd.undoHistory.clear();
        });

        Button btnSimpanAll = new Button("Kunci & Cetak Halaman yang Dicentang");
        btnSimpanAll.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10px 20px;");
        btnSimpanAll.setOnAction(e -> {
            long selectedCount = pages.stream().filter(p -> p.isSelectedToPrint).count();
            if (selectedCount == 0) {
                statusLabel.setText("Tidak ada lembar yang dicentang untuk dicetak!");
                return;
            }

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Simpan PDF Layout Cetak");
            chooser.setInitialFileName(isCombined ? "cetak_4r_gabung.pdf" : "cetak_4r_pisah.pdf");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            File outputFile = chooser.showSaveDialog(previewStage);

            if (outputFile != null) {
                try {
                    statusLabel.setText("Mencetak " + selectedCount + " halaman ke PDF...");
                    int fileCounter = 1;

                    for (int i = 0; i < pages.size(); i++) {
                        PageData pd = pages.get(i);
                        if (pd.isSelectedToPrint) {
                            pd.canvas.setVisible(true);
                            pd.canvas.toFront();

                            SnapshotParameters params = new SnapshotParameters();
                            params.setFill(Color.WHITE);
                            WritableImage snapshot = pd.canvas.snapshot(params, null);
                            BufferedImage finalLayoutImage = SwingFXUtils.fromFXImage(snapshot, null);

                            File finalPath = outputFile;
                            if (selectedCount > 1) {
                                String origPath = outputFile.getAbsolutePath();
                                int dot = origPath.lastIndexOf('.');
                                String base = (dot > 0) ? origPath.substring(0, dot) : origPath;
                                String ext = (dot > 0) ? origPath.substring(dot) : ".pdf";
                                finalPath = new File(base + "_Hal_" + fileCounter + ext);
                            }

                            new PhotoExporter().savePdf(finalLayoutImage, finalPath.toPath());
                            fileCounter++;
                            pd.canvas.setVisible(false);
                        }
                    }

                    updateView.run();
                    statusLabel.setText("Sukses! PDF disimpan di: " + outputFile.getParent());
                    previewStage.close();
                } catch (Exception ex) {
                    showError("Gagal cetak PDF", ex);
                }
            }
        });

        HBox topNav = new HBox(20, btnPrev, lblPageIndicator, btnNext, new Separator(javafx.geometry.Orientation.VERTICAL), chkSelectPage);
        topNav.setAlignment(Pos.CENTER);

        HBox editControls = new HBox(15, new Label("Mode Alat:"), toolMode, chkShowPrintBorder);
        editControls.setAlignment(Pos.CENTER);

        HBox bottomControls = new HBox(15, lblOrientation, btnRotateCanvas, btnUndo, btnReset);
        bottomControls.setAlignment(Pos.CENTER);

        VBox layout = new VBox(15, topNav, canvasContainer, editControls, bottomControls, btnSimpanAll);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center; -fx-background-color: #f4f4f4;");

        updateView.run();
        previewStage.setScene(new Scene(layout));
        previewStage.showAndWait();
    }

    private void showError(String title, Exception e) {
        e.printStackTrace();
        statusLabel.setText(title + ": " + e.getMessage());
    }
}
