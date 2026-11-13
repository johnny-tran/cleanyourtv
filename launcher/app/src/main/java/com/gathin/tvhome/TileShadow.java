package com.gathin.tvhome;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.Canvas;
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

    private TileShadow(View card) {
        this.card = card;
        parent = (View) card.getParent();
        original = parent.getBackground();
        renderer = new GlassView(card.getContext(), null);
        renderer.setStyle(Config.GLASS_LIFT);
        renderer.setLayerType(View.LAYER_TYPE_NONE, null);
        renderer.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
    }

    static void animate(View card, boolean focused, boolean animations, boolean reducedMotion, float scale) {
        if (!(card.getParent() instanceof View)) return;
        TileShadow shadow = (TileShadow) card.getTag(R.id.tile_shadow);
        if (shadow == null) {
            if (!focused) return;
            shadow = new TileShadow(card);
            card.setTag(R.id.tile_shadow, shadow);
            background(shadow.parent, shadow);
        }
        TileShadow current = shadow;
        if (shadow.animator != null) shadow.animator.cancel();
        shadow.focusScale = scale;
        shadow.animator = ObjectAnimator.ofInt(shadow, "alpha", shadow.alpha, focused ? 255 : 0);
        shadow.animator.setDuration(!animations ? 0 : reducedMotion ? Motion.REDUCED_MS : focused ? 260 : 180);
        shadow.animator.setInterpolator(new DecelerateInterpolator(1.5f));
        shadow.animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                if (current.alpha == 0 && !card.isFocused() && card.getTag(R.id.tile_shadow) == current) {
                    card.setTag(R.id.tile_shadow, null);
                    if (current.parent.getBackground() == current) background(current.parent, current.original);
                }
            }
        });
        shadow.animator.start();
    }

    private static void background(View view, Drawable drawable) {
        int l = view.getPaddingLeft(), t = view.getPaddingTop(), r = view.getPaddingRight(), b = view.getPaddingBottom();
        view.setBackground(drawable);
        view.setPadding(l, t, r, b);
    }

    @Override public void draw(Canvas canvas) {
        if (original != null) { original.setBounds(getBounds()); original.draw(canvas); }
        if (alpha == 0 || card.getWidth() == 0) return;
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

    @Override public void setAlpha(int value) { alpha = value; invalidateSelf(); }
    @Override public int getAlpha() { return alpha; }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
