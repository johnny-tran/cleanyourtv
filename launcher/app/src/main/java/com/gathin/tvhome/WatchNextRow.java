package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.DecelerateInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;

/**
 * The Continue watching row on Home: poster cards from {@link WatchNext}, placed above the dock. Kept in its
 * own class so the fork's edits to MainActivity stay a handful of marked lines. Ported from
 * dhruv2003/sony-tv-launcher (MIT); see docs/fork.md.
 *
 * The row owns its own view tree (inflated into the container the layout provides) and reports through
 * {@link Host}. It never touches the launcher's grid, so a failure here cannot take Home down.
 */
final class WatchNextRow {

    interface Host {
        Activity activity();
        /** True while focus is inside the row, so the launcher can hide its own highlight. */
        void onRowFocus(boolean inRow);
        /** Up from the first row: hand focus back to the launcher (top bar). */
        void onLeaveUp(View from);
        /** Down from the row: hand focus to the dock, the launcher's usual Home focus. */
        void onLeaveDown();
    }

    private final Host host;
    private final View container;
    private final View block;          // title + scroll wrapper, hidden when there is nothing to resume
    private final HorizontalScrollView scroll;
    private final LinearLayout row;
    private final float density;

    private List<WatchNext.Item> items = Collections.emptyList();
    private int lastFocused;
    private String signature = "";
    private boolean loaded;

    WatchNextRow(Host host, View container, View block, HorizontalScrollView scroll, LinearLayout row) {
        this.host = host;
        this.container = container;
        this.block = block;
        this.scroll = scroll;
        this.row = row;
        this.density = container.getResources().getDisplayMetrics().density;
    }

    /** True when there is a card to focus. */
    boolean visible() {
        return block.getVisibility() == View.VISIBLE && row.getChildCount() > 0;
    }

    View lastFocusedView() {
        if (row.getChildCount() == 0) return null;
        return row.getChildAt(Math.max(0, Math.min(lastFocused, row.getChildCount() - 1)));
    }

    void focus() {
        View v = lastFocusedView();
        if (v != null) v.requestFocus();
    }

    /**
     * Re-reads the list on a background thread and rebuilds the row when it changed. Called from onResume,
     * so it picks up anything watched while Home was away.
     */
    void refresh(Runnable onChanged) {
        new Thread(() -> {
            WatchNext.Result result = WatchNext.query(host.activity().getContentResolver());
            host.activity().runOnUiThread(() -> render(result, onChanged));
        }, "watch-next").start();
    }

    private void render(WatchNext.Result result, Runnable onChanged) {
        List<WatchNext.Item> next = result.error == null ? result.items : Collections.emptyList();
        StringBuilder sig = new StringBuilder(result.error == null ? "" : result.error);
        for (WatchNext.Item it : next) {
            sig.append(it.title).append(':').append(it.subtitle).append(':').append(it.art)
                    .append(':').append(it.progress).append('|');
        }
        if (loaded && sig.toString().equals(signature)) return;   // nothing changed since the last read
        signature = sig.toString();
        loaded = true;

        boolean wasInRow = focusedCard() != null;
        row.removeAllViews();
        items = next;
        for (int i = 0; i < next.size(); i++) row.addView(card(next.get(i), i));
        block.setVisibility(next.isEmpty() ? View.GONE : View.VISIBLE);
        lastFocused = Math.min(lastFocused, Math.max(0, next.size() - 1));
        if (onChanged != null) onChanged.run();   // the launcher repositions the row above the dock
        if (wasInRow) {
            if (next.isEmpty()) host.onLeaveDown();
            else focus();
        }
    }

    private View card(WatchNext.Item item, int index) {
        View v = LayoutInflater.from(container.getContext()).inflate(R.layout.item_watch, row, false);
        View card = v.findViewById(R.id.watch_card);
        ImageView poster = v.findViewById(R.id.poster);
        TextView fallback = v.findViewById(R.id.fallback_title);
        TextView name = v.findViewById(R.id.name);
        roundCorners(card);
        card.setContentDescription(item.title);
        fallback.setText(item.title);
        name.setText(item.title);

        View track = v.findViewById(R.id.progress_track);
        View fill = v.findViewById(R.id.progress_fill);
        if (item.progress > 0f) {
            fill.post(() -> {
                View parent = (View) fill.getParent();
                if (parent.getWidth() > 0) fill.getLayoutParams().width = Math.max(1, (int) (parent.getWidth() * item.progress));
                fill.requestLayout();
            });
        } else {
            fill.setVisibility(View.GONE);
            track.setVisibility(View.GONE);
        }

        if (item.art != null) {
            PosterLoader.load(container.getContext(), item.art, poster, (int) (320 * density),
                    () -> fallback.setVisibility(View.GONE));
        }

        card.setOnFocusChangeListener((view, focused) -> {
            animateFocus(view, name, focused);
            if (!focused) return;
            lastFocused = index;
            host.onRowFocus(true);
        });
        card.setOnKeyListener((view, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                host.onLeaveUp(view);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                host.onLeaveDown();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) return index == 0;
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) return index == row.getChildCount() - 1;
            return false;
        });
        card.setOnClickListener(view -> launch(item));
        return v;
    }

    private View focusedCard() {
        for (int i = 0; i < row.getChildCount(); i++) {
            View card = row.getChildAt(i).findViewById(R.id.watch_card);
            if (card != null && card.isFocused()) return card;
        }
        return null;
    }

    /** The launcher's own tile corner: 16dp at the design width, scaled down with the card. */
    private void roundCorners(View card) {
        final float radius = 16f * density;
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        card.setClipToOutline(true);
    }

    /** The launcher's lift-and-label focus animation, so the row matches the tiles (AppAdapter.Holder). */
    private void animateFocus(View card, TextView name, boolean focused) {
        card.animate().cancel();
        name.animate().cancel();
        float s = focused ? AppAdapter.FOCUS_SCALE : 1f;
        card.animate().scaleX(s).scaleY(s)
                .translationZ(focused ? 8f * density : 0f)
                .setDuration(focused ? 260 : 180)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .withLayer().start();
        name.animate().alpha(focused ? 1f : 0f)
                .translationY(focused ? density * 4f : -density * 6f)
                .setDuration(220).start();
    }

    private void launch(WatchNext.Item item) {
        Intent intent = null;
        if (item.intentUri != null) {
            try {
                intent = Intent.parseUri(item.intentUri, Intent.URI_INTENT_SCHEME);
                intent.setSelector(null);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            } catch (Exception e) {
                intent = null;
            }
        }
        if (intent == null && item.pkg != null) {
            intent = host.activity().getPackageManager().getLeanbackLaunchIntentForPackage(item.pkg);
            if (intent == null) intent = host.activity().getPackageManager().getLaunchIntentForPackage(item.pkg);
            if (intent != null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        if (intent == null) {
            Toast.makeText(container.getContext(), container.getContext().getString(R.string.cant_open, item.title),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            host.activity().startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(container.getContext(), container.getContext().getString(R.string.cant_open, item.title),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /** A short description of why the row is empty, for a log line; null when it holds cards. */
    String emptyReason() {
        return items.isEmpty() ? signature : null;
    }
}
