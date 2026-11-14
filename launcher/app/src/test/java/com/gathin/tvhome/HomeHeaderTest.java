package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class HomeHeaderTest {
    @Test public void morphRetainsExactEndpointsAndReversesThroughTheSameBounds() {
        assertEquals(165f, HomeHeader.mix(165f, 0f, 0f), 0.001f);
        assertEquals(0f, HomeHeader.mix(165f, 0f, 1f), 0.001f);
        assertEquals(82.5f, HomeHeader.mix(165f, 0f, .5f), 0.001f);
        assertEquals(66f, HomeHeader.mix(66f, 410f, -1f), 0.001f);
        assertEquals(410f, HomeHeader.mix(66f, 410f, 2f), 0.001f);
        assertEquals(1837f, HomeHeader.mix(1837f, 1837f, .5f), 0.001f);
    }

    @Test public void materialFadeMirrorsAtTheTwoEndsOfTheMorph() {
        assertEquals(0f, HomeHeader.materialProgress(0f, 1f, 0, 420), 0f);
        assertEquals(.5f, HomeHeader.materialProgress(0f, 1f, 45, 420), 0f);
        assertEquals(1f, HomeHeader.materialProgress(0f, 1f, 90, 420), 0f);
        assertEquals(1f, HomeHeader.materialProgress(1f, 0f, 330, 420), 0f);
        assertEquals(.5f, HomeHeader.materialProgress(1f, 0f, 375, 420), 0f);
        assertEquals(0f, HomeHeader.materialProgress(1f, 0f, 420, 420), 0f);
        for (long time = 0; time <= 420; time += 15) {
            assertEquals(HomeHeader.materialProgress(0f, 1f, time, 420),
                    HomeHeader.materialProgress(1f, 0f, 420 - time, 420), .00001f);
        }
    }

    @Test public void interruptedMaterialFadeRetainsItsCurrentColorAndReachesTheEndpoint() {
        assertEquals(.5f, HomeHeader.materialProgress(.5f, 0f, 0, 100), 0f);
        assertEquals(.5f, HomeHeader.materialProgress(.5f, 0f, 55, 100), 0f);
        assertEquals(0f, HomeHeader.materialProgress(.5f, 0f, 100, 100), 0f);
        assertEquals(0f, HomeHeader.materialProgress(1f, 0f, 20, 20), 0f);
        assertEquals(1f, HomeHeader.materialProgress(1f, 1f, 0, 420), 0f);
    }

    @Test public void pillMaterialChangesOnlyItsTintOnFocusAndDuringTheHandoff() {
        assertEquals(0x69000000, PopupMaterial.tintForState(new int[0], 0x99FFFFFF));
        assertEquals(0x99FFFFFF, PopupMaterial.tintForState(
                new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0x99FFFFFF));
        assertEquals(0x99FFFFFF, PopupMaterial.tintForState(
                new int[]{android.R.attr.state_activated}, 0x99FFFFFF));
        assertEquals(0x69000000, PopupMaterial.tintForState(
                new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed}, 0x99FFFFFF));
    }

    @Test public void sidebarRowsWaitForFinalMaterialsThenKeepTheirFullOpacity() {
        assertEquals(0f, HomeHeader.rowAlpha(.8f, false), 0f);
        assertEquals(0f, HomeHeader.rowAlpha(1f, false), 0f);
        assertEquals(0f, HomeHeader.rowAlpha(0f, true), 0f);
        assertEquals(1f, HomeHeader.rowAlpha(.01f, true), 0f);
        assertEquals(1f, HomeHeader.rowAlpha(.5f, true), 0f);
        assertEquals(1f, HomeHeader.rowAlpha(1f, true), 0f);
    }

    @Test public void sidebarVisitsAllActionsAndStopsAtBothEdges() {
        int row = 0;
        for (int expected : new int[]{1, 2, 2}) {
            row = HomeHeader.nextRow(row, false);
            assertEquals(expected, row);
        }
        for (int expected : new int[]{1, 0, 0}) {
            row = HomeHeader.nextRow(row, true);
            assertEquals(expected, row);
        }
    }
}
