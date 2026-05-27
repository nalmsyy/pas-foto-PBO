package com.pasfoto.processing;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class BackgroundReplacer {
    public BufferedImage replaceTransparentBackground(BufferedImage input, Color backgroundColor) {
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                int rgba = input.getRGB(x, y);
                int alpha = (rgba >>> 24) & 0xFF;

                if (alpha < 10) {
                    output.setRGB(x, y, backgroundColor.getRGB() | 0xFF000000);
                } else if (alpha < 255) {
                    output.setRGB(x, y, blendOverBackground(rgba, backgroundColor));
                } else {
                    output.setRGB(x, y, rgba | 0xFF000000);
                }
            }
        }
        return output;
    }

    private int blendOverBackground(int rgba, Color bg) {
        int alpha = (rgba >>> 24) & 0xFF;
        double a = alpha / 255.0;
        Color fg = new Color(rgba, true);

        int r = (int) Math.round(fg.getRed() * a + bg.getRed() * (1 - a));
        int g = (int) Math.round(fg.getGreen() * a + bg.getGreen() * (1 - a));
        int b = (int) Math.round(fg.getBlue() * a + bg.getBlue() * (1 - a));

        return new Color(r, g, b).getRGB() | 0xFF000000;
    }
}
