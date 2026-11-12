package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.Map;

/** Local row choices; never changes a streaming app's Watch Next records or viewing history. */
final class WatchNextDismissals {
    private WatchNextDismissals() { }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("watch_next_removed", Context.MODE_PRIVATE);
    }

    static Map<String, ?> load(Context context) { return preferences(context).getAll(); }

    static String key(WatchNext.Item item) {
        return (item.pkg == null ? "" : item.pkg) + "\n"
                + (item.contentId != null ? "content:" + item.contentId : "row:" + item.rowId);
    }

    static boolean isHidden(WatchNext.Item item, Map<String, ?> removed) {
        Object engagement = removed.get(key(item));
        return engagement instanceof Long && item.lastEngagement <= (Long) engagement;
    }

    static boolean remove(Context context, WatchNext.Item item) {
        // Report a failed disk write instead of claiming the choice will survive a restart.
        return preferences(context).edit().putLong(key(item), item.lastEngagement).commit();
    }

    static boolean restore(Context context) { return preferences(context).edit().clear().commit(); }
}
