package com.pasfoto.model;

import java.awt.Color;

public enum BackgroundColor {
    RED(new Color(220, 0, 0)),
    BLUE(new Color(0, 80, 180)),
    WHITE(Color.WHITE),
    CUSTOM(Color.WHITE);

    private Color awtColor; // HAPUS KATA 'final' DI SINI

    BackgroundColor(Color awtColor) {
        this.awtColor = awtColor;
    }

    public Color getAwtColor() {
        return awtColor;
    }

    // TAMBAHKAN FUNGSI SETTER INI
    public void setAwtColor(Color awtColor) {
        this.awtColor = awtColor;
    }

    @Override
    public String toString() {
        return switch (this) {
            case RED -> "Merah";
            case BLUE -> "Biru";
            case WHITE -> "Putih";
            case CUSTOM -> "Custom";
        };
    }
}
