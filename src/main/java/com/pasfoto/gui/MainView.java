package com.pasfoto.gui;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.pasfoto.batch.BatchProcessor;
import com.pasfoto.controller.PhotoController;
import com.pasfoto.export.PhotoExporter;
import com.pasfoto.export.PrintLayoutGenerator;
import com.pasfoto.model.BackgroundColor;
import com.pasfoto.model.PhotoSize;

import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class MainView {

    private final PhotoController controller;
    private final Stage stage;
    private final BorderPane root = new BorderPane();

    private final ImageView originalPreview = new ImageView();
    private final ImageView resultPreview = new ImageView();
    private final Label statusLabel = new Label("Siap. Pilih foto terlebih dahulu.");
    private File selectedFile;
    private BufferedImage lastResult;

    private final TextField customWidth = new TextField("30");
    private final TextField customHeight = new TextField("40");
    private final ColorPicker customColorPicker = new ColorPicker(javafx.scene.paint.Color.WHITE);

    public MainView(PhotoController controller, Stage stage) {
        this.controller = controller;
        this.stage = stage;
        buildLayout();
    }

    public Parent getRoot() {
        return root;
    }

    private void buildLayout() {
        ComboBox<String> methodCombo = new ComboBox<>(FXCollections.observableArrayList("Threshold", "GrabCut", "API"));
        methodCombo.setValue("Threshold");

        // --- UBAH DEFAULT UKURAN KE CUSTOM ---
        ComboBox<PhotoSize> sizeCombo = new ComboBox<>(FXCollections.observableArrayList(PhotoSize.values()));
        sizeCombo.setValue(PhotoSize.CUSTOM);

        // --- UBAH DEFAULT WARNA KE 'TIDAK ADA' (NONE) ---
        ComboBox<BackgroundColor> colorCombo = new ComboBox<>(FXCollections.observableArrayList(BackgroundColor.values()));
        colorCombo.setValue(BackgroundColor.NONE);

        CheckBox autoCenterCheck = new CheckBox("Auto-center wajah");
        autoCenterCheck.setSelected(true);

        customWidth.setPrefWidth(45);
        customHeight.setPrefWidth(45);

        HBox customSizeBox = new HBox(5, customWidth, new Label("x"), customHeight, new Label("mm"));
        customSizeBox.setAlignment(Pos.CENTER);
        // Karena defaultnya CUSTOM, kotak ukuran harus langsung ditampilkan (true)
        customSizeBox.setVisible(true);
        customSizeBox.setManaged(true);

        sizeCombo.setOnAction(e -> {
            boolean isCustom = (sizeCombo.getValue() == PhotoSize.CUSTOM);
            customSizeBox.setVisible(isCustom);
            customSizeBox.setManaged(isCustom);
        });
        HBox sizeContainer = new HBox(5, sizeCombo, customSizeBox);
        sizeContainer.setAlignment(Pos.CENTER_LEFT);

        customColorPicker.setVisible(false);
        customColorPicker.setManaged(false);

        colorCombo.setOnAction(e -> {
            boolean isCustom = (colorCombo.getValue() == BackgroundColor.CUSTOM);
            customColorPicker.setVisible(isCustom);
            customColorPicker.setManaged(isCustom);
        });
        HBox colorContainer = new HBox(5, colorCombo, customColorPicker);
        colorContainer.setAlignment(Pos.CENTER_LEFT);

        Button uploadButton = new Button("Upload Foto");
        Button batchButton = new Button("Mode Batch");
        Button saveButton = new Button("Simpan Hasil");
        Button printPdfButton = new Button("Cetak Layout 4R (PDF)");

        Button processButton = new Button("PROSES");
        processButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; -fx-font-weight: bold;");

        uploadButton.setOnAction(event -> uploadImage());
        saveButton.setOnAction(event -> saveResult());
        printPdfButton.setOnAction(event -> printLayoutPdf());

        processButton.setOnAction(event -> {
            applyCustomSettings(sizeCombo.getValue(), colorCombo.getValue(), customWidth, customHeight, customColorPicker);
            processImage(sizeCombo.getValue(), colorCombo.getValue(), autoCenterCheck.isSelected(), methodCombo.getValue());
        });

        batchButton.setOnAction(event -> {
            applyCustomSettings(sizeCombo.getValue(), colorCombo.getValue(), customWidth, customHeight, customColorPicker);
            processBatch(sizeCombo.getValue(), colorCombo.getValue(), autoCenterCheck.isSelected(), methodCombo.getValue());
        });

        Separator sep1 = new Separator(javafx.geometry.Orientation.VERTICAL);
        HBox barisAtas = new HBox(10, uploadButton, batchButton, sep1, saveButton, printPdfButton);
        barisAtas.setAlignment(Pos.CENTER_LEFT);

        Separator sep2 = new Separator(javafx.geometry.Orientation.VERTICAL);
        HBox barisBawah = new HBox(10,
                new Label("Metode:"), methodCombo,
                new Label("Ukuran:"), sizeContainer,
                new Label("Background:"), colorContainer,
                sep2,
                autoCenterCheck,
                processButton
        );
        barisBawah.setAlignment(Pos.CENTER_LEFT);

        VBox toolbar = new VBox(10, barisAtas, barisBawah);
        toolbar.setPadding(new Insets(12));
        toolbar.setStyle("-fx-border-color: #cccccc; -fx-border-width: 0 0 1 0; -fx-background-color: #f8f9fa;");

        originalPreview.setPreserveRatio(true);
        originalPreview.setFitWidth(480);
        originalPreview.setFitHeight(560);
        resultPreview.setPreserveRatio(true);
        resultPreview.setFitWidth(480);
        resultPreview.setFitHeight(560);

        VBox originalBox = createPreviewBox("Foto Asli", originalPreview);
        VBox resultBox = createPreviewBox("Hasil Pas Foto", resultPreview);

        HBox previewArea = new HBox(15, originalBox, resultBox);
        previewArea.setPadding(new Insets(12));
        HBox.setHgrow(originalBox, Priority.ALWAYS);
        HBox.setHgrow(resultBox, Priority.ALWAYS);

        statusLabel.setPadding(new Insets(10));

        root.setTop(toolbar);
        root.setCenter(previewArea);
        root.setBottom(statusLabel);
    }

    private void applyCustomSettings(PhotoSize size, BackgroundColor color, TextField wBox, TextField hBox, ColorPicker picker) {
        if (size == PhotoSize.CUSTOM) {
            try {
                double w = Double.parseDouble(wBox.getText());
                double h = Double.parseDouble(hBox.getText());
                size.setDimensions(w, h);
            } catch (NumberFormatException e) {
                statusLabel.setText("Peringatan: Input ukuran kustom tidak valid. Menggunakan ukuran default.");
            }
        }

        if (color == BackgroundColor.CUSTOM) {
            javafx.scene.paint.Color fxColor = picker.getValue();
            java.awt.Color awtColor = new java.awt.Color(
                    (float) fxColor.getRed(),
                    (float) fxColor.getGreen(),
                    (float) fxColor.getBlue(),
                    (float) fxColor.getOpacity()
            );
            color.setAwtColor(awtColor);
        }
    }

    private VBox createPreviewBox(String title, ImageView imageView) {
        Label label = new Label(title);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        VBox box = new VBox(10, label, imageView);
        box.setPadding(new Insets(12));
        box.setAlignment(Pos.TOP_CENTER);
        box.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 8; -fx-background-radius: 8;");
        return box;
    }

    private void uploadImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Pilih Foto");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.bmp"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        try {
            selectedFile = file;
            BufferedImage image = controller.loadImage(file);
            originalPreview.setImage(SwingFXUtils.toFXImage(image, null));
            resultPreview.setImage(null);
            lastResult = null;
            statusLabel.setText("Foto dipilih: " + file.getName());

            int originalWidthMm = (int) Math.round((image.getWidth() * 25.4) / 300.0);
            int originalHeightMm = (int) Math.round((image.getHeight() * 25.4) / 300.0);

            customWidth.setText(String.valueOf(originalWidthMm));
            customHeight.setText(String.valueOf(originalHeightMm));

        } catch (Exception e) {
            showError("Gagal membuka foto", e);
        }
    }

    private void processImage(PhotoSize size, BackgroundColor color, boolean autoCenter, String method) {
        if (selectedFile == null) {
            statusLabel.setText("Pilih foto terlebih dahulu.");
            return;
        }

        try {
            statusLabel.setText("Memproses foto...");
            lastResult = controller.processSingle(selectedFile, size, color, autoCenter, method);
            resultPreview.setImage(SwingFXUtils.toFXImage(lastResult, null));
            statusLabel.setText("Selesai diproses. Silakan simpan hasil.");
        } catch (Exception e) {
            showError("Gagal memproses foto", e);
        }
    }

    private void saveResult() {
        if (lastResult == null) {
            statusLabel.setText("Belum ada hasil untuk disimpan.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Simpan Pas Foto");
        chooser.setInitialFileName("pasfoto.png");
        // HANYA EKSPOR KE PNG KARENA JPG TIDAK MENDUKUNG TRANSPARANSI
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        File output = chooser.showSaveDialog(stage);
        if (output == null) {
            return;
        }

        try {
            controller.exportImage(lastResult, Path.of(output.toURI()));
            statusLabel.setText("Berhasil disimpan: " + output.getAbsolutePath());
        } catch (Exception e) {
            showError("Gagal menyimpan hasil", e);
        }
    }

    private void printLayoutPdf() {
        if (lastResult == null) {
            statusLabel.setText("Belum ada hasil untuk dicetak. Silakan proses foto terlebih dahulu.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Simpan Layout Cetak 4R");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File output = chooser.showSaveDialog(stage);

        if (output == null) {
            return;
        }

        try {
            statusLabel.setText("Membuat layout 4R...");
            List<BufferedImage> photos = Collections.nCopies(8, lastResult);
            PrintLayoutGenerator generator = new PrintLayoutGenerator();
            BufferedImage layout = generator.generate4RLayout(photos);
            new PhotoExporter().savePdf(layout, Path.of(output.toURI()));
            statusLabel.setText("Layout PDF berhasil disimpan di: " + output.getAbsolutePath());
        } catch (Exception e) {
            showError("Gagal membuat PDF", e);
        }
    }

    private void processBatch(PhotoSize size, BackgroundColor color, boolean autoCenter, String method) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Pilih Beberapa Foto (Batch)");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png"));
        List<File> files = fileChooser.showOpenMultipleDialog(stage);

        if (files == null || files.isEmpty()) {
            return;
        }

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Pilih Folder Penyimpanan Hasil Batch");
        File outputDir = dirChooser.showDialog(stage);

        if (outputDir == null) {
            return;
        }

        try {
            statusLabel.setText("Memproses " + files.size() + " foto... Mohon tunggu.");
            BatchProcessor bp = new BatchProcessor();
            bp.process(files, outputDir.toPath(), controller, size, color, autoCenter, method, "png");
            statusLabel.setText("Pemrosesan Batch Selesai! Hasil tersimpan di: " + outputDir.getAbsolutePath());
        } catch (Exception e) {
            showError("Gagal memproses Batch", e);
        }
    }

    private void showError(String title, Exception e) {
        e.printStackTrace();
        statusLabel.setText(title + ": " + e.getMessage());
    }
}
