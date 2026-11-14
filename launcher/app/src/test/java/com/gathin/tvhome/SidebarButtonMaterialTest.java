package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class SidebarButtonMaterialTest {
    @Test public void lightingCacheSharesShapesAndRetainsOnlyTwoGeometries() {
        SidebarButtonMaterial.Lighting first = SidebarButtonMaterial.lighting(16, 8, 4, 1);
        assertSame(first, SidebarButtonMaterial.lighting(16, 8, 4, 1));
        assertEquals(SidebarButtonMaterial.insetCoverage(.5f, .5f, 16, 8, 4, 40, 30),
                first.shade[0], .00001f);
        assertNotSame(first, SidebarButtonMaterial.lighting(16, 8, 2, 1));
        assertNotSame(first, SidebarButtonMaterial.lighting(16, 8, 4, .5f));
        assertNotSame(first, SidebarButtonMaterial.lighting(16, 8, 4, 1));
    }

    @Test public void focusAddsTheDodgeAndScreenFillsBelowSharedLighting() {
        int off = SidebarButtonMaterial.shadePixel(0xFF202020, false, 0, 0) & 255;
        int on = SidebarButtonMaterial.shadePixel(0xFF202020, true, 0, 0) & 255;
        assertEquals(56, off);
        assertEquals(201, on);
        for (boolean focused : new boolean[]{false, true}) {
            int base = SidebarButtonMaterial.shadePixel(0xFF202020, focused, 0, 0) & 255;
            assertEquals(base + 20, SidebarButtonMaterial.shadePixel(
                    0xFF202020, focused, .5f, 0) & 255);
            assertEquals(base - 12.5f, SidebarButtonMaterial.shadePixel(
                    0xFF202020, focused, 0, .5f) & 255, .51f);
        }
    }

    @Test public void insetLightFollowsThePillEdgesAndScalesWithDensity() {
        assertEquals(.5f, SidebarButtonMaterial.insetCoverage(179, 0, 358, 74, 40, 10), .0001f);
        assertEquals(0f, SidebarButtonMaterial.insetCoverage(179, 37, 358, 74, 40, 10), .0001f);
        assertEquals(.5f, SidebarButtonMaterial.insetCoverage(179, 74, 358, 74, -40, 10), .0001f);
        assertEquals(SidebarButtonMaterial.insetCoverage(8, 15, 358, 74, 40, 30),
                SidebarButtonMaterial.insetCoverage(4, 7.5f, 179, 37, 20, 15), .0001f);
    }

    @Test public void blendsClampAtBlackAndWhite() {
        assertEquals(0xFFFFFFFF, SidebarButtonMaterial.shadePixel(0xFFFFFFFF, true, 1, 0));
        assertEquals(0xFF000000, SidebarButtonMaterial.shadePixel(0xFF000000, false, 0, 2));
    }

    @Test public void sidebarLightingUsesItsOwnCornerInsteadOfTheButtonPillRadius() {
        assertEquals(.5f, SidebarButtonMaterial.insetCoverage(191, 0, 382, 364, 48, 40, 10), .0001f);
        assertEquals(0f, SidebarButtonMaterial.insetCoverage(191, 182, 382, 364, 48, 40, 30), .0001f);
        float corner = SidebarButtonMaterial.insetCoverage(24, 8, 382, 364, 48, 40, 30);
        assertEquals(corner, SidebarButtonMaterial.insetCoverage(12, 4, 191, 182, 24, 20, 15), .0001f);
        assertNotEquals(corner, SidebarButtonMaterial.insetCoverage(24, 8, 382, 364, 40, 30), .01f);
    }
}
