package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import android.view.PixelCopy;
import android.view.Window;
import android.os.Handler;
import android.os.Looper;
import java.util.function.Consumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A single small Home snapshot, blurred once when a menu opens; never captures another app. */
final class PopupMaterial extends Drawable {
    static final int SIDEBAR_TINT = 0x8C000000;
    static final int SIDEBAR_RADIUS_DP = 29, SIDEBAR_PADDING_DP = 8;

    /** Match the sidebar container, keeping action-row geometry and inactive styling intact. */
    static void applyActions(Window window, View root, View panel, String style) {
        LinearLayout rows = panel.findViewById(R.id.rows);
        boolean classic = Config.POPUP_CLASSIC.equals(style);
        for (int i = 0; i < rows.getChildCount(); i++) {
            View row = rows.getChildAt(i);
            ((TextView) row.findViewById(R.id.title)).setTypeface(UiFont.get(panel.getContext(), R.string.font_medium));
            if (!classic) row.setBackgroundResource(R.drawable.action_row_bg);
        }
        if (classic) return;
        float density = panel.getResources().getDisplayMetrics().density;
        int padding = Math.round(SIDEBAR_PADDING_DP * density);
        float corner = SIDEBAR_RADIUS_DP * density;
        panel.setPadding(padding, padding, padding, padding);
        panel.setElevation(0f);
        GradientDrawable fallback = new GradientDrawable();
        fallback.setColor(SIDEBAR_TINT); fallback.setCornerRadius(corner);
        fallback.setStroke(Math.max(1, Math.round(.5f * density)), 0x59000000);
        panel.setBackground(fallback);
        Runnable prepare = () -> apply(window, root, panel, style, corner, material -> {
            panel.setBackground(material);
            PopupMaterial glass = (PopupMaterial) material;
            glass.setMaterialTint(SIDEBAR_TINT);
            glass.setSidebarGlassProgress(1f);
            glass.setSidebarGlass(panel.getWidth(), panel.getHeight(), corner, () -> { });
        }, () -> { });
        if (panel.isAttachedToWindow()) panel.post(prepare);
        else panel.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View view) {
                view.removeOnAttachStateChangeListener(this);
                view.post(prepare);
            }
            @Override public void onViewDetachedFromWindow(View view) { }
        });
    }

    static final ExecutorService RENDERER = Executors.newSingleThreadExecutor(task ->
            new Thread(task, "TVHomeGlass"));
    private final Bitmap backdrop;
    private final View panel;
    private final int rootX, rootY, rootWidth, rootHeight;
    private float radius;
    private int materialTint = 0x69000000;
    private Integer focusedTint;
    private Bitmap sidebarLight, sidebarShade, sidebarShadow;
    private float sidebarPhase;
    private int shadowPad;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF area = new RectF(), destination = new RectF();
    private final Path clip = new Path();
    private final int[] location = new int[2];

    static void apply(Window window, View root, View panel, String style) {
        apply(window, root, panel, style, root.getResources().getDimension(R.dimen.popup_corner_radius));
    }

    static void apply(Window window, View root, View panel, String style, float radius) {
        apply(window, root, panel, style, radius, panel::setBackground);
    }

    static void apply(Window window, View root, View panel, String style, float radius,
                      Consumer<Drawable> background) {
        apply(window, root, panel, style, radius, background, () -> { });
    }

    static void apply(Window window, View root, View panel, String style, float radius,
                      Consumer<Drawable> background, Runnable fallback) {
        if (Config.POPUP_CLASSIC.equals(style)) {
            fallback.run();
            return;
        }
        if (!panel.isAttachedToWindow()) {
            panel.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override public void onViewAttachedToWindow(View view) {
                    view.removeOnAttachStateChangeListener(this);
                    apply(window, root, panel, style, radius, background, fallback);
                }
                @Override public void onViewDetachedFromWindow(View view) { }
            });
            return;
        }
        if (root.getWidth() == 0 || root.getHeight() == 0) {
            fallback.run();
            return;
        }
        Bitmap image = Bitmap.createBitmap(Math.max(1, root.getWidth() / 6),
                Math.max(1, root.getHeight() / 6), Bitmap.Config.ARGB_8888);
        int[] at = new int[2];
        root.getLocationInWindow(at);
        Rect source = new Rect(at[0], at[1], at[0] + root.getWidth(), at[1] + root.getHeight());
        try {
            PixelCopy.request(window, source, image, result -> {
                if (result != PixelCopy.SUCCESS || !panel.isAttachedToWindow()) {
                    image.recycle();
                    if (panel.isAttachedToWindow()) fallback.run();
                    return;
                }
                RENDERER.execute(() -> {
                    try {
                        blur(image);
                    } catch (RuntimeException error) {
                        image.recycle();
                        android.util.Log.w("TVHome", "Using opaque popup: Home blur failed", error);
                        panel.post(() -> {
                            if (panel.isAttachedToWindow()) fallback.run();
                        });
                        return;
                    }
                    panel.post(() -> {
                        if (!panel.isAttachedToWindow()) {
                            image.recycle();
                            return;
                        }
                        int left = panel.getPaddingLeft(), top = panel.getPaddingTop();
                        int right = panel.getPaddingRight(), bottom = panel.getPaddingBottom();
                        background.accept(new PopupMaterial(image, root, panel, radius));
                        panel.setPadding(left, top, right, bottom);
                    });
                });
            }, new Handler(Looper.getMainLooper()));
        } catch (RuntimeException error) {
            image.recycle();
            android.util.Log.w("TVHome", "Using opaque popup: Home backdrop could not be captured", error);
            fallback.run();
            return;
        }
    }

    static void blur(Bitmap image) {
        blur(image, 4);
    }

    static void blur(Bitmap image, int radius) {
        if (radius <= 0) return;
        int w = image.getWidth(), h = image.getHeight();
        int[] pixels = new int[w * h];
        image.getPixels(pixels, 0, w, 0, 0, w, h);
        for (int pass = 0; pass < 3; pass++) {
            pixels = HeroArt.boxBlur(pixels, w, h, radius, true);
            pixels = HeroArt.boxBlur(pixels, w, h, radius, false);
        }
        image.setPixels(pixels, 0, w, 0, 0, w, h);
    }

    private PopupMaterial(Bitmap image, View root, View panel, float radius) {
        backdrop = image;
        this.panel = panel;
        root.getLocationOnScreen(location);
        rootX = location[0]; rootY = location[1];
        rootWidth = root.getWidth(); rootHeight = root.getHeight();
        this.radius = radius;
    }

    void setMaterialTint(int tint) {
        if (materialTint == tint) return;
        materialTint = tint;
        invalidateSelf();
    }

    void setFocusedTint(int tint) {
        focusedTint = tint;
        onStateChange(getState());
    }

    @Override public boolean isStateful() { return focusedTint != null; }

    @Override protected boolean onStateChange(int[] states) {
        if (focusedTint == null) return false;
        int tint = tintForState(states, focusedTint);
        if (materialTint == tint) return false;
        setMaterialTint(tint);
        return true;
    }

    static int tintForState(int[] states, int focusedTint) {
        for (int state : states) {
            if (state == android.R.attr.state_focused || state == android.R.attr.state_activated)
                return focusedTint;
        }
        return 0x69000000;
    }

    /** Cache only the sidebar's extra edge lighting; other popup materials remain unchanged. */
    void setSidebarGlass(int width, int height, float corner, Runnable ready) {
        if (width <= 0 || height <= 0) return;
        float scale = panel.getResources().getDisplayMetrics().density / 2f;
        RENDERER.execute(() -> {
            int[] light = new int[width * height], shade = new int[light.length];
            SidebarButtonMaterial.Lighting lightingMask = SidebarButtonMaterial.lighting(width, height, corner, scale);
            for (int i = 0; i < light.length; i++) {
                light[i] = Math.round(26 * lightingMask.highlights[i]) << 24 | 0x00FFFFFF;
                shade[i] = Math.round(25 * lightingMask.shade[i]) << 24;
            }
            Bitmap lighting = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            lighting.setPixels(light, 0, width, 0, 0, width, height);
            Bitmap shading = Bitmap.createBitmap(shade, width, height, Bitmap.Config.ARGB_8888);
            Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
            edge.setStyle(Paint.Style.STROKE); edge.setStrokeWidth(scale);
            // shortcut: cached edge lighting approximates Figma GLASS optics without a live refraction shader.
            edge.setShader(new LinearGradient(0, 0, 0, height,
                    new int[]{0x40FFFFFF, 0x00FFFFFF, 0x26FFFFFF},
                    new float[]{0, .5f, 1}, Shader.TileMode.CLAMP));
            new Canvas(lighting).drawRoundRect(new RectF(scale / 2, scale / 2,
                    width - scale / 2, height - scale / 2), corner, corner, edge);
            int padding = Math.max(1, Math.round(24 * scale));
            Bitmap shadowImage = Bitmap.createBitmap(width + 2 * padding, height + 2 * padding,
                    Bitmap.Config.ARGB_8888);
            edge.setShader(null); edge.setStyle(Paint.Style.FILL); edge.setColor(0xFF000000);
            edge.setShadowLayer(15 * scale, 0, 8 * scale, 0x0A000000);
            Canvas shadow = new Canvas(shadowImage);
            RectF face = new RectF(padding, padding, width + padding, height + padding);
            shadow.drawRoundRect(face, corner, corner, edge);
            edge.clearShadowLayer(); edge.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            shadow.drawRoundRect(face, corner, corner, edge);
            panel.post(() -> {
                if (!panel.isAttachedToWindow() || panel.getBackground() != this) {
                    lighting.recycle(); shading.recycle(); shadowImage.recycle();
                    return;
                }
                sidebarLight = lighting; sidebarShade = shading; sidebarShadow = shadowImage;
                shadowPad = padding;
                invalidateSelf();
                ready.run();
            });
        });
    }

    void setSidebarGlassProgress(float phase) {
        if (sidebarPhase == phase) return;
        sidebarPhase = phase;
        invalidateSelf();
    }

    void drawSidebarShadow(Canvas canvas) {
        if (sidebarShadow == null) return;
        // The background's first bounds update normally occurs later, inside View.draw().
        setBounds(0, 0, panel.getWidth(), panel.getHeight());
        if (area.isEmpty()) return;
        paint.setAlpha(Math.round(sidebarPhase * 255));
        canvas.drawBitmap(sidebarShadow, null, new RectF(area.left - shadowPad, area.top - shadowPad,
                area.right + shadowPad, area.bottom + shadowPad), paint);
        paint.setAlpha(255);
    }

    /** Sample the final row position from this same cached Home blur, beneath the sidebar tint. */
    Bitmap buttonBackdrop(View row, float rowTranslationY, int tint) {
        int[] at = new int[2];
        row.getLocationOnScreen(at);
        float x = at[0] - rootX, y = at[1] - rowTranslationY - rootY;
        Bitmap image = Bitmap.createBitmap(row.getWidth(), row.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawBitmap(backdrop, null,
                new RectF(-x, -y, rootWidth - x, rootHeight - y),
                new Paint(Paint.FILTER_BITMAP_FLAG));
        canvas.drawColor(tint);
        if (sidebarLight != null && panel.getParent() instanceof View) {
            ((View) panel.getParent()).getLocationOnScreen(at);
            float left = at[0] - rootX - x, top = at[1] - rootY - y;
            canvas.drawBitmap(sidebarShade, left, top, null);
            canvas.drawBitmap(sidebarLight, left, top, null);
        }
        return image;
    }

    void setCornerRadius(float radius) {
        if (this.radius == radius) return;
        this.radius = radius;
        onBoundsChange(getBounds());
        invalidateSelf();
    }

    @Override protected void onBoundsChange(Rect bounds) {
        area.set(bounds);
        clip.reset();
        clip.addRoundRect(area, radius, radius, Path.Direction.CW);
    }

    @Override public void draw(Canvas canvas) {
        panel.getLocationOnScreen(location);
        float x = location[0] - rootX, y = location[1] - rootY;
        destination.set(-x, -y, rootWidth - x, rootHeight - y);
        canvas.save();
        canvas.clipPath(clip);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFFFFFFF);
        canvas.drawBitmap(backdrop, null, destination, paint);
        // The dock's material: 41% black over the same blur, so menus and the band read as one glass.
        paint.setColor(materialTint);
        canvas.drawRect(area, paint);
        if (sidebarLight != null) {
            paint.setAlpha(Math.round(sidebarPhase * 255));
            canvas.drawBitmap(sidebarShade, null, area, paint);
            canvas.drawBitmap(sidebarLight, null, area, paint);
        }
        canvas.restore();
        paint.setStyle(Paint.Style.STROKE);
        // The dock band's inner stroke: #fff at 10%, 2px (1dp) inside the edge.
        float phase = sidebarLight == null ? 0f : sidebarPhase;
        float density = panel.getResources().getDisplayMetrics().density;
        paint.setStrokeWidth(density * (1f - .5f * phase));
        int gray = Math.round(255 * (1f - phase));
        paint.setColor(android.graphics.Color.argb(Math.round(26 + 63 * phase), gray, gray, gray));
        canvas.drawRoundRect(area.left + 1f, area.top + 1f, area.right - 1f, area.bottom - 1f,
                radius, radius, paint);
    }

    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
