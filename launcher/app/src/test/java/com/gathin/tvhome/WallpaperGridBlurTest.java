package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class WallpaperGridBlurTest {
    @Test public void blurRadiusScalesWithWallpaperResolution() {
        assertEquals(6, WallpaperPlayer.gridBlurRadius(1920));
        assertEquals(3, WallpaperPlayer.gridBlurRadius(960));
        assertEquals(1, WallpaperPlayer.gridBlurRadius(0));
    }
}
