package com.pasfoto.export;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

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

    public void savePdf(BufferedImage layoutImage, Path outputPath) {
        // TODO tahap lanjutan:
        // Tambahkan dependency PDFBox/iText, lalu tulis layoutImage ke PDF.
        throw new UnsupportedOperationException("Ekspor PDF belum diimplementasikan. Gunakan PNG/JPEG terlebih dahulu.");
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
