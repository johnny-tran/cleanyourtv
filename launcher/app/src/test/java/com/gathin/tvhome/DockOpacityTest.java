package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DockOpacityTest {
    @Test public void materialBlurScalesWithOpacity() {
        assertEquals(4, DockBackdrop.blurRadius(100));
        assertEquals(3, DockBackdrop.blurRadius(75));
        assertEquals(3, DockBackdrop.blurRadius(70));
        assertEquals(2, DockBackdrop.blurRadius(50));
        assertEquals(1, DockBackdrop.blurRadius(30));
        assertEquals(0, DockBackdrop.blurRadius(0));
    }

    @Test public void materialOpacityMultipliesTheFillAlphaAndPreservesItsColor() {
        assertEquals(0x69000000, DockBackdrop.fillColor(0x69000000, 100));
        assertEquals(0x4F000000, DockBackdrop.fillColor(0x69000000, 75));
        assertEquals(0x35123456, DockBackdrop.fillColor(0x69123456, 50));
        assertEquals(0x80181818, DockBackdrop.fillColor(0xFF181818, 50));
        assertEquals(0x00123456, DockBackdrop.fillColor(0x00123456, 75));
    }

    @Test public void aChoiceMadeOnTheTvWinsOverTheTheme() {
        // Theme says 80, the owner picked 30 in Launcher settings.
        assertEquals(30, Config.chosenDockOpacity(30, 80));
        assertEquals(100, Config.chosenDockOpacity(100, 80));
    }

    @Test public void noChoiceKeepsTheThemeValue() {
        // -1 is what Config passes when nothing is saved (or a theme push cleared it).
        assertEquals(80, Config.chosenDockOpacity(-1, 80));
    }

    @Test public void anOutOfRangeChoiceIsIgnored() {
        // Below 30 the band would vanish and the dock lose its shape; the theme key has the same floor.
        assertEquals(80, Config.chosenDockOpacity(0, 80));
        assertEquals(80, Config.chosenDockOpacity(29, 80));
        assertEquals(80, Config.chosenDockOpacity(101, 80));
    }
}
