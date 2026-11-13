package com.gathin.tvhome;

/** Figma's 1920x1080 dock, expressed in dp at density 2; shrinks to fit narrower grids. */
final class DockLayout {
    private DockLayout() { }
    static int tileWidth(int inner, int columns, float density) {
        if (inner <= 0 || columns <= 0) return Math.round(125 * density);
        return Math.max(1, Math.min(Math.round(125 * density), Math.round(inner / (float) columns - 24 * density)));
    }
    static float endPadding(int tileHeight, float scaleY, float density) {
        return Math.max(0f, 20 * density - tileHeight * (scaleY - 1f) / 2f);
    }
    static float scaledStart(float start, float pivot, float scale) {
        return start + pivot * (1f - scale);
    }
    static int tileHeight(int width) { return Math.round(width * 0.6f); }
    static float corner(int width) { return width * 0.12f; }
    static int parkedTop(int height, int tileHeight, float density) {
        // 6dp top decoration, 8dp item padding, 24dp bottom decoration and a 13dp next-row peek.
        return height - tileHeight - Math.round((16 + 30 + 8 + 13) * density);
    }
}
