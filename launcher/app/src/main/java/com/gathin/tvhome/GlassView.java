package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

/**
 * A translucent glass slab: soft sheen fill, bright top specular, crisp rim and a faint outer glow.
 * One instance floats over the grid and is moved with spring animations between tiles.
 */
public class GlassView extends View {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint specular = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private float radius;
    private final Size size = new Size();
    private ValueAnimator sizeAnimator;
    /** Glow colour as 0xRRGGBB: white normally, a festival's accent (diya gold) while a festive skin is on. */
    private int glowRgb = 0xFFFFFF;
    /** Glow strength: the white glow stays faint, a coloured one needs more alpha to read as warm light. */
    private int glowAlpha = 0x30;
    /** glass_style rim or halo: no sheen and no fill, only the crisp rim (and, for halo, a lighter outer glow). */
    private boolean flat;
    private boolean halo;

    public GlassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        radius = 20f * density;
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(1.5f * density);
        rim.setColor(0xCCFFFFFF);
        glow.setStyle(Paint.Style.STROKE);
        // Cache the slab as a texture. The spring only changes translation, so the GPU can
        // move it without replaying clipPath and the glow on every frame. That path is
        // software-rendered on the MT5895 and is what makes the highlight crawl.
        setLayerType(LAYER_TYPE_HARDWARE, null);
    }

    /** Tints the rim and the outer glow; 0 restores the plain white glass. */
    void setAccent(int argb) {
        if (argb == 0) {
            glowRgb = 0xFFFFFF;
            glowAlpha = 0x30;
            rim.setColor(0xCCFFFFFF);
        } else {
            glowRgb = argb & 0xFFFFFF;
            glowAlpha = 0x70;
            // The rim is the accent lifted towards white, so the edge still reads as glass, not paint.
            int r = (((glowRgb >> 16) & 0xFF) + 255 * 2) / 3, g = (((glowRgb >> 8) & 0xFF) + 255 * 2) / 3,
                    b = ((glowRgb & 0xFF) + 255 * 2) / 3;
            rim.setColor(0xE6000000 | (r << 16) | (g << 8) | b);
        }
        invalidate();
    }

    /** One of Config's GLASS_* styles. */
    void setStyle(String style) {
        flat = Config.GLASS_RIM.equals(style) || Config.GLASS_HALO.equals(style);
        halo = Config.GLASS_HALO.equals(style);
        rim.setStrokeWidth((flat ? 2f : 1.5f) * density);
        invalidate();
    }

    void setRadius(float px) {
        radius = px;
        invalidate();
    }

    /** Room for the stock glow plus a 1.5dp clear gap before the outline's inner edge. */
    float drawingInset() { return 6f * density + outlineOffset(); }

    private float outlineOffset() { return 1.5f * density + rim.getStrokeWidth() / 2f; }

    /** Retarget from the currently drawn bounds, keeping the rim's stroke width constant during a resize. */
    void resize(int width, int height, float corner, boolean animate) {
        boolean changed = size.retarget(width, height, corner);
        if (!changed && animate) return;
        if (sizeAnimator != null) sizeAnimator.cancel();
        // Reserve drawing room once; changing LayoutParams every animation frame would relayout Home.
        ViewGroup.LayoutParams lp = getLayoutParams();
        int roomWidth = Math.max(lp.width, width), roomHeight = Math.max(lp.height, height);
        if (roomWidth != lp.width || roomHeight != lp.height) {
            lp.width = roomWidth;
            lp.height = roomHeight;
            setLayoutParams(lp);
        }
        if (!animate) {
            size.at(1f);
            applySize();
            return;
        }
        sizeAnimator = ValueAnimator.ofFloat(0f, 1f);
        sizeAnimator.setDuration(240);
        sizeAnimator.setInterpolator(new DecelerateInterpolator(1.5f));
        sizeAnimator.addUpdateListener(animation -> {
            size.at((float) animation.getAnimatedValue());
            applySize();
        });
        sizeAnimator.start();
    }

    boolean isSizedFor(int width, int height) {
        return size.targetWidth == width && size.targetHeight == height;
    }

    private void applySize() {
        radius = size.radius;
        updateShaders(size.height);
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateShaders(size.targetHeight > 0 ? size.height : h);
    }

    private void updateShaders(float h) {
        fill.setShader(new LinearGradient(0, 0, 0, h, 0x3DFFFFFF, 0x12FFFFFF, Shader.TileMode.CLAMP));
        specular.setShader(new LinearGradient(0, 0, 0, h * 0.45f, 0x66FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float inset = drawingInset();
        float width = size.targetWidth > 0 ? size.width : getWidth();
        float height = size.targetHeight > 0 ? size.height : getHeight();
        rect.set(inset, inset, width - inset, height - inset);

        // Outer glow: thin rings across the inset, each a little fainter, so it fades out smoothly. Three wide
        // strokes left visible bands once the glow took an app's colour at a stronger alpha. The hardware layer
        // caches the result, so the rings cost one draw, not one per frame of the spring.
        if (!flat || halo) {
            int rings = 12;
            float step = 6f * density / rings;
            int alpha = flat ? glowAlpha / 2 : glowAlpha;
            glow.setStrokeWidth(step + 0.5f);
            for (int i = 0; i < rings; i++) {
                float o = outlineOffset() + step * (i + 0.5f);
                float t = 1f - (float) i / rings;
                glow.setColor(Math.round(alpha * t) << 24 | glowRgb);
                canvas.drawRoundRect(rect.left - o, rect.top - o, rect.right + o, rect.bottom + o, radius + o, radius + o, glow);
            }
        }
        if (flat) {
            drawRim(canvas);
            return;
        }

        canvas.drawRoundRect(rect, radius, radius, fill);

        // Sheen across the top 45%: clipRect stays on the GPU (clipPath did not on the MT5895) and
        // the round rect under it keeps the top corners rounded with a flat bottom edge.
        canvas.save();
        canvas.clipRect(rect.left, rect.top, rect.right, rect.top + rect.height() * 0.45f);
        canvas.drawRoundRect(rect, radius, radius, specular);
        canvas.restore();

        drawRim(canvas);
    }

    /** An offset outline leaves the tile edge untouched, with background visible in the gap. */
    private void drawRim(Canvas canvas) {
        float offset = outlineOffset();
        canvas.drawRoundRect(rect.left - offset, rect.top - offset, rect.right + offset, rect.bottom + offset,
                radius + offset, radius + offset, rim);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (sizeAnimator != null) sizeAnimator.cancel();
        super.onDetachedFromWindow();
    }

    /** Resize state is separate from the View so interrupted transitions can be regression-tested on the JVM. */
    static final class Size {
        float width, height, radius;
        int targetWidth, targetHeight;
        private float targetRadius, fromWidth, fromHeight, fromRadius;

        boolean retarget(int w, int h, float r) {
            if (targetWidth == w && targetHeight == h && targetRadius == r) return false;
            fromWidth = width;
            fromHeight = height;
            fromRadius = radius;
            targetWidth = w;
            targetHeight = h;
            targetRadius = r;
            return true;
        }

        void at(float fraction) {
            width = fromWidth + (targetWidth - fromWidth) * fraction;
            height = fromHeight + (targetHeight - fromHeight) * fraction;
            radius = fromRadius + (targetRadius - fromRadius) * fraction;
        }
    }
}
