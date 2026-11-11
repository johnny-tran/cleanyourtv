package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.widget.HorizontalScrollView;

/** Continue watching (fork): focus scrolling keeps the row's margins, including on the return trip. */
public final class WatchNextScrollView extends HorizontalScrollView {
    public WatchNextScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        View row = getChildAt(0);
        if (row == null) return 0;
        // The row's padding sets its resting margin. Native focus scrolling ignores that padding and
        // only reveals the card at the physical screen edge, leaving the first card clipped on return.
        return focusScrollDelta(getScrollX(), getWidth(), rect.left, rect.right, row.getPaddingLeft());
    }

    static int focusScrollDelta(int scrollX, int width, int left, int right, int inset) {
        int visibleLeft = scrollX + inset;
        int visibleRight = scrollX + width - inset;
        if (left < visibleLeft) return left - visibleLeft;
        if (right > visibleRight) return right - visibleRight;
        return 0;
    }
}
