package com.pasfoto.gui;

import com.pasfoto.controller.PhotoController;
import com.pasfoto.model.BackgroundColor;
import com.pasfoto.model.PhotoSize;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

public class MainView {
    private final PhotoController controller;
    private final Stage stage;
    private final BorderPane root = new BorderPane();

    private final ImageView originalPreview = new ImageView();
    private final ImageView resultPreview = new ImageView();
    private final Label statusLabel = new Label("Siap. Pilih foto terlebih dahulu.");

    private File selectedFile;
    private BufferedImage lastResult;

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

        ComboBox<PhotoSize> sizeCombo = new ComboBox<>(FXCollections.observableArrayList(PhotoSize.values()));
        sizeCombo.setValue(PhotoSize.PAS_3X4);

        ComboBox<BackgroundColor> colorCombo = new ComboBox<>(FXCollections.observableArrayList(BackgroundColor.values()));
        colorCombo.setValue(BackgroundColor.RED);

        CheckBox autoCenterCheck = new CheckBox("Auto-center wajah");
        autoCenterCheck.setSelected(true);

        Button uploadButton = new Button("Upload Foto");
        Button processButton = new Button("Proses");
        Button saveButton = new Button("Simpan Hasil");

        uploadButton.setOnAction(event -> uploadImage());
        processButton.setOnAction(event -> processImage(sizeCombo.getValue(), colorCombo.getValue(), autoCenterCheck.isSelected(), methodCombo.getValue()));
        saveButton.setOnAction(event -> saveResult());

        HBox toolbar = new HBox(10,
                uploadButton,
                new Separator(),
                new Label("Metode:"), methodCombo,
                new Label("Ukuran:"), sizeCombo,
                new Label("Background:"), colorCombo,
                autoCenterCheck,
                processButton,
                saveButton
        );
        toolbar.setPadding(new Insets(12));
        toolbar.setAlignment(Pos.CENTER_LEFT);

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
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG", "*.png"),
                new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg")
        );

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

    private void showError(String title, Exception e) {
        e.printStackTrace();
        statusLabel.setText(title + ": " + e.getMessage());
    }
}
