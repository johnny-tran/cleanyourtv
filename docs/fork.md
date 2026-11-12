# This checkout is a fork

This repository is a **personal fork** of [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv).
It keeps the CleanYourTV launcher and adds a **Continue watching**
(Watch Next) row on Home, above the dock, ported from
[dhruv2003/sony-tv-launcher](https://github.com/dhruv2003/sony-tv-launcher) (MIT), and a `greeting_text`
key that replaces the time-of-day greeting, plus the focus, dock and popup refinements documented below.
Other launcher features come from upstream.

Coding agents: read this page before changing `launcher/`, and follow it when updating from upstream.

## Keep this page current (required)

Every file you change that upstream does not have must be recorded here. Add it to **What the feature
touches** below and, when you add a key or a feature, a short section describing it. Keeping this page
current is what keeps the next upstream rebase conflict-free: it is the fork's map of its own divergence.

`tests/test_fork_docs_track_changes.py` checks it: it diffs this branch against `upstream/main` and fails
until every changed file is named here. Screenshots, this page and the fork's own JVM tests
(`launcher/app/src/test/`) are exempt, so you never have to list a `.jpg` or a test one by one.

## Remotes and branches

- `upstream` = `Gathin23/cleanyourtv` (the original). Nothing is pushed to it.
- `origin` = the fork (`johnny-tran/cleanyourtv` on this checkout). `main` and `continue-watching` are here.
- The feature and its UI fixes live on branch **`continue-watching`**. **`main` is a clean copy of
  `upstream/main`** with no local changes, so a pull is always a fast-forward and the fork commits can be rebased onto it.

## Update the fork (every upstream update)

```sh
git checkout main
git fetch upstream && git merge --ff-only upstream/main     # fast-forward, no merge commit
git checkout continue-watching
git rebase main                                             # replay the fork commits
git push --force-with-lease origin continue-watching        # the rebase rewrote the branch
tools/docker-build.sh launcher testReleaseUnitTest lintRelease assembleRelease
SKIP_BUILD=1 TV_HOST=<tv ip> launcher/build-and-install.sh
adb -s <tv ip>:5555 shell pm grant com.gathin.tvhome android.permission.READ_TV_LISTINGS
```

The last line matters: `READ_TV_LISTINGS` is a runtime permission and a sideload does not grant it, so the
Continue watching row is empty until it is granted after each install.

A rebase conflict is expected **only** in the files the feature touches (the short list below). Everything
else comes from upstream untouched, so all of upstream's new features arrive with the merge.

## What the feature touches (this is the rebase surface; keep it short)

New files (cannot conflict):
- `launcher/app/src/main/java/com/gathin/tvhome/PopupMaterial.java` — a one-shot blurred Home backdrop with dark tint for popup menus.
- `launcher/app/src/main/java/com/gathin/tvhome/PopupPlacement.java` — keeps dropdown menus at least 24dp inside the viewport while preserving anchor alignment when it fits; accounts for focus zoom to keep a consistent 10dp tile-to-menu gap.
- `launcher/app/src/main/java/com/gathin/tvhome/WatchNext.java` — the Watch Next provider query.
- `launcher/app/src/main/java/com/gathin/tvhome/PosterLoader.java` — poster download and cache.
- `launcher/app/src/main/java/com/gathin/tvhome/WatchNextDismissals.java` — local removals, restored by newer published viewing activity or manually.
- `launcher/app/src/main/java/com/gathin/tvhome/WatchNextRow.java` — the row: its view, render and focus.
- `launcher/app/src/main/java/com/gathin/tvhome/WatchNextScrollView.java` — focus scrolling that preserves the row's margins.
- `launcher/app/src/main/java/com/gathin/tvhome/TileFocus.java` — shared app/poster focus motion and colour rules.
- `launcher/app/src/main/java/com/gathin/tvhome/TopBarNavigation.java` — keeps horizontal icon navigation out of the poster row.
- `launcher/app/src/main/res/layout/item_watch.xml` — one watch card.

Edited files, each marked with a `continue watching (fork)` comment where possible:
- `launcher/app/src/main/java/com/gathin/tvhome/AppLibraryDialog.java` — reserves the same offset-outline drawing space as Home.
- `launcher/app/src/main/java/com/gathin/tvhome/DockDecoration.java` — even-padding geometry reserves equal clearance around focused bounds and derives concentric corners.
- `launcher/app/src/main/java/com/gathin/tvhome/HeroArt.java` — shares its existing box-blur pass with popup materials.
- `AGENTS.md` — the one-line pointer to this page, so a coding agent finds it.
- `launcher/app/src/main/AndroidManifest.xml` — the `READ_TV_LISTINGS` and `INTERNET` permissions.
- `launcher/app/src/main/java/com/gathin/tvhome/Config.java` and `res/values/config.xml` — the `watch_next`
  boolean, off by default.
- `launcher/app/src/main/java/com/gathin/tvhome/Config.java`, `res/values/config.xml`,
  `MainActivity.updateGreeting()` and `res/values/strings.xml` — the `greeting_text` key (see below).
- `launcher/app/src/main/java/com/gathin/tvhome/AppAdapter.java` — delegates focus motion to `TileFocus`; shares corner and artwork colour rules with posters.
- `launcher/app/src/main/res/layout/item_app.xml` — shares the `TileName` style with poster labels.
- `launcher/app/src/main/java/com/gathin/tvhome/GlassView.java` — morphs the shared highlight between poster and app tile sizes; offsets its bright outline and glow outside the artwork with a 1.5dp clear gap.
- `launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java` — build the row, refresh it in
  `onResume`, lay it above the dock, route the D-pad in and out; park the compact dock with a 16dp next-row peek.
- `launcher/app/src/main/res/layout/activity_main.xml` — the row's container.
- `launcher/app/src/main/res/values/strings.xml`, `styles.xml` — the header string, its `Caption` style and shared `TileName` style.
- `themes/README.md`, `docs/switches.md` — the `watch_next` key's rows, which the repository's own tests
  require of any new key.
- `docs/launcher.md`, `tests/test_settings_menu_docs.py` — document and check the conditional Continue Watching restore menu action.

## Deliberate differences from upstream

- **`INTERNET` on Home.** Upstream forbids it; the fork needs it to fetch poster art, since most Watch Next
  posters are `https://`. The launcher still makes no request other than the poster images, and the feature
  is off unless `watch_next` is set.
- **The 576 KB APK gate.** `.github/workflows/ci.yml` and `tests/test_translation_recipe_gate.py` gate the
  launcher APK under 589,824 B. The feature adds code and a network stack, so the fork's APK is over it.
  That gate is upstream's, for 1 GB TVs; the fork is not gated on it. Do not lower it in a pull request
  upstream; leave it as is in the fork.

## The fork owner's device setup

The settings for the TV this fork runs on (dock order, hidden apps, `columns`, the glass look) live in a
**pushed theme overlay** (`tools/theme.sh <folder>`), not in `config.xml`. That is deliberate: it keeps the
committed fork diff equal to the feature, so an upstream merge never conflicts on the device's own settings.

## Rollback

A clean, pre-fork CleanYourTV 3.4 APK and the pre-switch Home and disabled-package list are kept on the
fork owner's machine under `work/backup-cleanyourtv/` (`work/` is git-ignored and is not in a clone).
Restore with `cmd package set-home-activity --user 0 com.gathin.tvhome/.MainActivity` after reinstalling
that APK.

## Removing Continue watching cards

Hold OK on a Continue watching card to open the same glass menu as app tiles, then choose **Remove from
Continue Watching**. This stores only its package/content identity and last engagement timestamp in the
launcher's private preferences; it does not delete the streaming app's shared entry or viewing history.
The card returns when the app publishes a newer engagement timestamp. Apps that do not share updated
Watch Next data cannot trigger this automatically. Hold the top-right gear and choose **Restore Continue
Watching** to clear local removals for all titles still available in the shared feed.

## Replacing the greeting

`greeting_text` (a theme key, or `config.xml`) replaces the time-of-day greeting above the clock
("Good evening") with the given text; `greeting_name` is still appended after it. Empty keeps the
time-of-day text. It is a fork addition: a stock CleanYourTV build ignores the key. Rows live in
`themes/README.md` and `docs/switches.md`, which the repository's own tests require of any new key.

## Popup material and placement

Home popup menus use a snapshot of the launcher's own window through `PixelCopy`, captured after the
panel attaches. The snapshot is one-sixth screen resolution and uses three separable box-blur passes
from `HeroArt.boxBlur`, a dark tint (`#C0121418`), 20dp corners and a faint edge. It is captured once per
opening; the backdrop is frozen while the menu is open. Capture failure leaves the existing opaque
panel background in place, and dismissal releases the captured backdrop. No dependency or permission
was added for this effect. This is an Android approximation of Apple's dark materials, not Apple's
private renderer or exact constants.

Dropdowns retain their anchor alignment when it fits, otherwise shift inside a 24dp horizontal viewport
margin. Their width is capped to that available space. Below-card menus account for focus zoom so the
visible tile-to-menu gap is consistently 10dp for app and Continue Watching cards. The separate
above-card placement path does not use the dropdown's horizontal clamp.

References: [Apple dark blur](https://developer.apple.com/documentation/uikit/uiblureffect/style/extradark),
[Apple focus code](https://developer.apple.com/videos/play/wwdc2023/10162/) and
[Android window blur](https://source.android.com/docs/core/display/window-blurs).

## Focus outline and dock layout

- `GlassView` keeps the stock focus motion and existing `glass`, `rim` and `halo` styles. Its outline
  and glow sit outside the artwork, leaving a 1.5dp clear gap before the outline's inner edge. This
  separates the white outline from white artwork without painting a dark inner ring.
- Home and App Library reserve the renderer's actual drawing inset, keeping the offset outline from
  clipping. App Library still uses its existing default glass style; the Home theme's `glass_style`
  selection applies to Home's app and Continue Watching highlight.
- `dock_even_padding` reserves clearance around the maximum focused tile bounds, with concentric
  corners based on the enlarged tile radius. The dock does not resize as focus moves.
- In a vertical layout with `dock_even_padding`, hidden dock labels no longer reserve empty height.
  Labels in lower rows keep their existing space, preserving their descenders and focus animation.
- With a dock, `dock_even_padding`, hidden resting labels, `single_row=false` and `rows_below_dock=0`,
  the parked layout leaves a 16dp slice of the next row visible at the bottom. Existing shelf clearance
  remains a lower bound on short screens. Continue Watching follows the dock's position.
- `dock_glass` retains its original faint translucent gradient. The experimental milky dock backdrop
  and floating-shadow focus style were reverted; neither is part of the final change.

## Current device overlay (not committed)

The current local theme uses six columns, `glass_style=halo`, `dock_glass=true`,
`dock_even_padding=true`, `single_row=false`, `rows_below_dock=0`, `always_show_names=false`,
`app_glow=false` and `ambient_glow=false`. The network button and date are hidden; the top-right
controls use icons. These are device choices, not new defaults in `config.xml`. The theme overlay,
saved app order, screenshots and device backups remain private and outside the commit.

## Validation of the UI refinements

The final source was built with `testReleaseUnitTest lintRelease assembleRelease`: all 342 JVM tests
passed, lint passed, and the release APK was 598,977 bytes. This remains above upstream's 576 KB gate
as described above; that gate was not changed. Added coverage checks dropdown margins/alignment,
scaled-card menu gaps and clearance around focused dock tiles.

Installed and inspected on a Sony BRAVIA 4K VH2 running Android TV 12. Private TV screenshots verified
popup materials, input dropdown placement, offset outlines, the restored dark dock, six columns and
the final 16dp peek. An injected Down/Up check restored the parked layout at the preceding 24dp
setting; physical remote navigation was not retested after the final spacing/material adjustments.
No performance benchmark was run, and this is not a claim of performance improvement. The complete
Python suite is not a passing release check: an earlier full run had failures/errors; use the targeted
fork-documentation check for this documentation update.
