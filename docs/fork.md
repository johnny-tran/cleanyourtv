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
- `launcher/app/src/main/res/drawable/glass_panel.xml` and `res/values/dimens.xml` — share the popup corner radius between Classic and Glass.
- `launcher/app/src/main/res/drawable/input_row_bg.xml` — the menu row's pill corners, shared by Classic and Glass.
- `launcher/app/src/main/java/com/gathin/tvhome/AppLibraryDialog.java` — reserves the same offset-outline drawing space as Home and shares its configured popup material.
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

## Configurable popup styles

`popup_style` follows the existing `glass_style` configuration pattern: a non-translatable resource
in `launcher/app/src/main/res/values/config.xml`, overridden by the theme JSON and normalized in
`Config.java`. It accepts `classic` or `glass`, ignoring case and surrounding whitespace. Missing or
unknown values use `glass`, preserving this fork's existing popup appearance. `classic` uses the
original panel without requesting a snapshot. This setting is separate from card focus and dock material.

Home's shared popup factory, App Library and the library's app-action menu all use this selection.
The underlying blurred capture is always the Activity's Home window; a nested library action menu
does not capture the separate App Library popup window. There is no new on-screen settings row,
permission or dependency. Change `"popup_style": "classic"` or `"popup_style": "glass"` in the theme
and push it using `tools/theme.sh`, as with the other theme controls.

App Library's full-screen popup wrapper draws its 55% black backdrop immediately, independently of
the centered panel's fade-in. It replaces Android's separately animated dim-behind window flag, so
opening does not start with a lighter Home backdrop. Outside clicks dismiss the wrapper; clicks in
the panel do not. The glass snapshot still captures only Home, preserving the panel material.

`tools/manage.py` validates the enum, with parity checks in `tests/test_manage_set_enums.py`. `tests/test_shipped_config_values.py` checks shipped values;
`PopupStyleTest.java` covers normalization and fallback. `themes/README.md` documents the key and
`docs/switches.md` reuses the existing TV screenshot of the original App Library panel for Classic.

The configurable-style build passes 344 JVM tests, release lint and assembly (599,553-byte APK).
Targeted Python checks pass for enum tooling, shipped config values, switch screenshots and fork file
coverage. Installed on the Sony BRAVIA Android TV 12 after ADB authorization. The same Home app menu was
opened and captured in both Classic and Glass; spacing stayed consistent and the materials changed
as expected. The shared 19dp outer radius was also checked with the first and last app-menu rows
focused on the TV. The TV and local device overlay now select `glass`. App Library, its nested action menu
and physical remote navigation still need a live check for this setting.

## Popup material and placement

Home popup menus use a snapshot of the launcher's own window through `PixelCopy`, captured after the
panel attaches. The snapshot is one-sixth screen resolution and uses three separable box-blur passes
from `HeroArt.boxBlur`, a dark tint (`#C0121418`), 19dp corners (13dp row radius plus the 6dp inset, shared with Classic through `res/values/dimens.xml`) and a faint edge. It is captured once per
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

The preceding popup/focus/dock refinement source was built with `testReleaseUnitTest lintRelease assembleRelease`: all 342 JVM tests
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

## Project design skills

`.agents/skills/android-tv-launcher-design/` and `.agents/skills/android-tv-ui-sounds/` contain
MIT-licensed skills copied from `paisanos-io/tv-makeover`; each includes its `LICENSE`, source revision
and fork-specific path/integration guidance in `SKILL.md`. They support future design and sound work;
copying them does not change the launcher or install sound assets.

Included design helpers: `bbox.py`, `px.py`, `svg_to_vector.py`, `frames.py`, `crop_to_frame.sh`,
`measure.sh`, `fvar.py` and `references/figma-to-launcher.md`. Sound helpers: `prepare_sound.sh`,
`synth_ui_sound.sh` and `references/UiSounds.java`. Research templates remain in the ignored research
checkout; the skills explicitly caution against applying them over this fork.

## Figma dark dock and lift focus

`dock_style=dark` opts into the 1920×1080 Figma dock geometry (`DockLayout.java`) and a cached dark
background material (`DockBackdrop.java`); `classic` preserves the previous layout. Six columns target
250×150px resting cards with 30px corners, a dock from y=784 to 1014 with 60px corners, and a 26px
next-row peek. Grid margins change independently of the header. App artwork and saved order stay dynamic.

`glass_style=lift` uses the same shared tile motion for apps and Continue Watching, with a 306/250
focus scale and a cached downward shadow instead of a halo or outline. The shadow approximates the
Figma 30px offset/40px blur/35% black treatment; no live shadow filtering runs per animation frame.
`TileFocus.java` and `WatchNextRow.java` share the style. Dark dock geometry and lift focus are separate
settings; neither changes upstream defaults. `DockLayoutTest.java` covers the target geometry and scale.

The dark dock captures only background layers at reduced resolution, excluding app artwork, header and
Continue Watching. It reuses `PopupMaterial.blur`; blur equivalence and performance need real-TV checks.
Background and top-bar theme values are outside this pass. Continue Watching remains above the dock.

This dock pass passes 347 JVM tests, release lint and assembly (601,881-byte APK), plus the targeted
Python enum/config/switch/fork-map checks. Private TV captures verify first/last dock focus, lower-grid
focus and Continue Watching focus. A thresholded YouTube-card comparison gives Figma bounds
(62,807)–(367,990) and TV bounds (62,808)–(367,989), inclusive: within 1px. Artwork remains app-provided,
so the mockup's baked icon shading is not substituted for the installed apps' banners.

A short two-sequence framestats run on the new style recorded 239 frames: GPU p50/p90 6.6/15.6ms,
2 frames over 16.7ms; CPU completion p50/p90 8.2/17.6ms. This is limited injected-key evidence, not a
claim of sustained smoothness or improvement. Native remote feel and matching the blur/shadow over
the eventual photo wallpaper remain acceptance checks. No background or top-bar settings changed.

