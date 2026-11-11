package com.gathin.tvhome;

/** Physical Left/Right within the top bar, independent of the posters below it. */
final class TopBarNavigation {
    private TopBarNavigation() { }

    static int next(boolean[] available, int current, boolean left, boolean layoutRtl) {
        int step = left != layoutRtl ? -1 : 1;
        for (int i = current + step; i >= 0 && i < available.length; i += step) {
            if (available[i]) return i;
        }
        return current;
    }
}
