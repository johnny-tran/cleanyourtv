package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;
import java.util.ArrayDeque;

/** Figma BG's shared edge lighting and focus-only fill, baked once per backdrop/size. */
final class SidebarButtonMaterial extends Drawable {
    // Accessed only by the shared material worker; retain just the container and button shapes.
    private static final ArrayDeque<Lighting> LIGHTING = new ArrayDeque<>();

    static final class Lighting {
        final int width, height;
        final float corner, scale;
        final float[] highlights, shade;

        Lighting(int width, int height, float corner, float scale) {
            this.width = width; this.height = height; this.corner = corner; this.scale = scale;
            highlights = new float[width * height]; shade = new float[highlights.length];
            for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
                int i = y * width + x;
                highlights[i] = insetCoverage(x + .5f, y + .5f, width, height, corner, 40 * scale, 10 * scale)
                        + insetCoverage(x + .5f, y + .5f, width, height, corner, -40 * scale, 10 * scale);
                shade[i] = insetCoverage(x + .5f, y + .5f, width, height, corner, 40 * scale, 30 * scale);
            }
        }
    }

    static Lighting lighting(int width, int height, float corner, float scale) {
        for (Lighting item : LIGHTING) {
            if (item.width == width && item.height == height && item.corner == corner && item.scale == scale)
                return item;
        }
        Lighting item = new Lighting(width, height, corner, scale);
        if (LIGHTING.size() == 2) LIGHTING.removeFirst();
        LIGHTING.addLast(item);
        return item;
    }
    private final float designScale;
    private final View owner;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private Bitmap backdrop, normal, selected;
    private boolean focused;
    private int alpha = 255, pad;
    private int generation;
    private int materialWidth, materialHeight;
    private Runnable ready;
    private boolean requested;

    SidebarButtonMaterial(float density, View owner) {
        designScale = density / 2f;
        this.owner = owner;
    }

    void setBackdrop(Bitmap image, Runnable ready) {
        backdrop = image;
        this.ready = ready;
        requested = true;
        rebuild();
        invalidateSelf();
    }

    @Override protected void onBoundsChange(Rect bounds) {
        if (requested && (bounds.width() != materialWidth || bounds.height() != materialHeight)) rebuild();
    }

    private void rebuild() {
        int w = owner.getWidth(), h = owner.getHeight();
        if (w <= 0 || h <= 0) return;
        materialWidth = w; materialHeight = h;
        int version = ++generation;
        Bitmap sourceImage = backdrop;
        PopupMaterial.RENDERER.execute(() -> {
            int[] source = new int[w * h];
            if (sourceImage != null && sourceImage.getWidth() == w && sourceImage.getHeight() == h)
                sourceImage.getPixels(source, 0, w, 0, 0, w, h);
            else java.util.Arrays.fill(source, 0xFF20232A);
            int[] off = new int[source.length], on = new int[source.length];
            Lighting lighting = lighting(w, h, h / 2f, designScale);
            for (int i = 0; i < source.length; i++) {
                off[i] = shadePixel(source[i], false, lighting.highlights[i], lighting.shade[i]);
                on[i] = shadePixel(source[i], true, lighting.highlights[i], lighting.shade[i]);
            }
            int padding = Math.max(1, Math.round(24 * designScale));
            Bitmap nextNormal = bake(off, w, h, padding);
            Bitmap nextSelected = bake(on, w, h, padding);
            owner.post(() -> {
                if (version != generation || !owner.isAttachedToWindow() || owner.getBackground() != this) {
                    nextNormal.recycle(); nextSelected.recycle();
                    return;
                }
                pad = padding; normal = nextNormal; selected = nextSelected;
                invalidateSelf();
                if (ready != null) {
                    Runnable finished = ready;
                    ready = null;
                    finished.run();
                }
            });
        });
    }

    private Bitmap bake(int[] pixels, int w, int h, int pad) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        Bitmap result = Bitmap.createBitmap(w + 2 * pad, h + 2 * pad, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        RectF face = new RectF(pad, pad, pad + w, pad + h);
        float radius = h / 2f;
        paint.setStyle(Paint.Style.FILL); paint.setColor(0xFF000000);
        paint.setShadowLayer(15 * designScale, 0, 8 * designScale, 0x05000000);
        canvas.drawRoundRect(face, radius, radius, paint);
        paint.clearShadowLayer();
        Bitmap fill = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888);
        BitmapShader shader = new BitmapShader(fill, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        Matrix position = new Matrix(); position.setTranslate(pad, pad); shader.setLocalMatrix(position);
        paint.setShader(shader);
        canvas.drawRoundRect(face, radius, radius, paint);
        paint.setShader(null);
        // Figma's linear-burn hairline and lateral edge shadows become dark translucent strokes.
        paint.setStyle(Paint.Style.STROKE); paint.setColor(0x33000000);
        paint.setStrokeWidth(designScale);
        canvas.drawRoundRect(face, radius, radius, paint);
        paint.setColor(0x2F000000);
        paint.setStrokeWidth(.5f * designScale);
        canvas.save();
        canvas.clipRect(pad, pad, pad + w, pad + h);
        canvas.drawRoundRect(new RectF(pad - .5f * designScale, pad,
                pad + w + .5f * designScale, pad + h), radius, radius, paint);
        canvas.restore();
        return result;
    }

    // shortcut: smooth distance masks approximate Figma's blur/refraction; use a shader only if TV profiling permits.
    static float insetCoverage(float x, float y, float w, float h, float offsetY, float blur) {
        return insetCoverage(x, y, w, h, h / 2f, offsetY, blur);
    }

    static float insetCoverage(float x, float y, float w, float h, float corner, float offsetY, float blur) {
        float spread = Math.abs(offsetY), radius = corner + spread;
        float qx = Math.abs(x - w / 2f) - (w / 2f + spread - radius);
        float qy = Math.abs(y - h / 2f - offsetY) - (h / 2f + spread - radius);
        float dx = Math.max(qx, 0), dy = Math.max(qy, 0);
        float distance = (float) Math.sqrt(dx * dx + dy * dy)
                + Math.min(Math.max(qx, qy), 0) - radius;
        float phase = Math.max(0f, Math.min(1f, .5f + distance / blur));
        return phase * phase * (3f - 2f * phase);
    }

    static int shadePixel(int pixel, boolean focus, float highlights, float shade) {
        return 0xFF000000 | channel((pixel >> 16) & 255, focus, highlights, shade) << 16
                | channel((pixel >> 8) & 255, focus, highlights, shade) << 8
                | channel(pixel & 255, focus, highlights, shade);
    }

    private static int channel(int value, boolean focus, float highlights, float shade) {
        // Shared #101010 additive fill and neutral 4% luminosity lift.
        float result = Math.min(255, value + 16);
        result += (255 - result) * .04f;
        if (focus) {
            result = Math.min(255, result / (1f - 111f / 255f)); // #6f6f6f Color Dodge.
            result += (255 - result) * .65f; // White 65% Screen, below the shared lighting.
        }
        result += 40 * highlights - 25 * shade; // #282828 additive and #e6e6e6 linear burn.
        return Math.round(Math.max(0, Math.min(255, result)));
    }

    @Override public boolean isStateful() { return true; }
    @Override protected boolean onStateChange(int[] states) {
        boolean next = false;
        for (int state : states) if (state == android.R.attr.state_focused) next = true;
        if (next == focused) return false;
        focused = next; invalidateSelf(); return true;
    }
    @Override public void draw(Canvas canvas) {
        Bitmap image = focused ? selected : normal;
        if (image == null) return;
        paint.setAlpha(alpha); paint.setStyle(Paint.Style.FILL);
        canvas.drawBitmap(image, getBounds().left - pad, getBounds().top - pad, paint);
    }
    @Override public void setAlpha(int value) { alpha = value; invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
