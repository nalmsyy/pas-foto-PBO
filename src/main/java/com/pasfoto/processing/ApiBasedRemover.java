package com.pasfoto.processing;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

import javax.imageio.ImageIO;

import io.github.cdimascio.dotenv.Dotenv;

public class ApiBasedRemover implements BackgroundRemover {

    private final String apiUrl;
    private final String apiHeader;
    private final String apiKey;

    public ApiBasedRemover() {
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

        // Tambahkan timeout 15 detik agar aplikasi tidak freeze selamanya saat sinyal jelek
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header(apiHeader, apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "image/png")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        try {
            HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                return ImageIO.read(new ByteArrayInputStream(response.body()));
            } else {
                throw new Exception("Ditolak Server API (Kode " + response.statusCode() + "): " + new String(response.body()));
            }

            // --- INI PENAMBAHAN UNTUK MENANGKAP INTERNET MATI ---
        } catch (java.net.UnknownHostException | java.net.ConnectException e) {
            throw new Exception("Koneksi Gagal! Pastikan komputer terhubung ke internet.");
        } catch (java.net.http.HttpTimeoutException e) {
            throw new Exception("Koneksi Terputus (Timeout)! Jaringan internet sangat lambat.");
        } catch (Exception e) {
            throw new Exception("Terjadi kesalahan jaringan: " + e.getMessage());
        }
    }
}
