package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class GlassSizeTest {
    @Test public void shrinkingPassesThroughIntermediateBoundsAndCorners() {
        GlassView.Size size = new GlassView.Size();
        size.retarget(376, 222, 32f);
        size.at(1f);
        size.retarget(276, 166, 24f);
        size.at(0f);
        assertEquals(376f, size.width, 0f);
        size.at(0.5f);
        assertEquals(326f, size.width, 0f);
        assertEquals(194f, size.height, 0f);
        assertEquals(28f, size.radius, 0f);
        size.at(1f);
        assertEquals(276f, size.width, 0f);
    }

    @Test public void rapidReversalStartsFromTheCurrentSizeWithoutSnapping() {
        GlassView.Size size = new GlassView.Size();
        size.retarget(376, 222, 32f);
        size.at(1f);
        size.retarget(276, 166, 24f);
        size.at(0.5f);
        assertTrue(size.retarget(376, 222, 32f));
        size.at(0f);
        assertEquals(326f, size.width, 0f);
        assertEquals(194f, size.height, 0f);
        size.at(0.5f);
        assertEquals(351f, size.width, 0f);
        size.at(1f);
        assertEquals(376f, size.width, 0f);
        assertEquals(222f, size.height, 0f);
        assertEquals(32f, size.radius, 0f);
    }

    @Test public void layoutRefitsDoNotRestartAnInFlightResize() {
        GlassView.Size size = new GlassView.Size();
        size.retarget(376, 222, 32f);
        size.at(1f);
        size.retarget(276, 166, 24f);
        size.at(0.25f);
        assertFalse(size.retarget(276, 166, 24f));
        size.at(0.5f);
        assertEquals(326f, size.width, 0f);
    }
}
