package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PopupStyleTest {
    @Test public void classicAcceptsCaseAndWhitespace() {
        assertEquals(Config.POPUP_CLASSIC, Config.popupStyle("classic"));
        assertEquals(Config.POPUP_CLASSIC, Config.popupStyle(" CLASSIC "));
    }

    @Test public void glassAndUnknownValuesPreserveExistingAppearance() {
        for (String value : new String[] {"glass", " GLASS ", "", "halo", null}) {
            assertEquals(Config.POPUP_GLASS, Config.popupStyle(value));
        }
    }
}
