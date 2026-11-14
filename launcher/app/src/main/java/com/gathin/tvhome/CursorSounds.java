package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Rect;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/** Launcher-local navigation audio; leaves Android's click sounds and other apps alone. */
final class CursorSounds {
    static final String KEY = "cursor_sounds";

    static boolean enabled(Context context) {
        return context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY, true);
    }

    static boolean setEnabled(Context context, boolean enabled) {
        return context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY, enabled).commit();
    }

    static void play(View view, int effect) {
        if (enabled(view.getContext())) view.playSoundEffect(effect);
    }

    static int direction(int key) {
        switch (key) {
            case KeyEvent.KEYCODE_DPAD_UP: return View.FOCUS_UP;
            case KeyEvent.KEYCODE_DPAD_DOWN: return View.FOCUS_DOWN;
            case KeyEvent.KEYCODE_DPAD_LEFT: return View.FOCUS_LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT: return View.FOCUS_RIGHT;
            default: return 0;
        }
    }

    static View wrap(View content) {
        FrameLayout frame = new FrameLayout(content.getContext()) {
            @Override public boolean dispatchKeyEvent(KeyEvent event) {
                if (super.dispatchKeyEvent(event)) return true;
                int direction = direction(event.getKeyCode());
                if (event.getAction() != KeyEvent.ACTION_DOWN || direction == 0
                        || enabled(getContext())) return false;
                // ViewRootImpl plays automatic D-pad ticks even when View sound effects are disabled.
                // Perform its remaining focus search here so the framework never reaches that sound path.
                View focused = findFocus();
                if (focused == null) {
                    restoreDefaultFocus();
                } else {
                    View next = focused.focusSearch(direction);
                    if (next != null && next != focused) {
                        Rect rect = new Rect();
                        focused.getFocusedRect(rect);
                        offsetDescendantRectToMyCoords(focused, rect);
                        offsetRectIntoDescendantCoords(next, rect);
                        if (next.requestFocus(direction, rect)) return true;
                    }
                    dispatchUnhandledMove(focused, direction);
                }
                return true;
            }
        };
        frame.setClipChildren(false);
        frame.setClipToPadding(false);
        ViewGroup.LayoutParams params = content.getLayoutParams();
        frame.addView(content, new FrameLayout.LayoutParams(
                params == null ? ViewGroup.LayoutParams.MATCH_PARENT : params.width,
                params == null ? ViewGroup.LayoutParams.MATCH_PARENT : params.height));
        return frame;
    }
}
