package com.pasfoto.processing;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

public class FaceDetector {
    private CascadeClassifier cascade;

    public FaceDetector() {
        try {
            File cascadeFile = new File("haarcascade_frontalface_default.xml");
            if (!cascadeFile.exists()) {
                URL url = new URL("https://raw.githubusercontent.com/opencv/opencv/master/data/haarcascades/haarcascade_frontalface_default.xml");
                Files.copy(url.openStream(), cascadeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            cascade = new CascadeClassifier(cascadeFile.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Gagal memuat AI Cascade: " + e.getMessage());
        }
    }

    // Keyword 'synchronized' ditambahkan agar aman saat Mode Batch memproses secara paralel
    public synchronized Optional<Rectangle> detectLargestFace(BufferedImage image) {
        if (cascade == null || cascade.empty()) {
            return fallbackDetection(image);
        }

        // try-with-resources: Membersihkan objek C++ secara instan agar tidak terjadi memory leak/crash
        try (Java2DFrameConverter javaConverter = new Java2DFrameConverter();
             OpenCVFrameConverter.ToMat matConverter = new OpenCVFrameConverter.ToMat();
             Frame frame = javaConverter.convert(image);
             Mat matImage = matConverter.convert(frame).clone(); // Clone mengamankan C++ pointer
             Mat grayImage = new Mat();
             RectVector faces = new RectVector()) {
             
            if (matImage.channels() > 1) {
                opencv_imgproc.cvtColor(matImage, grayImage, opencv_imgproc.COLOR_BGR2GRAY);
            } else {
                matImage.copyTo(grayImage);
            }

            // Memindai koordinat wajah pada matriks gambar
            cascade.detectMultiScale(grayImage, faces);

            if (faces.size() == 0) {
                return fallbackDetection(image);
            }

            Rect largestFace = faces.get(0);
            long maxArea = largestFace.width() * largestFace.height();

            for (int i = 1; i < faces.size(); i++) {
                Rect face = faces.get(i);
                long area = face.width() * face.height();
                if (area > maxArea) {
                    largestFace = face;
                    maxArea = area;
                }
            }

            return Optional.of(new Rectangle(largestFace.x(), largestFace.y(), largestFace.width(), largestFace.height()));
            
        } catch (Exception e) {
            System.err.println("Error saat OpenCV deteksi wajah: " + e.getMessage());
            return fallbackDetection(image);
        }
    }

    private Optional<Rectangle> fallbackDetection(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();
        int faceW = Math.max(1, w / 4);
        int faceH = Math.max(1, h / 3);
        int x = (w - faceW) / 2;
        int y = Math.max(0, h / 5);
        return Optional.of(new Rectangle(x, y, faceW, faceH));
    }
}