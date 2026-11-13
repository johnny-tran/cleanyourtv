package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/** Small cached copy of background layers only; app tiles and header never enter the dock blur. */
final class DockBackdrop {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path clip = new Path();
    private final int[] rootAt = new int[2], gridAt = new int[2];
    private Bitmap image;
    private long signature = Long.MIN_VALUE;

    private boolean background(View v) {
        int id = v.getId();
        return id == R.id.sky || id == R.id.heroArt || id == R.id.ambient || id == R.id.heroScrim
                || id == View.NO_ID && v instanceof ImageView;
    }

    void draw(Canvas canvas, ViewGroup grid, RectF band, float radius) {
        ViewGroup root = grid.getRootView().findViewById(R.id.root);
        long key = System.currentTimeMillis() / 60000;
        key = key * 31 + root.getWidth(); key = key * 31 + root.getHeight();
        for (int i = 0; i < root.getChildCount(); i++) {
            View v = root.getChildAt(i);
            if (!background(v)) continue;
            // The sharp wallpaper is hidden behind its blurred copy; its cached dock material stays the same.
            key = key * 31 + (v.getId() == View.NO_ID ? View.VISIBLE : v.getVisibility());
            key = key * 31 + Math.round(v.getAlpha() * 4);
            if (v instanceof ImageView) key = key * 31 + System.identityHashCode(((ImageView) v).getDrawable());
        }
        if (key != signature) {
            signature = key;
            capture(root);
        }
        clip.reset(); clip.addRoundRect(band, radius, radius, Path.Direction.CW);
        canvas.save(); canvas.clipPath(clip);
        if (image != null) {
            root.getLocationOnScreen(rootAt); grid.getLocationOnScreen(gridAt);
            float x = rootAt[0] - gridAt[0], y = rootAt[1] - gridAt[1];
            paint.setColor(0xFFFFFFFF);
            canvas.drawBitmap(image, null, new RectF(x, y, x + root.getWidth(), y + root.getHeight()), paint);
        }
        paint.setColor(image == null ? 0xFF181818 : 0x69000000); // Figma's 41% black material
        canvas.drawRect(band, paint);
        canvas.restore();
    }

    private void capture(ViewGroup root) {
        Bitmap next = Bitmap.createBitmap(Math.max(1, root.getWidth() / 6), Math.max(1, root.getHeight() / 6), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(next);
        c.scale(next.getWidth() / (float) root.getWidth(), next.getHeight() / (float) root.getHeight());
        try {
            if (root.getBackground() != null) root.getBackground().draw(c);
            for (int i = 0; i < root.getChildCount(); i++) {
                View v = root.getChildAt(i);
                if (!background(v) || (v.getVisibility() != View.VISIBLE && v.getId() != View.NO_ID)
                        || v.getAlpha() == 0) continue;
                c.saveLayerAlpha(v.getLeft(), v.getTop(), v.getRight(), v.getBottom(), Math.round(v.getAlpha() * 255));
                c.translate(v.getLeft(), v.getTop());
                Drawable d = v instanceof ImageView ? ((ImageView) v).getDrawable() : null;
                if (d instanceof BitmapDrawable) {
                    Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
                    Bitmap copy = bitmap.copy(Bitmap.Config.ARGB_8888, false);
                    BitmapDrawable safe = new BitmapDrawable(root.getResources(), copy);
                    safe.setBounds(d.getBounds());
                    c.translate(v.getPaddingLeft(), v.getPaddingTop());
                    c.concat(((ImageView) v).getImageMatrix());
                    safe.draw(c); copy.recycle();
                } else v.draw(c);
                c.restore();
            }
            PopupMaterial.blur(next);
            image = next;
        } catch (RuntimeException error) {
            next.recycle();
            android.util.Log.w("TVHome", "Using dark dock fallback", error);
        }
    }
}
