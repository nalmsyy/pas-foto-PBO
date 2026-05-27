package com.pasfoto.processing;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ColorThresholdRemover implements BackgroundRemover {
    private final double threshold;

    public ColorThresholdRemover() {
        this(65.0);
    }

    public ColorThresholdRemover(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public BufferedImage removeBackground(BufferedImage input) {
        BufferedImage argb = toArgb(input);
        Color background = estimateBackgroundColor(argb);
        BufferedImage output = new BufferedImage(argb.getWidth(), argb.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < argb.getHeight(); y++) {
            for (int x = 0; x < argb.getWidth(); x++) {
                int rgba = argb.getRGB(x, y);
                Color pixel = new Color(rgba, true);
                double distance = colorDistance(pixel, background);

                if (distance < threshold) {
                    output.setRGB(x, y, 0x00000000);
                } else {
                    output.setRGB(x, y, rgba | 0xFF000000);
                }
            }
        }
        return output;
    }

    private BufferedImage toArgb(BufferedImage input) {
        BufferedImage converted = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        converted.getGraphics().drawImage(input, 0, 0, null);
        return converted;
    }

    private Color estimateBackgroundColor(BufferedImage image) {
        int[][] points = {
                {0, 0},
                {image.getWidth() - 1, 0},
                {0, image.getHeight() - 1},
                {image.getWidth() - 1, image.getHeight() - 1}
        };

        int r = 0;
        int g = 0;
        int b = 0;

        for (int[] point : points) {
            Color color = new Color(image.getRGB(point[0], point[1]), true);
            r += color.getRed();
            g += color.getGreen();
            b += color.getBlue();
        }

        return new Color(r / points.length, g / points.length, b / points.length);
    }

    private double colorDistance(Color a, Color b) {
        int dr = a.getRed() - b.getRed();
        int dg = a.getGreen() - b.getGreen();
        int db = a.getBlue() - b.getBlue();
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }
}
