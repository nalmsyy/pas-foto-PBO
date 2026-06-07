package com.pasfoto.processing;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import com.pasfoto.model.PhotoSize;

public class PhotoFormatter {
    public static final int DEFAULT_DPI = 300;

    public BufferedImage cropAndResize(BufferedImage input, PhotoSize size) {
        return cropAndResize(input, size, DEFAULT_DPI);
    }

    public BufferedImage cropAndResize(BufferedImage input, PhotoSize size, int dpi) {
        int targetW = size.getWidthPx(dpi);
        int targetH = size.getHeightPx(dpi);
        double targetAspect = size.getAspectRatio();

        int cropW = input.getWidth();
        int cropH = input.getHeight();
        double currentAspect = (double) cropW / cropH;

        if (currentAspect > targetAspect) {
            cropW = (int) Math.round(cropH * targetAspect);
        } else {
            cropH = (int) Math.round(cropW / targetAspect);
        }

        int cropX = Math.max(0, (input.getWidth() - cropW) / 2);
        int cropY = Math.max(0, (input.getHeight() - cropH) / 2);

        BufferedImage cropped = input.getSubimage(cropX, cropY, cropW, cropH);
        BufferedImage resized = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(cropped, 0, 0, targetW, targetH, null);
        g.dispose();
        return resized;
    }
}
