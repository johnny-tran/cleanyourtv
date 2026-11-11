package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WatchNextScrollViewTest {
    @Test
    public void returningToFirstCardRestoresTheOriginalMargin() {
        assertEquals(-72, WatchNextScrollView.focusScrollDelta(72, 960, 72, 232, 72));
        assertEquals(0, WatchNextScrollView.focusScrollDelta(0, 960, 72, 232, 72));
    }

    @Test
    public void scrollingPastFifthCardKeepsTheSameInsetOnTheRight() {
        assertEquals(88, WatchNextScrollView.focusScrollDelta(0, 960, 816, 976, 72));
        assertEquals(186, WatchNextScrollView.focusScrollDelta(88, 960, 1002, 1162, 72));
    }

    @Test
    public void movingLeftRevealsTheCardAtTheInsetInsteadOfTheScreenEdge() {
        assertEquals(-88, WatchNextScrollView.focusScrollDelta(274, 960, 258, 418, 72));
        assertEquals(0, WatchNextScrollView.focusScrollDelta(186, 960, 258, 418, 72));
    }

    @Test
    public void cardsAlreadyInsideTheMarginsDoNotMoveTheRow() {
        assertEquals(0, WatchNextScrollView.focusScrollDelta(0, 960, 444, 604, 72));
    }
}
