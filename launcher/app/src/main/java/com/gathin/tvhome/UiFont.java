package com.gathin.tvhome;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.io.File;

/**
 * The typeface behind one of the font_* strings in res/values/fonts.xml, for views built in code. The string
 * points at a bundled font, or, in locales Outfit can't draw (values-vi), names a system family (#833).
 */
final class UiFont {
    private UiFont() {}

    private static Typeface privateMedium;
    private static boolean checkedPrivateFont;

    private static Typeface privateMedium(Context context) {
        if (!checkedPrivateFont) {
            checkedPrivateFont = true;
            File font = new File(context.getExternalFilesDir(null), "header-assets/sf_medium.ttf");
            try { if (font.isFile()) privateMedium = Typeface.createFromFile(font); }
            catch (RuntimeException error) { android.util.Log.w("TVHome", "Using bundled fonts", error); }
        }
        return privateMedium;
    }

    /** Private prototype font; absence leaves all theme and locale fonts untouched. */
    static void apply(View root) {
        Typeface font = privateMedium(root.getContext());
        if (font != null) apply(root, font);
    }

    private static void apply(View view, Typeface font) {
        if (view instanceof TextView) ((TextView) view).setTypeface(font);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) apply(group.getChildAt(i), font);
        }
    }

    static Typeface get(Context context, int fontString) {
        Typeface font = privateMedium(context);
        if (font != null) return font;
        Resources res = context.getResources();
        TypedValue value = new TypedValue();
        res.getValue(fontString, value, true);
        String name = String.valueOf(value.string);
        if (name.startsWith("res/")) return res.getFont(value.resourceId);
        return Typeface.create(name, Typeface.NORMAL);
    }
}
