package com.gathin.tvhome;

import android.app.Activity;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.animation.DecelerateInterpolator;
import android.graphics.Color;
import android.graphics.Outline;
import android.view.SoundEffectConstants;
import android.view.ViewOutlineProvider;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextClock;
import android.widget.TextView;

import java.io.File;
import java.util.function.Consumer;

/** Opt-in Figma header. Private prototype fonts/art live beside config.json, outside the APK. */
final class HomeHeader {
    private static final int SIDEBAR_TINT = 0x8C000000; // 55% black; closed pill keeps its own material.
    private final Activity activity;
    private final FrameLayout root;
    private final Config config;
    private final Consumer<PopupWindow> popupChanged;
    private final Runnable inputs, library, settings, launcherMenu;
    private final Typeface medium;
    private final File assets;
    private final float density;
    final LinearLayout pill;
    private final TextView greeting;
    private final TextClock pillClock;
    private final ImageView pillAvatar;
    private boolean sidebarOpen;
    private boolean validClock = true;

    HomeHeader(Activity activity, FrameLayout root, Config config, Consumer<PopupWindow> popupChanged,
               Runnable inputs, Runnable library, Runnable settings, Runnable launcherMenu) {
        this.activity = activity;
        this.root = root;
        this.config = config;
        this.popupChanged = popupChanged;
        this.inputs = inputs;
        this.library = library;
        this.settings = settings;
        this.launcherMenu = launcherMenu;
        density = activity.getResources().getDisplayMetrics().density;
        assets = new File(activity.getExternalFilesDir(null), "header-assets");
        medium = UiFont.get(activity, R.string.font_medium);
        greeting = text("", 15, medium);
        greeting.setTextColor(0xFFB6B6B6);
        FrameLayout.LayoutParams gp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        gp.leftMargin = dp(25); gp.topMargin = dp(31.5f);
        root.addView(greeting, gp);

        pill = new LinearLayout(activity);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setGravity(Gravity.CENTER_VERTICAL);
        pill.setPadding(dp(11.5f), dp(.5f), dp(.5f), dp(.5f));
        pill.setBackground(pillBackground(panel(0xB0121418, 40)));
        pill.setFocusable(true); pill.setClickable(true);
        pill.setContentDescription(activity.getString(R.string.settings));
        pillClock = clock();
        pill.addView(pillClock);
        LinearLayout.LayoutParams av = new LinearLayout.LayoutParams(dp(32), dp(32));
        av.leftMargin = dp(7.5f);
        pillAvatar = avatar(dp(3.5f));
        pill.addView(pillAvatar, av);
        FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(-2, dp(33), Gravity.TOP | Gravity.END);
        pp.rightMargin = dp(25); pp.topMargin = dp(24);
        root.addView(pill, pp);
        pill.setOnClickListener(v -> show());
        pill.setOnLongClickListener(v -> { launcherMenu.run(); return true; });
        pill.setOnFocusChangeListener((v, focused) -> {
            pillClock.setTextColor(focused || sidebarOpen ? Color.BLACK : Color.WHITE);
        });
        // Capture once after the first frame; the sidebar takes its own fresh Home snapshot on opening.
        pill.post(() -> PopupMaterial.apply(activity.getWindow(), root, pill, config.popupStyle, dp(40),
                material -> pill.setBackground(pillBackground(material))));
        PopupMaterial.RENDERER.execute(() -> {
            SidebarButtonMaterial.lighting(dp(220), dp(32) + 2 * dp(11) + 3 * dp(42)
                    + 2 * dp(6) + dp(8), dp(29), density / 2f);
            SidebarButtonMaterial.lighting(dp(220) - 2 * dp(8), dp(42), dp(42) / 2f, density / 2f);
        });
    }

    void updateGreeting(CharSequence value, boolean valid) {
        validClock = valid;
        greeting.setText(value);
        greeting.setVisibility(valid && config.showGreeting ? View.VISIBLE : View.INVISIBLE);
        pill.setVisibility(valid ? View.VISIBLE : View.INVISIBLE);
        if (!sidebarOpen) pill.setAlpha(1f);
    }

