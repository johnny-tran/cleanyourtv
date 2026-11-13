package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
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

    static int posterWidth(int viewportWidth) {
        return Math.max(1, Math.round(viewportWidth * (340f / 1920f)));
    }

    /** Finish fading as the rising dock reaches the row's center; scrolling back restores it. */
    static float scrollAlpha(int scrolled, int rowHeight) {
        return Math.max(0f, Math.min(1f, 1f - scrolled / (float) Math.max(1f, rowHeight / 2f)));
    }

    interface Host {
        Activity activity();
        /** Shares the launcher's floating focus highlight and scroll bookkeeping. */
        void onCardFocused(View card);
        void onRowScrolled(int dx);
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
    private final Config cfg;

    private List<WatchNext.Item> items = Collections.emptyList();
    private int lastFocused;
    private String signature = "";
    private boolean loaded;
    private Runnable onChanged;
    private int refreshGeneration;

    WatchNextRow(Host host, View container, View block, HorizontalScrollView scroll, LinearLayout row, Config cfg) {
        this.host = host;
        this.cfg = cfg;
        this.container = container;
        this.block = block;
        this.scroll = scroll;
        scroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> {
            if (focusedCard() != null) host.onRowScrolled(x - oldX);
        });
        this.row = row;
        this.density = container.getResources().getDisplayMetrics().density;
        if (Config.DOCK_STYLE_DARK.equals(cfg.dockStyle)) {
            scroll.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
                if (r - l == or - ol) return;
                for (int i = 0; i < row.getChildCount(); i++) sizePoster(row.getChildAt(i));
            });
        }
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
        this.onChanged = onChanged;
        int generation = ++refreshGeneration;
        new Thread(() -> {
            WatchNext.Result result = WatchNext.query(host.activity().getContentResolver(),
                    WatchNextDismissals.load(host.activity()));
            host.activity().runOnUiThread(() -> {
                if (generation == refreshGeneration && !host.activity().isDestroyed()) render(result, onChanged);
            });
        }, "watch-next").start();
    }

    private void render(WatchNext.Result result, Runnable onChanged) {
        List<WatchNext.Item> next = result.error == null ? result.items : Collections.emptyList();
        StringBuilder sig = new StringBuilder(result.error == null ? "" : result.error);
        for (WatchNext.Item it : next) {
            sig.append(WatchNextDismissals.key(it)).append(':').append(it.lastEngagement).append(':');
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
        card.setTag(item);
        ImageView poster = v.findViewById(R.id.poster);
        TextView fallback = v.findViewById(R.id.fallback_title);
        TextView name = v.findViewById(R.id.name);
        sizePoster(v);
        roundCorners(card);
        card.setContentDescription(item.title);
        fallback.setText(item.title);
        name.setText(item.title);
        name.setAlpha(cfg.alwaysShowNames ? 0.75f : 0f);

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
                    () -> {
                        fallback.setVisibility(View.GONE);
                        if (card.isFocused()) host.onCardFocused(card);
                    });
        }

        card.setOnFocusChangeListener((view, focused) -> {
            TileFocus.animate(view, poster, name, focused, cfg.animations, cfg.advancedEffects,
                    cfg.reducedMotion, cfg.alwaysShowNames ? 0.75f : 0f, cfg.focusOvershoot, false, cfg.glassStyle);
            if (!focused) return;
            lastFocused = index;
            host.onCardFocused(view);
        });
        card.setOnKeyListener((view, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                host.onLeaveUp(view);
                if (!view.isFocused()) view.playSoundEffect(SoundEffectConstants.NAVIGATION_UP);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                host.onLeaveDown();
                if (!view.isFocused()) view.playSoundEffect(SoundEffectConstants.NAVIGATION_DOWN);
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                boolean left = keyCode == KeyEvent.KEYCODE_DPAD_LEFT;
                int target = index + (left ? -1 : 1);
                // Handle the move here: HorizontalScrollView changes focus without the grid's system tick.
                if (target >= 0 && target < row.getChildCount()
                        && row.getChildAt(target).findViewById(R.id.watch_card).requestFocus()) {
                    view.playSoundEffect(left ? SoundEffectConstants.NAVIGATION_LEFT
                            : SoundEffectConstants.NAVIGATION_RIGHT);
                }
                return true;
            }
            return false;
        });
        card.setOnClickListener(view -> launch(item));
        return v;
    }

    private void sizePoster(View item) {
        if (!Config.DOCK_STYLE_DARK.equals(cfg.dockStyle)) return;
        int viewport = scroll.getWidth() > 0 ? scroll.getWidth()
                : container.getResources().getDisplayMetrics().widthPixels;
        int width = posterWidth(viewport);
        int height = Math.round(width * 9f / 16f);
        View card = item.findViewById(R.id.watch_card);
        ViewGroup.LayoutParams cardParams = card.getLayoutParams();
        cardParams.width = width;
        cardParams.height = height;
        card.setLayoutParams(cardParams);
        View name = item.findViewById(R.id.name);
        ViewGroup.MarginLayoutParams nameParams = (ViewGroup.MarginLayoutParams) name.getLayoutParams();
        nameParams.width = width;
        nameParams.topMargin = height + Math.round(6 * density);
        name.setLayoutParams(nameParams);
    }

    WatchNext.Item itemForCard(View card) {
        Object tag = card.getTag();
        return tag instanceof WatchNext.Item && items.contains(tag) ? (WatchNext.Item) tag : null;
    }

    boolean remove(View card) {
        WatchNext.Item item = itemForCard(card);
        if (item == null || !WatchNextDismissals.remove(host.activity(), item)) return false;
        refresh(onChanged);
        return true;
    }

    View focusedCard() {
        for (int i = 0; i < row.getChildCount(); i++) {
            View card = row.getChildAt(i).findViewById(R.id.watch_card);
            if (card != null && card.isFocused()) return card;
        }
        return null;
    }

    /** The launcher's own tile corner: 16dp at the design width, scaled down with the card. */
    private void roundCorners(View card) {
        card.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), AppAdapter.cornerRadius(view.getWidth(), density));
            }
        });
        card.setClipToOutline(true);
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
