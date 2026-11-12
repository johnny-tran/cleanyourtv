package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.PixelCopy;
import android.view.Window;
import android.os.Handler;
import android.os.Looper;

/** A single small Home snapshot, blurred once when a menu opens; never captures another app. */
final class PopupMaterial extends Drawable {
    private final Bitmap backdrop;
    private final View panel;
    private final int rootX, rootY, rootWidth, rootHeight;
    private final float radius;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF area = new RectF(), destination = new RectF();
    private final Path clip = new Path();
    private final int[] location = new int[2];

    static void apply(Window window, View root, View panel) {
        if (!panel.isAttachedToWindow()) {
            panel.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override public void onViewAttachedToWindow(View view) {
                    view.removeOnAttachStateChangeListener(this);
                    apply(window, root, panel);
                }
                @Override public void onViewDetachedFromWindow(View view) { }
            });
            return;
        }
        if (root.getWidth() == 0 || root.getHeight() == 0) return;
        Bitmap image = Bitmap.createBitmap(Math.max(1, root.getWidth() / 6),
                Math.max(1, root.getHeight() / 6), Bitmap.Config.ARGB_8888);
        int[] at = new int[2];
        root.getLocationInWindow(at);
        Rect source = new Rect(at[0], at[1], at[0] + root.getWidth(), at[1] + root.getHeight());
        try {
            PixelCopy.request(window, source, image, result -> {
                if (result != PixelCopy.SUCCESS || !panel.isAttachedToWindow()) {
                    image.recycle();
                    return;
                }
                blur(image);
                int left = panel.getPaddingLeft(), top = panel.getPaddingTop();
                int right = panel.getPaddingRight(), bottom = panel.getPaddingBottom();
                panel.setBackground(new PopupMaterial(image, root, panel));
                panel.setPadding(left, top, right, bottom);
            }, new Handler(Looper.getMainLooper()));
        } catch (RuntimeException error) {
            image.recycle();
            android.util.Log.w("TVHome", "Using opaque popup: Home backdrop could not be captured", error);
            return;
        }
    }

    static void blur(Bitmap image) {
        int w = image.getWidth(), h = image.getHeight();
        int[] pixels = new int[w * h];
        image.getPixels(pixels, 0, w, 0, 0, w, h);
        for (int pass = 0; pass < 3; pass++) {
            pixels = HeroArt.boxBlur(pixels, w, h, 4, true);
            pixels = HeroArt.boxBlur(pixels, w, h, 4, false);
        }
        image.setPixels(pixels, 0, w, 0, 0, w, h);
    }

    private PopupMaterial(Bitmap image, View root, View panel) {
        backdrop = image;
        this.panel = panel;
        root.getLocationOnScreen(location);
        rootX = location[0]; rootY = location[1];
        rootWidth = root.getWidth(); rootHeight = root.getHeight();
        radius = 20 * root.getResources().getDisplayMetrics().density;
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
        paint.setColor(0xC0121418);
        canvas.drawRect(area, paint);
        canvas.restore();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(panel.getResources().getDisplayMetrics().density * 0.5f);
        paint.setColor(0x26FFFFFF);
        canvas.drawRoundRect(area.left + 0.5f, area.top + 0.5f, area.right - 0.5f, area.bottom - 0.5f,
                radius, radius, paint);
    }

    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
