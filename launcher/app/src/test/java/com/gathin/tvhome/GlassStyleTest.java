package com.gathin.tvhome;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** glass_style from config.xml or a theme: rim or halo by name, glass for anything else. */
public class GlassStyleTest {

    @Test
    public void rimByNameAnyCase() {
        assertEquals(Config.GLASS_RIM, Config.glassStyle("rim"));
        assertEquals(Config.GLASS_RIM, Config.glassStyle(" Rim "));
    }

    @Test
    public void haloByNameAnyCase() {
        assertEquals(Config.GLASS_HALO, Config.glassStyle("halo"));
        assertEquals(Config.GLASS_HALO, Config.glassStyle("HALO "));
    }

    @Test public void zoomByName() {
        assertEquals(Config.GLASS_ZOOM, Config.glassStyle(" ZOOM "));
        assertEquals(1.224f, TileFocus.scale(true, false, Config.GLASS_ZOOM), 0.001f);
    }

    @Test public void liftByName() {
        assertEquals(Config.GLASS_LIFT, Config.glassStyle(" LIFT "));
    }

    @Test
    public void anythingElseIsGlass() {
        assertEquals(Config.GLASS_DEFAULT, Config.glassStyle("glass"));
        assertEquals(Config.GLASS_DEFAULT, Config.glassStyle(""));
        assertEquals(Config.GLASS_DEFAULT, Config.glassStyle(null));
        assertEquals(Config.GLASS_DEFAULT, Config.glassStyle("frosted"));
    }
}
