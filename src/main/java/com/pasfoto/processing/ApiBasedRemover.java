package com.pasfoto.processing;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import javax.imageio.ImageIO;

public class ApiBasedRemover implements BackgroundRemover {
    private final String apiKey;

    public ApiBasedRemover(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public BufferedImage removeBackground(BufferedImage input) throws Exception {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("MASUKKAN_API_KEY_DISINI")) {
            throw new Exception("API Key remove.bg belum diisi! Silakan isi di PhotoController.java");
        }

        // 1. Ubah gambar menjadi format teks (Base64) agar mudah dikirim lewat HTTP
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(input, "png", baos);
        String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

        // 2. Siapkan format payload JSON
        String jsonPayload = "{\"image_file_b64\": \"" + base64Image + "\", \"size\": \"auto\"}";

        // 3. Kirim ke Server API
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.remove.bg/v1.0/removebg"))
                .header("X-Api-Key", apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "image/png")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        // 4. Terima balasan dari Server
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 200) {
            // 5. Ubah byte balasan kembali menjadi BufferedImage
            return ImageIO.read(new ByteArrayInputStream(response.body()));
        } else {
            throw new Exception("Error API Server: " + new String(response.body()));
        }
    }
}