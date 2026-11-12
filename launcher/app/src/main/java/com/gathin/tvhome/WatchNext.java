package com.gathin.tvhome;

import android.content.ContentResolver;
import android.database.Cursor;
import android.media.tv.TvContract;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads the system Continue watching / Watch Next list that other apps publish. A launcher holding
 * {@code READ_TV_LISTINGS} sees its own rows plus every row another app marked searchable (docs/watch-next.md).
 *
 * Fork note: this and {@link PosterLoader}, {@link WatchNextRow} and res/layout/item_watch.xml are the
 * Continue Watching feature, ported from dhruv2003/sony-tv-launcher (MIT); see
 * docs/fork.md for the upstream-update recipe.
 */
final class WatchNext {

    static final class Item {
        long rowId;
        String contentId;
        long lastEngagement;
        String title;
        String subtitle;
        String art;
        float progress;
        String intentUri;
        String pkg;
    }

    static final class Result {
        final List<Item> items = new ArrayList<>();
        /** null on a successful query; else a short reason so the caller can tell failure from "no rows". */
        String error;
    }

    private static final int MAX_ITEMS = 20;
    // Named columns, not null: some providers' default projection includes a split_id column their
    // watch_next_programs table lacks (docs/watch-next.md). No selection: a caller without
    // ACCESS_ALL_EPG_DATA gets a SecurityException for one.
    private static final String[] PROJECTION = {
            "_id", "title", "episode_title", "season_display_number", "episode_display_number",
            "poster_art_uri", "thumbnail_uri", "last_playback_position_millis", "duration_millis",
            "intent_uri", "package_name", "last_engagement_time_utc_millis", "internal_provider_id",
    };

    private WatchNext() {
    }

    static Result query(ContentResolver resolver, Map<String, ?> removed) {
        Result result = new Result();
        try (Cursor c = resolver.query(TvContract.WatchNextPrograms.CONTENT_URI, PROJECTION,
                null, null, "last_engagement_time_utc_millis DESC")) {
            if (c == null) {
                result.error = "no cursor";
                return result;
            }
            while (c.moveToNext() && result.items.size() < MAX_ITEMS) {
                Item it = new Item();
                it.rowId = num(c, "_id");
                it.contentId = str(c, "internal_provider_id");
                it.lastEngagement = num(c, "last_engagement_time_utc_millis");
                it.pkg = str(c, "package_name");
                // Filter before the display limit, so removed cards are replaced by the next available titles.
                if (WatchNextDismissals.isHidden(it, removed)) continue;
                it.title = str(c, "title");
                if (it.title == null) continue;
                it.subtitle = subtitle(c);
                it.art = first(str(c, "poster_art_uri"), str(c, "thumbnail_uri"));
                long pos = num(c, "last_playback_position_millis");
                long dur = num(c, "duration_millis");
                it.progress = dur > 0 && pos > 0 ? Math.min(1f, (float) pos / dur) : 0f;
                it.intentUri = str(c, "intent_uri");
                if (it.intentUri == null && it.pkg == null) continue;   // nothing to launch
                result.items.add(it);
            }
        } catch (SecurityException e) {
            result.error = "not permitted";
        } catch (Exception e) {
            result.error = e.getClass().getSimpleName();
        }
        return result;
    }

    private static String subtitle(Cursor c) {
        String season = str(c, "season_display_number");
        String episode = str(c, "episode_display_number");
        String epTitle = str(c, "episode_title");
        StringBuilder sub = new StringBuilder();
        if (season != null) sub.append('S').append(season);
        if (episode != null) sub.append(sub.length() > 0 ? " " : "").append('E').append(episode);
        if (epTitle != null) sub.append(sub.length() > 0 ? " \u00b7 " : "").append(epTitle);
        return sub.length() > 0 ? sub.toString() : null;
    }

    private static String str(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        if (i < 0 || c.isNull(i)) return null;
        String s = c.getString(i);
        return s == null || s.trim().isEmpty() ? null : s;
    }

    private static long num(Cursor c, String col) {
        int i = c.getColumnIndex(col);
        return i < 0 || c.isNull(i) ? 0L : c.getLong(i);
    }

    private static String first(String a, String b) {
        return a != null ? a : b;
    }
}
