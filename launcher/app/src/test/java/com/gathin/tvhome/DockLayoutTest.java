package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class DockLayoutTest {
    @Test public void dockEndsMatchVerticalClearanceThroughoutFocusZoom() {
        float scale = 306f / 250f;
        assertEquals(40f, DockLayout.endPadding(150, 1f, 2), 0.001f);
        assertEquals(23.2f, DockLayout.endPadding(150, scale, 2), 0.001f);
        float half = (1f + scale) / 2;
        assertEquals(31.6f, DockLayout.endPadding(150, half, 2), 0.001f);
        assertEquals(50f, DockLayout.scaledStart(90, 125, 1f) - DockLayout.endPadding(150, 1f, 2), 0.001f);
        assertEquals(38.8f, DockLayout.scaledStart(90, 125, scale) - DockLayout.endPadding(150, scale, 2), 0.001f);
        assertEquals(1881.2f, DockLayout.scaledStart(1580, 125, scale) + 250 * scale
                + DockLayout.endPadding(150, scale, 2), 0.001f);
        assertEquals(0f, DockLayout.endPadding(150, 2f, 2), 0f);
    }
    @Test public void matchesFigmaAt1080p() {
        assertEquals(250, DockLayout.tileWidth(1788, 6, 2));
        assertEquals(150, DockLayout.tileHeight(250));
        assertEquals(30f, DockLayout.corner(250), 0.001f);
        assertEquals(824, DockLayout.parkedTop(1080, 150, 2) + 28);
        assertEquals(1.224f, TileFocus.scale(true, false, Config.GLASS_LIFT), 0.001f);
        assertEquals(1f, TileFocus.scale(false, false, Config.GLASS_LIFT), 0f);
    }
    @Test public void narrowerLayoutsShrinkWithoutNegativeSizes() {
        assertEquals(152, DockLayout.tileWidth(1200, 6, 2));
        assertEquals(1, DockLayout.tileWidth(100, 8, 2));
        assertEquals(Config.DOCK_STYLE_DARK, Config.dockStyle(" DARK "));
        assertEquals(Config.DOCK_STYLE_CLASSIC, Config.dockStyle("unknown"));
        assertEquals(Config.DOCK_STYLE_CLASSIC, Config.dockStyle(null));
    }
}
