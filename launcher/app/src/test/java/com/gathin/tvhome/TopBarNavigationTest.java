package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TopBarNavigationTest {
    // Inputs, Wi-Fi, hidden VPN and Reorder, App Library, Settings.
    private final boolean[] buttons = {true, true, false, false, true, true};

    @Test public void rightVisitsEveryVisibleButtonIncludingSettings() {
        int index = 0;
        for (int expected : new int[]{1, 4, 5, 5}) {
            index = TopBarNavigation.next(buttons, index, false, false);
            assertEquals(expected, index);
        }
    }

    @Test public void leftReturnsToInputsAndStopsAtTheEdge() {
        int index = 5;
        for (int expected : new int[]{4, 1, 0, 0}) {
            index = TopBarNavigation.next(buttons, index, true, false);
            assertEquals(expected, index);
        }
    }

    @Test public void hiddenOrDisabledButtonsAreSkippedAtRuntime() {
        assertEquals(5, TopBarNavigation.next(new boolean[]{true, false, false, false, false, true}, 0, false, false));
        assertEquals(0, TopBarNavigation.next(new boolean[]{true, false, false, false, false, false}, 0, false, false));
    }

    @Test public void physicalArrowsFollowTheBarsLayoutDirection() {
        assertEquals(4, TopBarNavigation.next(buttons, 5, false, true));
        assertEquals(5, TopBarNavigation.next(buttons, 4, true, true));
    }
}
