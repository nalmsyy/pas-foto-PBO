package com.pasfoto.processing;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Optional;

public class FaceDetector {
    public Optional<Rectangle> detectLargestFace(BufferedImage image) {
        // TODO tahap lanjutan:
        // Integrasikan Haar Cascade OpenCV untuk deteksi wajah asli.
        // Placeholder ini mengembalikan area perkiraan di tengah agar pipeline auto-center bisa diuji.
        int w = image.getWidth();
        int h = image.getHeight();
        int faceW = Math.max(1, w / 4);
        int faceH = Math.max(1, h / 3);
        int x = (w - faceW) / 2;
        int y = Math.max(0, h / 5);
        return Optional.of(new Rectangle(x, y, faceW, faceH));
    }
}