`glass_style=zoom` keeps the same 1.224× focus enlargement as `lift` but draws no shared focus overlay
(no ring, halo or shadow). This provides a clean comparison and applies to apps and Continue Watching.
The shadowed `lift` option remains available; the initial shadow-free TV preview selected `zoom`; the current preview uses card-local `lift`.

Dark dock end padding now follows the current animated scale/pivot of the first and last cards, with
clearance matching the visible top/bottom padding: 20dp at rest, about 11.6dp at full focus.
The gap interpolates with the tile scale rather than staying fixed. A focused end expands outward and returns as the tile shrinks;
vertical bounds stay unchanged. The grid's drawing viewport extends to the screen edges while keeping
the same inner cells, so expansion is not clipped. The tile animator invalidates the dock decoration
without requesting layout; intermediate-scale geometry is covered by `DockLayoutTest.java`.

In the dark dock layout, Continue Watching's title margin and row start padding are derived from the
unfocused first app card's cell position (90px on the six-column 1080p target). The existing padded
focus-scroll logic uses this new inset on return trips as well; Classic keeps its original margins.
Dark-layout Continue Watching posters scale with the viewport to 340×191px at 1080p (16:9),
keeping that left inset and the existing gaps so four full cards and part of the fifth signal horizontal scrolling.
Continue Watching handles its horizontal D-pad moves explicitly and plays the same Android navigation
sound as the app grid only after focus moves. Its Up/Down exits and the top-bar/dock entrances also
play directional system sounds. Row ends stay silent, and the TV's system sound setting remains authoritative;
there are no bundled sounds or independent volume controls.
The sound-routing build passes 351 JVM tests, release lint and assembly (604,225-byte APK).
Injected TV navigation checks cover right/left scrolling back to the first poster, the left edge,
top-bar entry/exit and returning to the dock. Audible consistency and physical-remote behavior
still require the owner's listening check.

`lift` now places its cached shadow behind each individual app/Continue Watching card through
`TileShadow.java` and the keyed tag in `res/values/ids.xml`. Selection fades/scales that card's shadow
in and fades it out in place on blur; the shared moving highlight is disabled for this style. The
shadow background is removed after its fade-out, releasing its cached mask. It does not participate in
row measurement. It reuses
`GlassView`'s existing mask renderer. `zoom` still provides enlargement without a shadow.

Card-local shadow validation: final release build/lint and 349 JVM tests pass (603,161-byte APK).
Private Sony screenshots check selected YouTube, a different app and Continue Watching without
changing layout. Pixel samples below YouTube darken when selected. A short 119-frame navigation run
on the photo wallpaper recorded GPU p50/p90 5.9/6.4ms and 0 GPU frames over 16.7ms; CPU completion
p50/p90 was 8.0/10.3ms. This is limited injected-key evidence, with physical remote feel still pending.

## Wallpaper blur below the dark dock

`WallpaperPlayer.java` prepares a small blurred copy alongside each decoded wallpaper when
`dock_style=dark`. `MainActivity.java` fades only this background in when focus leaves the dock for
lower app rows, and out on return. `res/values/ids.xml` identifies the layer so `DockBackdrop.java`'s
background-only capture excludes it and keeps using the sharp wallpaper even while hidden under the blur.
This avoids rebuilding the dock material during the fade. Tiles, focus, top bar and dock material stay unchanged.
The cached 1/12-resolution blur approximates the Figma grid's 68px blur with a 40% dark gray tint;
no blur runs per animation frame. Rotation and memory-pressure reloads replace both pictures.
Classic dock and missing wallpaper keep their existing background behavior; reduced-motion and
animation-off settings shorten or skip the transition.

Release build/lint and 351 JVM tests pass (604,133-byte APK). Private Sony captures verify blurred
lower rows and a sharp return to the dock. A short four-key transition sample recorded GPU p50/p90
5.7/15.2ms, 0 of 77 GPU frames over 16.7ms, and CPU completion p50/p90 9.4/18.3ms. The preceding
sharp-only sample was 4.5/5.7ms GPU and 7.3/9.0ms CPU across 71 frames: transitions add work, though
this small sample is not sustained-performance evidence. Physical remote feel and wallpaper rotation
remain untested in this pass.

## Dock inner stroke and pill menus

The dark dock band (`DockDecoration.java`) now draws Figma's inner stroke, which was missed in the
earlier dock pass: `#fff` at 10% (`0x1AFFFFFF`), 2px (1dp), inset by half its width so it sits inside
the band's edge and stays concentric with its 30dp corners. The non-dark band keeps its original edge.

Menu panels (`PopupMaterial.java`) now share the dock's material: the same three-pass box blur with the
dock's 41% black tint (`0x69000000`) instead of the old `0xC0121418`, and the same inner stroke in place
of the previous 0.5dp faint edge. `input_row_bg.xml` makes each menu row a full pill (`popup_row_radius`
21dp = the 42dp row height / 2), and the panel radius follows it concentrically (`popup_corner_radius`
27dp = 21dp row radius + the panel's 6dp inset), both in `res/values/dimens.xml`. This applies to the
Home popup factory, App Library and its action menu, which all share the panel background and rows.
`glass_panel.xml` also backs the quick-settings panel and the module-card prompt, which keep their own
padding, so the shared radius change reaches them too.

Verified on the Sony BRAVIA (Android TV 12) with `dock_style=dark`, `glass_style=lift` and
`popup_style=glass`: pixel scans of a Home capture show the band's 2px lighter top edge and the panel's
inner stroke; the tile menu shows pill rows inside a concentric panel.
