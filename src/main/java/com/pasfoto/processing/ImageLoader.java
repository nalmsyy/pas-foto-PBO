package com.pasfoto.processing;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ImageLoader {
    public BufferedImage load(Path path) throws IOException {
        BufferedImage image = ImageIO.read(path.toFile());
        if (image == null) {
            throw new IOException("File bukan gambar yang didukung: " + path);
        }
        return image;
    }

    public List<BufferedImage> loadAll(List<File> files) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        for (File file : files) {
            images.add(load(file.toPath()));
        }
        return images;
    }
}
