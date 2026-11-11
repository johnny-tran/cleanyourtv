package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Downsampled, LRU-cached poster loading for content:// and http(s):// URIs, off the main thread. The only
 * network use in this fork is fetching these poster images. Ported from dhruv2003/sony-tv-launcher (MIT).
 */
final class PosterLoader {

    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(12 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    private PosterLoader() {
    }

    static void load(Context ctx, String uri, ImageView target, int maxWidth, Runnable onLoaded) {
        target.setTag(uri);
        Bitmap cached = CACHE.get(uri);
        if (cached != null) {
            target.setImageBitmap(cached);
            onLoaded.run();
            return;
        }
        Context app = ctx.getApplicationContext();
        EXECUTOR.execute(() -> {
            Bitmap bmp;
            try {
                bmp = decode(app, uri, maxWidth);
            } catch (Exception | OutOfMemoryError e) {
                bmp = null;
            }
            if (bmp == null) return;
            CACHE.put(uri, bmp);
            final Bitmap result = bmp;
            MAIN.post(() -> {
                if (uri.equals(target.getTag())) {
                    target.setImageBitmap(result);
                    onLoaded.run();
                }
            });
        });
    }

    private static Bitmap decode(Context ctx, String uri, int maxWidth) throws Exception {
        byte[] bytes = read(ctx, uri);
        if (bytes == null) return null;
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        int sample = 1;
        while (bounds.outWidth / (sample * 2) >= maxWidth) sample *= 2;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
    }

    private static byte[] read(Context ctx, String uri) throws Exception {
        Uri parsed = Uri.parse(uri);
        String scheme = parsed.getScheme() == null ? "" : parsed.getScheme().toLowerCase(Locale.ROOT);
        HttpURLConnection conn = null;
        InputStream in;
        if (scheme.equals("http") || scheme.equals("https")) {
            conn = (HttpURLConnection) new URL(uri).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(8000);
            if (conn.getResponseCode() / 100 != 2) {
                conn.disconnect();
                return null;
            }
            in = conn.getInputStream();
        } else {
            // content://, android.resource:// and anything else the resolver can open.
            in = ctx.getContentResolver().openInputStream(parsed);
        }
        if (in == null) return null;
        try (InputStream s = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16 * 1024];
            int n;
            while ((n = s.read(buf)) >= 0) {
                out.write(buf, 0, n);
                if (out.size() > MAX_BYTES) return null;
            }
            return out.toByteArray();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
