package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class TileFocusTest {
    @Test public void animationSwitchControlsTheScaleForEveryTileKind() {
        assertEquals(1f, TileFocus.scale(false, false), 0f);
        assertEquals(1f, TileFocus.scale(false, true), 0f);
        assertEquals(1.10f, TileFocus.scale(true, false), 0f);
        assertEquals(1.12f, TileFocus.scale(true, true), 0f);
    }

    @Test public void postersAndGridAppsUseWhiteUnlessAppGlowIsEnabled() {
        assertFalse(TileFocus.coloured(false, true, false, false, false));
        assertTrue(TileFocus.coloured(true, true, true, false, false));
        assertTrue(TileFocus.coloured(true, false, false, false, false));
    }

    @Test public void dockColourAndFestivalRulesArePreserved() {
        assertTrue(TileFocus.coloured(false, true, false, true, false));
        assertFalse(TileFocus.coloured(false, true, true, true, false));
        assertFalse(TileFocus.coloured(true, true, false, true, true));
    }
}
