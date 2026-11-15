package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Inset glass edge, lit from the top left; three feathered strokes avoid live blur. */
final class GlassEdge extends Drawable {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final LinearGradient light = new LinearGradient(0, 0, 1, 1,
            new int[]{0x59FFFFFF, 0x28FFFFFF, 0x20FFFFFF}, new float[]{0f, .55f, 1f}, Shader.TileMode.CLAMP);
    private final Matrix matrix = new Matrix();
    private final RectF area = new RectF(), drawableBounds = new RectF();
    private final float width, radius;
    private int alpha = 255;

    /** A negative radius follows the app tile's existing proportional corners. */
    GlassEdge(float width, float radius) {
        this.width = width;
        this.radius = radius;
        paint.setStyle(Paint.Style.STROKE);
        paint.setShader(light);
    }

    void draw(Canvas canvas, RectF bounds) {
        matrix.setScale(bounds.width(), bounds.height());
        matrix.postTranslate(bounds.left, bounds.top);
        light.setLocalMatrix(matrix);
        float corner = radius < 0 ? DockLayout.corner(Math.round(bounds.width())) : radius;
        for (int pass = 3; pass >= 1; pass--) {
            float stroke = width * pass, inset = stroke / 2f;
            area.set(bounds); area.inset(inset, inset);
            paint.setStrokeWidth(stroke);
            paint.setAlpha(pass == 1 ? alpha : Math.round(alpha * .16f));
            float r = Math.max(0f, corner - inset);
            canvas.drawRoundRect(area, r, r, paint);
        }
    }

    @Override public void draw(Canvas canvas) {
        drawableBounds.set(getBounds());
        draw(canvas, drawableBounds);
    }
    @Override public void setAlpha(int value) { alpha = value; invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
