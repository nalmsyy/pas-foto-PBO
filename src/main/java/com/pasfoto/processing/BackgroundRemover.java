package com.pasfoto.processing;

import java.awt.image.BufferedImage;

public interface BackgroundRemover {
    BufferedImage removeBackground(BufferedImage input) throws Exception;
}
