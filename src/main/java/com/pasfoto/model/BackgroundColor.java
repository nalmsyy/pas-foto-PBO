package com.pasfoto.model;

import java.awt.Color;

public enum BackgroundColor {
    NONE(new Color(0, 0, 0, 0)), // Warna dengan transparansi 100%
    RED(new Color(220, 0, 0)),
    BLUE(new Color(0, 80, 180)),
    WHITE(Color.WHITE),
    CUSTOM(Color.WHITE);

    private Color awtColor;

    BackgroundColor(Color awtColor) {
        this.awtColor = awtColor;
    }

    public Color getAwtColor() {
        return awtColor;
    }

    public void setAwtColor(Color awtColor) {
        this.awtColor = awtColor;
    }

    @Override
    public String toString() {
        return switch (this) {
            case NONE ->
                "Tidak Ada";
            case RED ->
                "Merah";
            case BLUE ->
                "Biru";
            case WHITE ->
                "Putih";
            case CUSTOM ->
                "Custom";
        };
    }
}
