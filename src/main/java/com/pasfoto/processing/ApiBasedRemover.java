package com.pasfoto.processing;

import java.awt.image.BufferedImage;

public class ApiBasedRemover implements BackgroundRemover {
    private final String apiKey;

    public ApiBasedRemover(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public BufferedImage removeBackground(BufferedImage input) {
        // TODO tahap lanjutan:
        // Gunakan HttpClient atau OkHttp untuk multipart upload ke remove.bg, Pixelcut, Clipdrop, atau Cloudinary.
        // Jangan hard-code API key. Ambil dari environment variable.
        if (apiKey == null || apiKey.isBlank()) {
            throw new UnsupportedOperationException("API key belum disiapkan. Isi environment variable REMOVE_BG_API_KEY terlebih dahulu.");
        }
        throw new UnsupportedOperationException("Implementasi API-based remover belum ditambahkan.");
    }
}
