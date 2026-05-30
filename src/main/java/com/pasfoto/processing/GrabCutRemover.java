package com.pasfoto.processing;

import java.awt.image.BufferedImage;

import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;

public class GrabCutRemover implements BackgroundRemover {
    
    @Override
    public BufferedImage removeBackground(BufferedImage input) throws Exception {
        // 1. Konversi dari Java BufferedImage ke OpenCV Mat
        Java2DFrameConverter javaConverter = new Java2DFrameConverter();
        OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
        
        Mat sourceMat = matConverter.convert(javaConverter.convert(input));
        Mat imageRGB = new Mat();
        
        // GrabCut mewajibkan format warna 3-channel (Tanpa Alpha)
        if (sourceMat.channels() == 4) {
            opencv_imgproc.cvtColor(sourceMat, imageRGB, opencv_imgproc.COLOR_RGBA2RGB);
        } else {
            sourceMat.copyTo(imageRGB);
        }

        // 2. Siapkan wadah memori untuk algoritma OpenCV
        Mat mask = new Mat(imageRGB.size(), opencv_core.CV_8UC1);
        Mat bgdModel = new Mat();
        Mat fgdModel = new Mat();

        // 3. Prediksi Area Subjek: 
        // Kita asumsikan wajah ada di area 80% tengah gambar, menyisakan 10% jarak di kiri-kanan
        int x = (int) (imageRGB.cols() * 0.1);
        int y = (int) (imageRGB.rows() * 0.05); // Kepala mendekati batas atas
        int w = (int) (imageRGB.cols() * 0.8);
        int h = (int) (imageRGB.rows() * 0.95);
        Rect rect = new Rect(x, y, w, h);

        // 4. Eksekusi GrabCut (3 iterasi untuk menyeimbangkan kecepatan dan hasil)
        opencv_imgproc.grabCut(imageRGB, mask, rect, bgdModel, fgdModel, 3, opencv_imgproc.GC_INIT_WITH_RECT);

        // 5. Ekstrak Mask kembali ke Java
        BufferedImage maskImage = javaConverter.getBufferedImage(matConverter.convert(mask));
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);

        // 6. Gambar ulang foto, buang background-nya
        for (int r = 0; r < input.getHeight(); r++) {
            for (int c = 0; c < input.getWidth(); c++) {
                // Di OpenCV, pixel mask 1 & 3 = Orang. Pixel 0 & 2 = Latar Belakang
                int maskVal = maskImage.getRaster().getSample(c, r, 0);
                
                if (maskVal == opencv_imgproc.GC_FGD || maskVal == opencv_imgproc.GC_PR_FGD) {
                    output.setRGB(c, r, input.getRGB(c, r)); // Pertahankan tubuh orang
                } else {
                    output.setRGB(c, r, 0x00000000); // Jadikan transparan
                }
            }
        }
        
        // 7. Cegah kebocoran memori (Memory Leak) karena OpenCV menggunakan C++
        imageRGB.release(); mask.release(); bgdModel.release(); fgdModel.release(); sourceMat.release();
        
        return output;
    }
}