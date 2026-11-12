package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class PopupPlacementTest {
    @Test public void belowMenuClearsTheEntireFocusedCard() {
        assertEquals(38, PopupPlacement.belowOffset(180, 1.1f, 20));
        assertEquals(33, PopupPlacement.belowOffset(128, 1.1f, 20));
        assertEquals(20, PopupPlacement.belowOffset(180, 1f, 20));
    }

    @Test public void shiftsOverflowingMenuInsideRightMargin() {
        assertEquals(-260, PopupPlacement.offset(1500, 80, 600, 0, 1920, 80, false));
    }

    @Test public void preservesAlignmentWhenItFits() {
        assertEquals(0, PopupPlacement.offset(1300, 80, 400, 0, 1920, 48, false));
    }

    @Test public void rightAlignedMenuKeepsLeftMargin() {
        assertEquals(368, PopupPlacement.offset(40, 80, 400, 40, 1960, 48, true));
    }

    @Test public void rightAlignmentDoesNotShiftTwice() {
        assertEquals(0, PopupPlacement.offset(1700, 80, 600, 0, 1920, 48, true));
    }
}
