package com.pasfoto.processing;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Optional;

public class FaceCenterer {
    public BufferedImage centerFace(BufferedImage input, Optional<Rectangle> face) {
        if (face.isEmpty()) {
            return input;
        }

        Rectangle rect = face.get();
        int imageCenterX = input.getWidth() / 2;
        int targetFaceCenterY = (int) (input.getHeight() * 0.38);

        int faceCenterX = rect.x + rect.width / 2;
        int faceCenterY = rect.y + rect.height / 2;

        int dx = imageCenterX - faceCenterX;
        int dy = targetFaceCenterY - faceCenterY;

        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = output.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(input, dx, dy, null);
        g.dispose();
        return output;
    }
}
