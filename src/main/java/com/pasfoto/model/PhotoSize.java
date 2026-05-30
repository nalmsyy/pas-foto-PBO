package com.pasfoto.model;

public enum PhotoSize {
    PAS_2X3(20, 30),
    PAS_3X4(30, 40),
    PAS_4X6(40, 60),
    CUSTOM(30, 40);

    private double widthMm;  // HAPUS KATA 'final'
    private double heightMm; // HAPUS KATA 'final'

    PhotoSize(double widthMm, double heightMm) {
        this.widthMm = widthMm;
        this.heightMm = heightMm;
    }

    public double getWidthMm() { return widthMm; }
    public double getHeightMm() { return heightMm; }

    // TAMBAHKAN FUNGSI SETTER INI
    public void setDimensions(double widthMm, double heightMm) {
        this.widthMm = widthMm;
        this.heightMm = heightMm;
    }

    public double getAspectRatio() {
        return widthMm / heightMm;
    }

    public int getWidthPx(int dpi) {
        return mmToPixels(widthMm, dpi);
    }

    public int getHeightPx(int dpi) {
        return mmToPixels(heightMm, dpi);
    }

    private int mmToPixels(double mm, int dpi) {
        return Math.max(1, (int) Math.round((mm / 25.4) * dpi));
    }

    @Override
    public String toString() {
        return switch (this) {
            case PAS_2X3 -> "2x3 cm";
            case PAS_3X4 -> "3x4 cm";
            case PAS_4X6 -> "4x6 cm";
            case CUSTOM -> "Custom";
        };
    }
}