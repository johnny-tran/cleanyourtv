package com.gathin.tvhome;

import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

/** Shared tile focus motion and colour rules for dock apps, grid apps and Continue watching. */
final class TileFocus {
    private TileFocus() { }

    static float scale(boolean animations, boolean advancedEffects) {
        return !animations ? 1f : advancedEffects ? 1.12f : AppAdapter.FOCUS_SCALE;
    }

    static boolean coloured(boolean appGlow, boolean cinematic, boolean whiteDock, boolean inDock, boolean festive) {
        return (appGlow || cinematic && !whiteDock && inDock) && !festive;
    }

    static void animate(View view, ImageView banner, TextView name, boolean focused, boolean animations,
                        boolean advancedEffects, boolean reducedMotion, float restNameAlpha,
                        boolean overshoot, boolean inDock) {
        float density = view.getResources().getDisplayMetrics().density;
        view.animate().cancel();
        banner.animate().cancel();
        name.animate().cancel();
        float scale = scale(animations, advancedEffects);
        float s = focused ? scale : 1f;
        if (!animations) {
            view.setScaleX(1f);
            view.setScaleY(1f);
            view.setTranslationY(0f);
            view.setTranslationZ(0f);
            banner.setScaleX(1f);
            banner.setScaleY(1f);
            name.setAlpha(focused ? 1f : restNameAlpha);
            name.setTranslationY(0f);
        } else if (reducedMotion) {
            // A short zoom with no hardware layer: building a layer for so short an animation costs more
            // frames than it saves. The name just appears instead of fading.
            view.animate().scaleX(s).scaleY(s)
                    .translationY(focused && advancedEffects && !inDock ? -4f * density : 0f)
                    .translationZ(focused && advancedEffects ? 8f * density : 0f)
                    .setDuration(focused ? Motion.REDUCED_MS : 60)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
            name.setAlpha(focused ? 1f : restNameAlpha);
            // Same place as the full motion's slide ends: the layout's -6dp is only its starting point, and
            // left there the name overlaps the zoomed tile.
            // Only on focus: snapping it back to -6dp while it fades out would jump it up over the tile.
            if (focused) name.setTranslationY(density * 4f);
        } else {
            view.animate().scaleX(s).scaleY(s)
                    .translationY(focused && advancedEffects && !inDock ? -4f * density : 0f)
                    .translationZ(focused && advancedEffects ? 8f * density : 0f)
                    .setDuration(advancedEffects ? (focused ? 220 : 170) : (focused ? 260 : 180))
                    .setInterpolator(focused && !overshoot ? new DecelerateInterpolator(1.5f)
                            : new OvershootInterpolator(focused ? (advancedEffects ? 0.7f : 1.6f) : 0f))
                    .withLayer().start();
            if (advancedEffects) {
                banner.animate().scaleX(focused ? 1.035f : 1f).scaleY(focused ? 1.035f : 1f)
                        .setDuration(280).start();
            }
            name.animate().alpha(focused ? 1f : restNameAlpha)
                    .translationY(focused ? density * 4f : -density * 6f)
                    .setDuration(220).start();
        }
    }
}