    private void show() {
        sidebarOpen = true;
        pill.setActivated(true);
        FrameLayout wrapper = new FrameLayout(activity);
        wrapper.setClipChildren(false);
        MorphPanel panel = new MorphPanel();
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        panel.addView(content, new FrameLayout.LayoutParams(-1, -2));
        panel.setClickable(true);
        FrameLayout.LayoutParams position = new FrameLayout.LayoutParams(dp(220), -2, Gravity.TOP | Gravity.END);
        position.topMargin = dp(13.5f); position.rightMargin = dp(13.5f);
        wrapper.addView(panel, position);

        LinearLayout profile = new LinearLayout(activity);
        profile.setGravity(Gravity.CENTER_VERTICAL);
        profile.setPadding(dp(12), dp(11), dp(12), dp(11));
        TextClock sidebarClock = clock();
        profile.addView(sidebarClock, new LinearLayout.LayoutParams(0, -2, 1));
        TextView name = text(config.greetingName, 12.5f, medium);
        name.setMaxWidth(dp(90)); name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        profile.addView(name);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        avatarParams.leftMargin = dp(8);
        ImageView sidebarAvatar = avatar(dp(3.5f));
        profile.addView(sidebarAvatar, avatarParams);
        content.addView(profile);
        LinearLayout rows = new LinearLayout(activity);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setClipChildren(false);
        rows.setClipToPadding(false);
        rows.setPadding(dp(8), 0, dp(8), dp(8));
        content.addView(rows);
        SidebarPopup popup = new SidebarPopup(wrapper, panel);
        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        popup.setOnDismissListener(() -> {
            panel.surface.setBackground(null); // Release the captured Home image as soon as the menu closes.
            sidebarOpen = false;
            pill.setAlpha(1f);
            pill.setVisibility(validClock ? View.VISIBLE : View.INVISIBLE);
            popupChanged.accept(null); pill.requestFocus();
            pill.setActivated(false);
            if (popup.afterClose != null) popup.afterClose.run();
        });
        wrapper.setOnClickListener(v -> popup.dismiss());
        Runnable[] actions = {inputs, library, settings};
        int[] titles = {R.string.inputs, R.string.sidebar_app_library, R.string.system_settings};
        String[] icons = {"tv", "square.grid.2x2", "gearshape"};
        String[] selected = {"inset.filled.tv", "square.grid.2x2.fill", "gearshape.fill"};
        int[] fallback = {R.drawable.ic_input, R.drawable.ic_apps_pill, R.drawable.ic_settings};
        TextView[] buttons = new TextView[3];
        for (int i = 0; i < buttons.length; i++) {
            final int index = i;
            TextView row = text(activity.getString(titles[i]), 15, medium);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10.5f), 0, dp(10.5f), 0);
            row.setCompoundDrawablePadding(dp(8));
            row.setFocusable(true); row.setClickable(true);
            row.setId(View.generateViewId());
            row.setBackground(new SidebarButtonMaterial(density, row));
            Runnable paint = () -> {
                boolean focus = row.isFocused();
                row.setTextColor(focus ? Color.BLACK : Color.WHITE);
                Drawable icon = icon(focus ? selected[index] : icons[index], fallback[index]);
                icon.setTint(focus ? Color.BLACK : Color.WHITE);
                icon.setBounds(0, 0, dp(24), dp(24));
                row.setCompoundDrawables(icon, null, null, null);
            };
            row.setOnFocusChangeListener((v, focused) -> paint.run());
            paint.run();
            row.setOnClickListener(v -> popup.close(actions[index]));
            if (i == 2) row.setOnLongClickListener(v -> { popup.close(launcherMenu); return true; });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(42));
            if (i > 0) lp.topMargin = dp(6);
            rows.addView(row, lp);
            buttons[i] = row;
        }
        for (int i = 0; i < buttons.length; i++) {
            TextView row = buttons[i];
            row.setNextFocusUpId(buttons[Math.max(0, i - 1)].getId());
            row.setNextFocusDownId(buttons[Math.min(2, i + 1)].getId());
            row.setNextFocusLeftId(row.getId()); row.setNextFocusRightId(row.getId());
            row.setOnKeyListener((v, key, event) -> {
                if (popup.closing) return true;
                if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT) return true;
                if (key != KeyEvent.KEYCODE_DPAD_UP && key != KeyEvent.KEYCODE_DPAD_DOWN) return false;
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    int current = rows.indexOfChild(v);
                    int next = nextRow(current, key == KeyEvent.KEYCODE_DPAD_UP);
                    if (next != current && buttons[next].requestFocus()) CursorSounds.play(v,
                            key == KeyEvent.KEYCODE_DPAD_UP ? SoundEffectConstants.NAVIGATION_UP
                                    : SoundEffectConstants.NAVIGATION_DOWN);
                }
                return true;
            });
        }
        popupChanged.accept(popup);
        popup.showAtLocation(root, Gravity.TOP | Gravity.START, 0, 0);
        panel.setAlpha(0f);
        panel.post(() -> {
            if (!popup.isShowing() || popup.closing) return;
            panel.initialize(sidebarClock, name, sidebarAvatar, rows);
            Consumer<PopupMaterial> prepareButtons = glass -> {
                int[] remaining = {buttons.length};
                for (TextView button : buttons) {
                    ((SidebarButtonMaterial) button.getBackground()).setBackdrop(
                            glass == null ? null : glass.buttonBackdrop(button, rows.getTranslationY(), SIDEBAR_TINT),
                            () -> {
                                if (--remaining[0] == 0 && !popup.closing) {
                                    panel.revealRows();
                                    if (motionEnabled()) popup.animateTo(1f, null);
                                    else {
                                        panel.materialPhase = 1f;
                                        panel.progress(1f);
                                    }
                                }
                            });
                }
            };
            panel.setAlpha(1f);
            // Hand off in one frame: keep the focused pill until its replacement has matching bounds.
            pill.setAlpha(0f);
            // Let Home draw without the pill before capturing the sidebar's backdrop.
            panel.postOnAnimation(() -> panel.postOnAnimation(() -> {
                if (popup.isShowing()) PopupMaterial.apply(activity.getWindow(), root,
                        panel.surface, config.popupStyle, dp(29), material -> {
                            panel.surface.setBackground(material);
                            panel.progress(panel.fraction);
                            if (material instanceof PopupMaterial) {
                                PopupMaterial glass = (PopupMaterial) material;
                                glass.setSidebarGlass(panel.getWidth(), panel.getHeight(), dp(29),
                                        () -> prepareButtons.accept(glass));
                            }
                        }, () -> prepareButtons.accept(null));
            }));
        });
        buttons[0].requestFocus();
    }

    private boolean motionEnabled() { return config.animations && !config.reducedMotion; }

    private final class SidebarPopup extends PopupWindow {
        private final MorphPanel panel;
        private ValueAnimator animator;
        private boolean closing;
        private Runnable afterClose;

        SidebarPopup(View wrapper, MorphPanel panel) {
            super(CursorSounds.wrap(wrapper), -1, -1, true);
            this.panel = panel;
            setAnimationStyle(0);
        }

        void close(Runnable action) {
            if (closing) return;
            afterClose = action;
            dismiss();
        }

        @Override public void dismiss() {
            if (closing) return;
            closing = true;
            if (!motionEnabled() || !isShowing() || panel.getWidth() == 0) {
                if (animator != null) animator.cancel();
                finishClose();
            } else animateTo(0f, this::finishClose);
        }

        private void finishClose() {
            if (!isShowing() || activity.isFinishing() || activity.isDestroyed()
                    || root.getWindowVisibility() != View.VISIBLE) {
                super.dismiss();
                return;
            }
            // Redraw the underlying pill before removing the popup's matching final frame.
            pill.setAlpha(1f);
            pill.setVisibility(validClock ? View.VISIBLE : View.INVISIBLE);
            root.postOnAnimation(() -> root.postOnAnimation(() -> super.dismiss()));
        }

        void animateTo(float target, Runnable finished) {
            if (animator != null) animator.cancel();
            animator = ValueAnimator.ofFloat(panel.fraction, target);
            animator.setDuration(Math.max(1, Math.round(420 * Math.abs(target - panel.fraction))));
            animator.setInterpolator(new DecelerateInterpolator(1.5f));
            float startMaterial = panel.materialPhase;
            animator.addUpdateListener(value -> {
                panel.materialPhase = materialProgress(startMaterial, target,
                        value.getCurrentPlayTime(), value.getDuration());
                panel.progress((float) value.getAnimatedValue());
            });
            if (finished != null) animator.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) { finished.run(); }
            });
            animator.start();
        }
    }

    /** Move the surface bounds and clip its contents; text is translated, never scaled. */
    private final class MorphPanel extends FrameLayout {
        final View surface;
        private final RectF closed = new RectF(), bounds = new RectF();
        private final Path clip = new Path();
        private final ArgbEvaluator colors = new ArgbEvaluator();
        private TextClock clock;
        private TextView name;
        private ImageView avatar;
        private View rows;
        private float clockDx, clockDy, avatarDx, avatarDy, avatarScale, nameDx;
        private boolean ready;
        private boolean rowMaterialsReady;
        float fraction, materialPhase;

        MorphPanel() {
            super(activity);
            surface = new View(activity);
            surface.setBackground(panel(SIDEBAR_TINT, 29));
            addView(surface, new FrameLayout.LayoutParams(0, 0));
        }

        void initialize(TextClock clock, TextView name, ImageView avatar, View rows) {
            this.clock = clock; this.name = name; this.avatar = avatar; this.rows = rows;
            int[] origin = new int[2], start = new int[2], end = new int[2];
            getLocationOnScreen(origin); pill.getLocationOnScreen(start);
            closed.set(start[0] - origin[0], start[1] - origin[1],
                    start[0] - origin[0] + pill.getWidth(), start[1] - origin[1] + pill.getHeight());
            pillClock.getLocationOnScreen(start); clock.getLocationOnScreen(end);
            clockDx = start[0] - end[0]; clockDy = start[1] - end[1];
            pillAvatar.getLocationOnScreen(start); avatar.getLocationOnScreen(end);
            float centerX = start[0] + pillAvatar.getWidth() / 2f;
            float centerY = start[1] + pillAvatar.getHeight() / 2f;
            avatarDx = centerX - end[0] - avatar.getWidth() / 2f;
            avatarDy = centerY - end[1] - avatar.getHeight() / 2f;
            avatarScale = (pillAvatar.getWidth() - 2f * pillAvatar.getPaddingLeft())
                    / (avatar.getWidth() - 2f * avatar.getPaddingLeft());
            name.getLocationOnScreen(end);
            nameDx = centerX - end[0];
            ready = true;
            materialPhase = 0f;
            progress(0f);
        }

        @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            if (ready) progress(fraction);
        }

        void revealRows() {
            rowMaterialsReady = true;
            rows.setAlpha(rowAlpha(fraction, rowMaterialsReady));
        }

        void progress(float value) {
            fraction = value;
            if (!ready) return;
            bounds.set(mix(closed.left, 0, value), mix(closed.top, 0, value),
                    mix(closed.right, getWidth(), value), mix(closed.bottom, getHeight(), value));
            float radius = mix(closed.height() / 2f, dp(29), value);
            surface.layout(Math.round(bounds.left), Math.round(bounds.top),
                    Math.round(bounds.right), Math.round(bounds.bottom));
            Drawable material = surface.getBackground();
            int tint = (int) colors.evaluate(materialPhase, 0x99FFFFFF, SIDEBAR_TINT);
            if (material instanceof PopupMaterial) {
                ((PopupMaterial) material).setCornerRadius(radius);
                ((PopupMaterial) material).setMaterialTint(tint);
                ((PopupMaterial) material).setSidebarGlassProgress(materialPhase);
            } else if (material instanceof GradientDrawable) {
                ((GradientDrawable) material).setCornerRadius(radius);
                ((GradientDrawable) material).setColor(tint);
            }
            clip.reset(); clip.addRoundRect(bounds, radius, radius, Path.Direction.CW);
            clock.setTranslationX(clockDx * (1f - value)); clock.setTranslationY(clockDy * (1f - value));
            clock.setTextColor((int) colors.evaluate(materialPhase, Color.BLACK, Color.WHITE));
            avatar.setTranslationX(avatarDx * (1f - value)); avatar.setTranslationY(avatarDy * (1f - value));
            avatar.setScaleX(mix(avatarScale, 1, value)); avatar.setScaleY(mix(avatarScale, 1, value));
            name.setTranslationX(nameDx * (1f - value)); name.setAlpha(value);
            rows.setAlpha(rowAlpha(value, rowMaterialsReady));
            rows.setTranslationY(-dp(8) * (1f - value));
            invalidate();
        }

        @Override protected void dispatchDraw(Canvas canvas) {
            if (!ready) return;
            if (surface.getBackground() instanceof PopupMaterial) {
                int shadow = canvas.save();
                canvas.translate(surface.getLeft(), surface.getTop());
                ((PopupMaterial) surface.getBackground()).drawSidebarShadow(canvas);
                canvas.restoreToCount(shadow);
            }
            int saved = canvas.save(); canvas.clipPath(clip);
            super.dispatchDraw(canvas);
            canvas.restoreToCount(saved);
        }
    }

    /** Never expose placeholder fills while the final glass images are being prepared. */
    static float rowAlpha(float expansion, boolean materialsReady) {
        return materialsReady && expansion > 0f ? 1f : 0f;
    }

    /** Mirror the opening's first 90ms fade in the closing's final 90ms, independent of geometry easing. */
    static float materialProgress(float start, float target, long elapsed, long duration) {
        float fadeDuration = Math.min(90f * Math.abs(target - start), duration);
        if (fadeDuration == 0f) return target;
        float delay = target < start ? duration - fadeDuration : 0f;
        float phase = Math.max(0f, Math.min(1f, (elapsed - delay) / fadeDuration));
        return mix(start, target, phase * phase * (3f - 2f * phase));
    }

    static float mix(float start, float end, float progress) {
        return start + (end - start) * Math.max(0f, Math.min(1f, progress));
    }

    static int nextRow(int current, boolean up) {
        return Math.max(0, Math.min(2, current + (up ? -1 : 1)));
    }

    private TextClock clock() {
        TextClock clock = new TextClock(activity);
        clock.setFormat12Hour("h:mm a"); clock.setFormat24Hour("HH:mm");
        ClockFormat.apply(clock, config.clockFormat);
        clock.setTextSize(12.5f); clock.setTextColor(Color.WHITE); clock.setTypeface(medium);
        clock.setIncludeFontPadding(false);
        return clock;
    }

    private ImageView avatar(int inset) {
        ImageView image = new ImageView(activity);
        image.setImageDrawable(icon("avatar", R.drawable.ic_settings));
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        // Figma's 50px photo is centered in a 64px layout box with 7px insets.
        image.setPadding(inset, inset, inset, inset);
        image.setCropToPadding(true);
        image.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setOval(view.getPaddingLeft(), view.getPaddingTop(),
                        view.getWidth() - view.getPaddingRight(), view.getHeight() - view.getPaddingBottom());
            }
        });
        image.setClipToOutline(true);
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return image;
    }

    private Drawable icon(String name, int fallback) {
        Drawable image = Drawable.createFromPath(new File(assets, name + ".png").getPath());
        return image != null ? image.mutate() : activity.getDrawable(fallback).mutate();
    }

    private TextView text(CharSequence value, float size, Typeface font) {
        TextView view = new TextView(activity);
        view.setText(value); view.setTextSize(size); view.setTypeface(font);
        view.setTextColor(Color.WHITE); view.setIncludeFontPadding(false); view.setSingleLine(true);
        return view;
    }

    private Drawable pillBackground(Drawable material) {
        if (material instanceof PopupMaterial) {
            ((PopupMaterial) material).setFocusedTint(0x99FFFFFF);
            return material;
        }
        StateListDrawable background = new StateListDrawable();
        background.addState(new int[]{android.R.attr.state_activated}, panel(0x99FFFFFF, 40));
        background.addState(new int[]{android.R.attr.state_focused}, panel(0x99FFFFFF, 40));
        background.addState(new int[0], material);
        return background;
    }

    private GradientDrawable panel(int color, float radius) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color); background.setCornerRadius(dp(radius));
        background.setStroke(dp(0.5f), 0x1AFFFFFF);
        return background;
    }

    private int dp(float value) { return Math.round(value * density); }
}
