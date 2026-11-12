package com.gathin.tvhome;

/** Keep dropdowns inside the viewport while retaining their anchor alignment when it fits. */
final class PopupPlacement {
    private PopupPlacement() { }

    /** Dropdown anchors use the unscaled height even though their screen position includes focus zoom. */
    static int belowOffset(int anchorHeight, float scaleY, int gap) {
        return gap + Math.round(anchorHeight * (scaleY - 1f));
    }

    static int offset(int anchorLeft, int anchorWidth, int popupWidth, int viewportLeft,
                      int viewportRight, int margin, boolean alignRight) {
        int alignedLeft = anchorLeft + (alignRight ? anchorWidth - popupWidth : 0);
        int left = Math.max(viewportLeft + margin,
                Math.min(alignedLeft, viewportRight - margin - popupWidth));
        return left - alignedLeft;
    }
}
