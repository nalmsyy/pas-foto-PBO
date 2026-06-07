package com.pasfoto.processing;

import io.github.cdimascio.dotenv.Dotenv;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

public class ApiBasedRemover implements BackgroundRemover {

    private final String apiUrl;
    private final String apiHeader;
    private final String apiKey;

    public ApiBasedRemover() {
        // Memuat nilai konfigurasi dari file .env secara otomatis
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        this.apiUrl = dotenv.get("API_URL", "https://api.remove.bg/v1.0/removebg");
        this.apiHeader = dotenv.get("API_HEADER_NAME", "X-Api-Key");
        this.apiKey = dotenv.get("API_KEY_VALUE", "");
    }

    @Override
    public BufferedImage removeBackground(BufferedImage input) throws Exception {
        if (apiKey.isEmpty() || apiKey.equals("TULIS_API_KEY_ASLI_KAMU_DISINI")) {
            throw new Exception("API Key belum diisi di file .env!");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(input, "png", baos);
        String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

        String jsonPayload = "{\"image_file_b64\": \"" + base64Image + "\", \"size\": \"auto\"}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl)) // Menggunakan URL dari .env
                .header(apiHeader, apiKey) // Menggunakan Header & Key dari .env
                .header("Content-Type", "application/json")
                .header("Accept", "image/png")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        if (response.statusCode() == 200) {
            return ImageIO.read(new ByteArrayInputStream(response.body()));
        } else {
            throw new Exception("Error API: " + new String(response.body()));
        }
    }
}
