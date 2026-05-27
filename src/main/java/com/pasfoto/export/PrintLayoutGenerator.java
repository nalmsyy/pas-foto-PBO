package com.pasfoto.export;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

public class PrintLayoutGenerator {
    public static final int DEFAULT_DPI = 300;
    public static final int PAGE_4R_WIDTH_PX = 6 * DEFAULT_DPI;
    public static final int PAGE_4R_HEIGHT_PX = 4 * DEFAULT_DPI;

    public BufferedImage generate4RLayout(List<BufferedImage> photos) {
        if (photos == null || photos.isEmpty()) {
            throw new IllegalArgumentException("Daftar foto kosong.");
        }

        BufferedImage page = new BufferedImage(PAGE_4R_WIDTH_PX, PAGE_4R_HEIGHT_PX, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = page.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, page.getWidth(), page.getHeight());

        BufferedImage sample = photos.get(0);
        int margin = 40;
        int gap = 25;
        int cols = Math.max(1, (page.getWidth() - margin * 2 + gap) / (sample.getWidth() + gap));
        int rows = Math.max(1, (page.getHeight() - margin * 2 + gap) / (sample.getHeight() + gap));
        int capacity = cols * rows;

        int totalW = cols * sample.getWidth() + (cols - 1) * gap;
        int totalH = rows * sample.getHeight() + (rows - 1) * gap;
        int startX = Math.max(margin, (page.getWidth() - totalW) / 2);
        int startY = Math.max(margin, (page.getHeight() - totalH) / 2);

        for (int i = 0; i < Math.min(capacity, photos.size()); i++) {
            int row = i / cols;
            int col = i % cols;
            int x = startX + col * (sample.getWidth() + gap);
            int y = startY + row * (sample.getHeight() + gap);
            g.drawImage(photos.get(i), x, y, null);
        }

        g.dispose();
        return page;
    }
}
