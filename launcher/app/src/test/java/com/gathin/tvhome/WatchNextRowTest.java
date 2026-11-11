package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WatchNextRowTest {
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
