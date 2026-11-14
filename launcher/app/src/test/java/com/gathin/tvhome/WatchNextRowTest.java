package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WatchNextRowTest {
    @Test public void largeLayoutsShowTheirNamedCountAndLeaveTheNextCardPeeking() {
        for (String layout : new String[]{"two", "three"}) {
            int count = "two".equals(layout) ? 2 : 3;
            int width = WatchNextRow.posterWidth(1920, layout);
            int nextLeft = 90 + count * (width + 52);
            org.junit.Assert.assertTrue(nextLeft < 1920);
            org.junit.Assert.assertTrue(nextLeft + width > 1920);
            assertEquals(width / 2, WatchNextRow.posterWidth(960, layout));
            assertEquals(1, WatchNextRow.posterWidth(0, layout));
        }
        assertEquals(696, WatchNextRow.posterWidth(1920, "two"));
        assertEquals(480, WatchNextRow.posterWidth(1920, "three"));
        assertEquals(340, WatchNextRow.posterWidth(1920, "compact"));
    }

    @Test public void layoutChoicesNormalizeAndUnknownValuesKeepCompact() {
        assertEquals("two", Config.watchNextLayout(" TWO "));
        assertEquals("three", Config.watchNextLayout("three"));
        assertEquals("compact", Config.watchNextLayout("unknown"));
        assertEquals("compact", Config.watchNextLayout(null));
    }

    @Test public void episodeLineUsesOneSpacedDotAndHidesMissingParts() {
        assertEquals("S2 E4 · Discovery", WatchNext.episodeLabel("2", "4", "Discovery"));
        assertEquals("S2 E4", WatchNext.episodeLabel("2", "4", null));
        assertEquals("Discovery", WatchNext.episodeLabel(null, null, "Discovery"));
        assertEquals("E4 · Discovery", WatchNext.episodeLabel(null, "4", "Discovery"));
        org.junit.Assert.assertNull(WatchNext.episodeLabel(null, null, null));
    }

    @Test public void widerPostersLeaveTheFifthCardPartlyOffscreen() {
        int width = WatchNextRow.posterWidth(1920);
        assertEquals(340, width);
        assertEquals(170, WatchNextRow.posterWidth(960));
        assertEquals(1, WatchNextRow.posterWidth(0));
        int fifthLeft = 90 + 4 * (width + 52);
        org.junit.Assert.assertTrue(fifthLeft < 1920);
        org.junit.Assert.assertTrue(fifthLeft + width > 1920);
    }
    @Test
    public void fadesAsTheDockScrollsOverTheRowAndReturnsWithIt() {
        assertEquals(1f, WatchNextRow.scrollAlpha(0, 160), 0f);
        assertEquals(0.5f, WatchNextRow.scrollAlpha(40, 160), 0f);
        assertEquals(0f, WatchNextRow.scrollAlpha(80, 160), 0f);
        assertEquals(0f, WatchNextRow.scrollAlpha(400, 160), 0f);
        assertEquals(0.75f, WatchNextRow.scrollAlpha(20, 160), 0f);
        assertEquals(1f, WatchNextRow.scrollAlpha(0, 160), 0f);
    }

    @Test
    public void handlesOverscrollAndTheRowBeforeItHasBeenMeasured() {
        assertEquals(1f, WatchNextRow.scrollAlpha(-5, 160), 0f);
        assertEquals(1f, WatchNextRow.scrollAlpha(0, 0), 0f);
        assertEquals(0f, WatchNextRow.scrollAlpha(20, 0), 0f);
    }
}
