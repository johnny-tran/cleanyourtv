package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class TileShadowTest {
    @Test public void radiosityMatchesFigmaBlurAtDesignSizeAndScalesWithTheTile() {
        float sigma = TileShadow.radiosityBlurRadius(250) * .57735f + .5f;
        assertEquals(27f / 2f, sigma, .0001f);
        assertEquals(sigma / 2f, TileShadow.radiosityBlurRadius(125) * .57735f + .5f, .0001f);
        assertTrue(TileShadow.radiosityBlurRadius(1) > 0f);
    }
}
