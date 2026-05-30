package com.pasfoto.controller;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import com.pasfoto.export.PhotoExporter;
import com.pasfoto.model.BackgroundColor;
import com.pasfoto.model.PhotoSize;
import com.pasfoto.processing.ApiBasedRemover;
import com.pasfoto.processing.BackgroundRemover;
import com.pasfoto.processing.BackgroundReplacer;
import com.pasfoto.processing.ColorThresholdRemover;
import com.pasfoto.processing.FaceCenterer;
import com.pasfoto.processing.FaceDetector;
import com.pasfoto.processing.GrabCutRemover;
import com.pasfoto.processing.ImageLoader;
import com.pasfoto.processing.PhotoFormatter;

public class PhotoController {
    private final ImageLoader imageLoader = new ImageLoader();
    private final FaceDetector faceDetector = new FaceDetector();
    private final FaceCenterer faceCenterer = new FaceCenterer();
    private final BackgroundReplacer backgroundReplacer = new BackgroundReplacer();
    private final PhotoFormatter photoFormatter = new PhotoFormatter();
    private final PhotoExporter photoExporter = new PhotoExporter();

    public BufferedImage loadImage(File file) throws Exception {
        return imageLoader.load(file.toPath());
    }

    public BufferedImage processSingle(
            File inputFile,
            PhotoSize photoSize,
            BackgroundColor backgroundColor,
            boolean autoCenter,
            String removerMethod
    ) throws Exception {
        BufferedImage original = loadImage(inputFile);
        BackgroundRemover remover = createRemover(removerMethod);

        BufferedImage transparent = remover.removeBackground(original);

        if (autoCenter) {
            Optional<Rectangle> face = faceDetector.detectLargestFace(transparent);
            transparent = faceCenterer.centerFace(transparent, face);
        }

        BufferedImage replaced = backgroundReplacer.replaceTransparentBackground(
                transparent,
                backgroundColor.getAwtColor()
        );

        return photoFormatter.cropAndResize(replaced, photoSize);
    }

    public Path processAndExport(
            File inputFile,
            Path outputDir,
            PhotoSize photoSize,
            BackgroundColor backgroundColor,
            boolean autoCenter,
            String removerMethod,
            String extension
    ) throws Exception {
        BufferedImage result = processSingle(inputFile, photoSize, backgroundColor, autoCenter, removerMethod);
        String baseName = stripExtension(inputFile.getName());
        Path outputPath = outputDir.resolve(baseName + "_pasfoto_" + photoSize.name().toLowerCase() + "." + extension);
        exportImage(result, outputPath);
        return outputPath;
    }

    public void exportImage(BufferedImage image, Path outputPath) throws Exception {
        photoExporter.saveImage(image, outputPath);
    }

    private BackgroundRemover createRemover(String method) {
        if (method == null) {
            return new ColorThresholdRemover();
        }

        return switch (method.toLowerCase()) {
            case "grabcut" -> new GrabCutRemover();
            case "api" -> new ApiBasedRemover("uMpooQfdQJS3A8YHv3Yx5mUk");
            case "threshold" -> new ColorThresholdRemover();
            default -> new ColorThresholdRemover();
        };
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
