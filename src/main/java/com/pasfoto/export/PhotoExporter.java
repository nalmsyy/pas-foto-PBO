package com.pasfoto.export;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

public class PhotoExporter {

    public void saveImage(BufferedImage image, Path outputPath) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        String format = getFormat(outputPath);
        BufferedImage imageToSave = image;

        if (format.equals("jpg") || format.equals("jpeg")) {
            imageToSave = removeAlphaForJpeg(image);
        }

        boolean success = ImageIO.write(imageToSave, format, outputPath.toFile());
        if (!success) {
            throw new IOException("Format output tidak didukung: " + format);
        }
    }

    public void savePdf(BufferedImage layoutImage, Path outputPath) throws IOException {
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        BufferedImage rgbImage = removeAlphaForJpeg(layoutImage);
        try (PDDocument document = new PDDocument()) {

            // --- PERBAIKAN: Deteksi Orientasi Otomatis ---
            float width = 432f;
            float height = 288f;

            // Jika gambar ternyata berbentuk Portrait (Tinggi > Lebar), tukar ukurannya!
            if (rgbImage.getWidth() < rgbImage.getHeight()) {
                width = 288f;
                height = 432f;
            }

            PDPage page = new PDPage(new PDRectangle(width, height));
            document.addPage(page);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(rgbImage, "jpg", baos);
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, baos.toByteArray(), "layout4R");

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.drawImage(pdImage, 0, 0, width, height);
            }

            document.save(outputPath.toFile());
        }
    }

    private String getFormat(Path outputPath) {
        String fileName = outputPath.getFileName().toString().toLowerCase(Locale.ROOT);
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "png";
        }
        String extension = fileName.substring(dot + 1);
        return extension.equals("jpeg") ? "jpg" : extension;
    }

    private BufferedImage removeAlphaForJpeg(BufferedImage input) {
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = output.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, output.getWidth(), output.getHeight());
        g.drawImage(input, 0, 0, null);
        g.dispose();
        return output;
    }
}
