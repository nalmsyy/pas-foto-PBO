package com.pasfoto.processing;

import java.awt.image.BufferedImage;

public class GrabCutRemover implements BackgroundRemover {
    private final BackgroundRemover fallback = new ColorThresholdRemover();

    @Override
    public BufferedImage removeBackground(BufferedImage input) throws Exception {
        // TODO tahap lanjutan:
        // 1. Tambahkan dependency OpenCV/JavaCV ke pom.xml.
        // 2. Convert BufferedImage ke Mat.
        // 3. Jalankan cv::grabCut dengan rectangle awal di area tengah.
        // 4. Convert mask foreground menjadi alpha channel.
        // Untuk sementara, kelas ini memakai fallback threshold agar alur aplikasi tetap bisa diuji.
        return fallback.removeBackground(input);
    }
}
