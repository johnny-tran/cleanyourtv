package com.gathin.tvhome;

import org.junit.Test;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class WatchNextDismissalsTest {
    private WatchNext.Item item(String pkg, String id, long engaged) {
        WatchNext.Item item = new WatchNext.Item();
        item.pkg = pkg;
        item.contentId = id;
        item.rowId = 42;
        item.lastEngagement = engaged;
        return item;
    }

    @Test public void dismissalSurvivesRefreshUntilNewViewingActivityIsReported() {
        WatchNext.Item item = item("app.one", "show", 100);
        Map<String, Long> hidden = Collections.singletonMap(WatchNextDismissals.key(item), 100L);
        assertTrue(WatchNextDismissals.isHidden(item, hidden));
        item.progress = 0.6f;
        assertTrue(WatchNextDismissals.isHidden(item, hidden));
        item.lastEngagement = 101;
        assertFalse(WatchNextDismissals.isHidden(item, hidden));
    }

    @Test public void stableContentIdentitySurvivesProviderRowReplacement() {
        WatchNext.Item item = item("app.one", "show", 100);
        String key = WatchNextDismissals.key(item);
        item.rowId = 99;
        assertEquals(key, WatchNextDismissals.key(item));
        assertNotEquals(key, WatchNextDismissals.key(item("app.two", "show", 100)));
        assertNotEquals(key, WatchNextDismissals.key(item("app.one", "other", 100)));
    }

    @Test public void undatedEntriesStayRemovedAndManualRestoreMakesThemVisible() {
        WatchNext.Item item = item("app.one", "show", 0);
        Map<String, Long> hidden = new HashMap<>();
        hidden.put(WatchNextDismissals.key(item), 0L);
        assertTrue(WatchNextDismissals.isHidden(item, hidden));
        hidden.clear();
        assertFalse(WatchNextDismissals.isHidden(item, hidden));
    }

    @Test public void rowIdFallbackSeparatesEntriesWithoutContentIds() {
        WatchNext.Item first = item("app.one", null, 100);
        WatchNext.Item second = item("app.one", null, 100);
        second.rowId++;
        assertNotEquals(WatchNextDismissals.key(first), WatchNextDismissals.key(second));
    }
}
