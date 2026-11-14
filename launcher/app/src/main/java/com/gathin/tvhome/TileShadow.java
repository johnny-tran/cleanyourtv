package com.gathin.tvhome;

import android.animation.ObjectAnimator;
import android.graphics.Canvas;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;

/** A card-local background shadow, excluded from row measurement and never moved between cards. */
final class TileShadow extends Drawable {
    private final View card, parent;
    private final Drawable original;
    private final GlassView renderer;
    private ObjectAnimator animator;
    private int alpha;
    private float focusScale;
    private Bitmap radiosity;
    private int restWidth, restHeight;
    private float restCorner;
    private final Paint restPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF restBounds = new RectF();

    private TileShadow(View card) {
        this.card = card;
        parent = (View) card.getParent();
        original = parent.getBackground();
        renderer = new GlassView(card.getContext(), null);
        renderer.setStyle(Config.GLASS_LIFT);
        renderer.setLayerType(View.LAYER_TYPE_NONE, null);
        renderer.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
    }

    static void prepare(View card, String style) {
        if (!Config.GLASS_LIFT.equals(style) || !(card.getParent() instanceof View)
                || card.getTag(R.id.tile_shadow) != null) return;
        TileShadow shadow = new TileShadow(card);
        card.setTag(R.id.tile_shadow, shadow);
        background(shadow.parent, shadow);
    }

    static void animate(View card, boolean focused, boolean animations, boolean reducedMotion, float scale) {
        if (!(card.getParent() instanceof View)) return;
        TileShadow shadow = (TileShadow) card.getTag(R.id.tile_shadow);
        if (shadow == null) {
            prepare(card, Config.GLASS_LIFT);
            shadow = (TileShadow) card.getTag(R.id.tile_shadow);
        }
        if (shadow.animator != null) shadow.animator.cancel();
        shadow.focusScale = scale;
        shadow.animator = ObjectAnimator.ofInt(shadow, "alpha", shadow.alpha, focused ? 255 : 0);
        shadow.animator.setDuration(!animations ? 0 : reducedMotion ? Motion.REDUCED_MS : focused ? 260 : 180);
        shadow.animator.setInterpolator(new DecelerateInterpolator(1.5f));
        shadow.animator.start();
    }

    private static void background(View view, Drawable drawable) {
        int l = view.getPaddingLeft(), t = view.getPaddingTop(), r = view.getPaddingRight(), b = view.getPaddingBottom();
        view.setBackground(drawable);
        view.setPadding(l, t, r, b);
    }

    @Override public void draw(Canvas canvas) {
        if (original != null) { original.setBounds(getBounds()); original.draw(canvas); }
        if (card.getVisibility() != View.VISIBLE || card.getWidth() == 0) return;
        drawRadiosity(canvas);
        if (alpha == 0) return;
        float pad = renderer.drawingInset();
        int width = Math.round(card.getWidth() * focusScale + 2 * pad);
        int height = Math.round(card.getHeight() * focusScale + 2 * pad);
        if (!renderer.isSizedFor(width, height)) {
            Outline outline = new Outline();
            card.getOutlineProvider().getOutline(card, outline);
            renderer.resize(width, height, Math.max(0f, outline.getRadius()) * focusScale, false);
            renderer.layout(0, 0, width, height);
        }
        float cx = card.getLeft() + card.getTranslationX() + card.getWidth() / 2f;
        float cy = card.getTop() + card.getTranslationY() + card.getHeight() / 2f;
        canvas.save();
        canvas.scale(card.getScaleX() / focusScale, card.getScaleY() / focusScale, cx, cy);
        canvas.translate(cx - width / 2f, cy - height / 2f);
        canvas.saveLayerAlpha(0, 0, width, height, alpha);
        renderer.draw(canvas);
        canvas.restore(); canvas.restore();
    }

    /** Figma layer blur is twice Gaussian sigma; Skia's mask radius converts back to sigma. */
    static float radiosityBlurRadius(int tileWidth) {
        return Math.max(.1f, (27f * tileWidth / 250f / 2f - .5f) / .57735f);
    }

    private void drawRadiosity(Canvas canvas) {
        if (alpha == 255) return;
        int w = card.getWidth(), h = card.getHeight();
        Outline outline = new Outline();
        card.getOutlineProvider().getOutline(card, outline);
        float corner = Math.max(0f, outline.getRadius());
        float unit = w / 250f;
        float pad = 54f * unit;
        if (radiosity == null || restWidth != w || restHeight != h || restCorner != corner) {
            if (radiosity != null) radiosity.recycle();
            restWidth = w; restHeight = h; restCorner = corner;
            radiosity = Bitmap.createBitmap(Math.max(1, (int) Math.ceil((w + 2 * pad) / 2)),
                    Math.max(1, (int) Math.ceil((h + 2 * pad) / 2)), Bitmap.Config.ARGB_8888);
            Canvas bake = new Canvas(radiosity);
            bake.scale(.5f, .5f);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(0x803A3A3C);
            paint.setMaskFilter(new BlurMaskFilter(radiosityBlurRadius(w),
                    BlurMaskFilter.Blur.NORMAL));
            bake.drawRoundRect(pad, pad + 11f * unit, pad + w, pad + h + 11f * unit,
                    corner, corner, paint);
        }
        float cx = card.getLeft() + card.getTranslationX() + w / 2f;
        float cy = card.getTop() + card.getTranslationY() + h / 2f;
        canvas.save();
        canvas.scale(card.getScaleX(), card.getScaleY(), cx, cy);
        restBounds.set(cx - w / 2f - pad, cy - h / 2f - pad,
                cx + w / 2f + pad, cy + h / 2f + pad);
        restPaint.setAlpha(255 - alpha);
        canvas.drawBitmap(radiosity, null, restBounds, restPaint);
        canvas.restore();
    }

    @Override public void setAlpha(int value) { alpha = value; invalidateSelf(); }
    @Override public int getAlpha() { return alpha; }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
