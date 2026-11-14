package com.gathin.tvhome;

import android.view.KeyEvent;
import android.view.View;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class CursorSoundsTest {
    @Test public void onlyDirectionalKeysUseTheSilentFocusFallback() {
        assertEquals(View.FOCUS_UP, CursorSounds.direction(KeyEvent.KEYCODE_DPAD_UP));
        assertEquals(View.FOCUS_DOWN, CursorSounds.direction(KeyEvent.KEYCODE_DPAD_DOWN));
        assertEquals(View.FOCUS_LEFT, CursorSounds.direction(KeyEvent.KEYCODE_DPAD_LEFT));
        assertEquals(View.FOCUS_RIGHT, CursorSounds.direction(KeyEvent.KEYCODE_DPAD_RIGHT));
        assertEquals(0, CursorSounds.direction(KeyEvent.KEYCODE_DPAD_CENTER));
        assertEquals(0, CursorSounds.direction(KeyEvent.KEYCODE_ENTER));
        assertEquals(0, CursorSounds.direction(KeyEvent.KEYCODE_BACK));
        assertEquals(0, CursorSounds.direction(KeyEvent.KEYCODE_HOME));
        assertEquals(0, CursorSounds.direction(KeyEvent.KEYCODE_VOLUME_UP));
    }
}
