package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.util.Log;
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

    private static final class Request {
        final String uri;
        Request(String uri) { this.uri = uri; }
    }

    static void load(Context ctx, String uri, ImageView target, int maxWidth, Runnable onLoaded) {
        Object previous = target.getTag();
        if (previous instanceof Request && uri.equals(((Request) previous).uri)) return;
        Request request = new Request(uri);
        target.setTag(request);
        target.setImageDrawable(null);
        Bitmap cached = CACHE.get(uri);
        if (cached != null) {
            target.setImageBitmap(cached);
            onLoaded.run();
            return;
        }
        attempt(ctx.getApplicationContext(), request, target, maxWidth, onLoaded, 0);
    }

    private static void attempt(Context app, Request request, ImageView target, int maxWidth,
                                Runnable onLoaded, int attempt) {
        EXECUTOR.execute(() -> {
            Bitmap bmp = null;
            boolean retry = true;
            try {
                bmp = decode(app, request.uri, maxWidth);
                if (bmp == null) Log.w("PosterLoader", "Artwork unavailable or undecodable; attempt " + (attempt + 1));
            } catch (Exception e) {
                // Exception messages can contain private artwork URLs; log only the failure type.
                Log.w("PosterLoader", "Artwork attempt " + (attempt + 1) + ": " + e.getClass().getSimpleName());
            } catch (OutOfMemoryError e) {
                retry = false;
                Log.w("PosterLoader", "Artwork decode exceeded available memory");
            }
            if (bmp != null) CACHE.put(request.uri, bmp);
            final Bitmap result = bmp;
            final boolean canRetry = retry;
            MAIN.post(() -> {
                if (target.getTag() != request) return;
                if (result != null) {
                    target.setImageBitmap(result);
                    onLoaded.run();
                } else if (canRetry && attempt < 2 && target.isAttachedToWindow()) {
                    MAIN.postDelayed(() -> {
                        if (target.getTag() != request) return;
                        if (target.isAttachedToWindow())
                            attempt(app, request, target, maxWidth, onLoaded, attempt + 1);
                        else target.setTag(null);
                    }, attempt == 0 ? 2000 : 5000);
                } else {
                    target.setTag(null); // Allow a fresh attempt on the next Home refresh.
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
        try {
            InputStream in;
            if (scheme.equals("http") || scheme.equals("https")) {
                conn = (HttpURLConnection) new URL(uri).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(8000);
                int status = conn.getResponseCode();
                if (status / 100 != 2) {
                    Log.w("PosterLoader", "Artwork HTTP status " + status);
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
            }
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
