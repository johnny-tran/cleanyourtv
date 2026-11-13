package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WatchNextRowTest {
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
