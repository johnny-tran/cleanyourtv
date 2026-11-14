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
- `launcher/app/src/main/java/com/gathin/tvhome/SidebarButtonMaterial.java` — cached sidebar button fills, focused blend and shared inset lighting from Figma.
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
- `launcher/app/src/main/res/values-iw/strings.xml` — Hebrew labels for the sidebar, Watch Next layouts and cursor sound control.
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
The fixed-backdrop build passes 351 JVM tests, release lint and assembly (604,305-byte APK).
Private Sony captures check stable outside-panel pixels before/after the first arrow, an inside
header click leaving the library open, outside-click dismissal, and Back dismissal. The opening's
asynchronous catalogue load still precedes the first popup frame; physical-remote acceptance is pending.

The App Library catalogue viewport extends through the panel's horizontal padding and clips at its
rounded outer edge. The same inset is transferred to the list's padding, preserving resting card
positions, focus scroll targets and the header's spacing while allowing partial cards to reach both edges.
Child drawing retains the old vertical padding clip and uses the panel's shared corner radius for
its outer outline. Release build/lint and 351 JVM tests pass (604,485-byte APK). Private Sony captures
check the unchanged first-card position and partial cards at both outer edges after scrolling.
The initial install hit Android's low-storage limit; `pm trim-caches 512M` freed temporary cache space
and the update installed. Physical remote acceptance and RTL appearance remain unchecked.

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

## Compact header and sidebar

`sidebar_header=true` opts into the Figma greeting, clock/avatar pill and three-action sidebar
(Inputs, App Library, Settings). The default is false. `HomeHeader.java` builds the views, keeps
D-pad focus within the sidebar, restores the pill on dismissal, and routes to the existing actions.
Holding the pill or Settings opens the existing launcher menu. The old header retains its measured
space so the dock and Continue Watching geometry stay unchanged. `MainActivity.java`, `Config.java`,
`res/values/config.xml`, `themes/README.md` and `docs/switches.md` wire and document the switch.
`PopupMaterial.java` accepts a caller-supplied radius so the pill and sidebar share its cached blur
without changing other menus. Private prototype SF fonts, symbol PNGs and the avatar photo live in
`header-assets/` beside the TV config, never in Git or the APK. Missing files use bundled fonts/icons.
Replace `header-assets/avatar.png` with a square photo and reload the launcher to update both avatar
views. They center-crop the source and clip to a 25dp circle (50px at the 1080p design density);
the image needs no baked mask. The closed and open 32dp layout boxes retain centered 3.5dp insets,
and the morph scales by visible content size so the photo stays the same size throughout.

The sidebar build passes 352 JVM tests, release lint (zero errors) and release assembly.
Targeted Python checks cover shipped config values, switch documentation and this fork map.
On the Sony Android TV 12, injected D-pad checks open each action, traverse all sidebar rows,
stop at the edges and return from App Library to the clock pill. The clock pill remains in
Home's focus tree while transparent behind the sidebar, avoiding focus moving to posters.
Private screenshots verify the circular avatar, sidebar bounds and unchanged dock geometry.
Physical remote acceptance, accessibility-reader output and a performance benchmark remain unchecked.

The sidebar morphs from the focused clock/avatar pill over 420ms. Its surface bounds and corner
radius interpolate, the clock translates left, the profile name reveals from behind the avatar,
and the clipped menu rows fade in as the surface grows vertically. The avatar's center is anchored
to the closed pill; text is never scaled. Back/outside clicks reverse the same geometry, and menu
actions wait until closing completes before opening the existing destination. Reduced motion or
animations disabled uses the final state immediately. `HomeHeaderTest.java` covers traversal and
bounded interpolation; `PopupMaterial.java` updates its radius without another backdrop capture.
The morph build passes 353 JVM tests, release lint and assembly (609,961-byte APK). Installed on
Sony Android TV 12; injected navigation verifies opening, reverse closing, App Library after closing,
and focus restoration. Private `work/sidebar-morph-demo.mp4` records two cycles and all three focus
states. Sequentially decoded video frames confirm the shrinking surface and anchored avatar during
closing; sparse frame-seeking previews were unreliable. TV returned to standby after capture.
Physical remote feel, accessibility-reader output and performance benchmarking remain unchecked.

The clock-to-sidebar handoff keeps the focused pill visible until the morph surface is initialized,
then swaps both in the same frame. Its cached glass backdrop sits behind a native focus/activated
state selector, so a late capture cannot replace the focused white fill. Closing redraws the matching
pill underneath before removing the popup window. This avoids exposing wallpaper between surfaces;
it is not a pressed-state color effect. Sidebar backdrop capture waits for the pill's hidden frame.
Handoff validation: release build/lint and 353 JVM tests pass (610,673-byte APK). The earlier TV video
had five decoded frames without the avatar across two openings. The corrected two-cycle recording
has zero missing-avatar frames across all 86 decoded frames, including closing. Private video and
assert-based continuity checker remain in `work/`; TV standby was confirmed after recording.

The focus-white fill and clock ink reach the dark-glass/white-text state in the first 90ms of
the 420ms opening; closing mirrors that fade in its final 90ms. `HomeHeader.materialProgress` shares
a smooth material phase between fill and ink, independent of geometry easing. Interrupted transitions
retain their current color and shorten the fade to fit. There is no new setting or change to expansion timing.
Faster-material validation: release build/lint and 354 JVM tests pass (610,681-byte APK).
Installed on Sony Android TV 12 and recorded two open/close cycles in private
`work/sidebar-fast-material.mp4`; sequential video frames show dark material before expansion
finishes. The continuity checker reports zero missing-avatar frames across all 80 decoded frames.
TV standby confirmed after recording; physical-remote feel remains unchecked.

The clock pill retains its cached blurred backdrop and inner stroke on focus; only its tint changes
from 41% black to 80% white, with black clock ink. `PopupMaterial.java` opts just this material into
focused/activated tint states; other popup materials keep their existing dark tint. The sidebar morph
interpolates that same tint and clock ink during the existing symmetric 90ms material phase, rather
than covering the blur and stroke with an opaque white foreground. Classic/capture-failure fallbacks
use the matching translucent white fill with their existing shape and stroke.

Sidebar buttons now share Figma BG's fill/edge/shadow stack instead of switching between flat fills.
`SidebarButtonMaterial.java` caches both states from the sidebar's existing Home blur: additive
`#101010`, a 4% neutral luminosity lift, and three inset lighting masks (design offsets ±40px,
blur radii 10/30px, spread -40px). Focus adds `#6F6F6F` Color Dodge and 65% white Screen below
that same lighting. Fine burn edges and the 2% drop shadow remain in both states. `HomeHeader.java`
installs one stateful background per button; `PopupMaterial.java` samples the final row positions
from its existing snapshot. Focus moves only select cached bitmaps, with no new capture or blur.
The native Figma GLASS refraction/dispersion and its Gaussian masks are approximated with the cached
blur and smooth distance masks; this is not an exact implementation of Figma's optical shader.
The 4% white luminosity blend uses a neutral channel lift. Dock, popovers, clock-pill styling,
geometry and navigation are unchanged.

Button-depth validation: 359 JVM tests, release lint and assembly pass. Targeted fork-map checks
and `git diff --check` pass. A private Sony Android TV 12 capture verifies focused Inputs alongside
unfocused App Library/Settings with shared inset shading. The TV returned to standby afterward.
Optical refraction parity, physical-remote acceptance and performance benchmarking remain unchecked.

## Sidebar Glass container trial and tighter spacing

The Sidebar (Glass) Figma frame `12435:32762` tightens `HomeHeader.java`: equal top/right
13.5dp margins, User Profile vertical padding 11dp, Sidebar Items top padding 0 and bottom 6.5dp.
Width, horizontal padding, button sizes/gaps and corner radius stay unchanged. The 5dp downward
container move cancels the 5dp reduction in profile top padding, retaining the avatar's vertical
center and the existing anchored open/close morph.

The container trial opts only its `PopupMaterial.java` instance into cached inset top/bottom light,
broad top shade, a fine burn edge and a 4% soft outer shadow. It reuses
`SidebarButtonMaterial.insetCoverage` with the container's 24dp corner instead of a pill radius.
Lighting appears with the symmetric material fade and the focused closed pill stays unchanged.
Buttons sample this shaded container behind their own glass layers. The shared Home blur and
41% black tint stay in place; Figma's luminosity blend/refraction remain approximations.
The extra lighting/shadow masks are cached once per opening, never blurred per animation frame.
Dock and other popup materials are unaffected. The glass trial is independent of the spacing edits,
so reverting the material does not require reverting the tighter layout.

Container-trial validation: release build/lint and 360 JVM tests pass. On Sony Android TV 12,
private captures check clock focus, focused Inputs/App Library, tighter container bounds and reverse
close back to the same clock pill. The launcher process stays unchanged across that sequence and
standby is confirmed afterward. The first native check caught an immutable-lighting-bitmap crash;
the lighting bitmap is now mutable. The outer shadow contains no opaque face and uses current
surface bounds before its first draw, avoiding a first-opening shadow artifact. Refraction parity,
physical remote feel and performance benchmarking remain unchecked.

The updated Avatar Menu uses a 32dp avatar box with a 25dp photo (64/50px at the design density)
and 0.5dp outer pill padding at top/right/bottom. The photo thus has 4dp visible clearance to the
pill edge, matching Figma. The old closed box was 29dp with 2dp outer padding: changing both together
preserves the avatar center. The sidebar uses the same 25dp visible circle; the morph uses content
size for its scale ratio, so opening does not shrink or enlarge the photo. The Sidebar (Glass)
profile padding and overall container spacing stay unchanged. The updated closed-control geometry
was read through Figma Console after the official MCP hit its plan limit.

### Tile radiosity and one private font

`TileShadow.java` now installs a cached, half-resolution neutral-gray radiosity layer at rest for
`lift` tiles, both apps (`AppAdapter.java`) and Continue Watching (`WatchNextRow.java`). It uses the
owner-confirmed Figma Radiosity values: #3A3A3C at 50% opacity, 11px downwards offset and 27px layer blur
at a 250px tile width. The blurred layer has the same width and height as the card, with measurements
scaled with the tile. It fades into the existing focused shadow. No blur runs during navigation;
empty slots draw neither shadow. These supplied values replace the earlier estimated neutral-gray fill.
`TileShadowTest.java`
checks the Figma-to-Skia blur conversion and size scaling. The prior focused-only mask cleanup above
is superseded: this background remains installed to draw the resting radiosity.

`UiFont.java` caches the private `header-assets/sf_medium.ttf` and applies it to text without altering
sizes, colors or spacing. `HomeHeader.java` uses it for greeting, clocks and sidebar labels, dropping
the separate bold and Rounded files. XML text is covered at inflation in `MainActivity.java`,
`AppAdapter.java`, `WatchNextRow.java`, `AppLibraryDialog.java` and `QuickSettingsActivity.java`, plus
`LauncherSettingsActivity.java` and both screensavers through `ClockScene.java`. Programmatic text
uses the same `UiFont.get` path. Continue Watching's heading retains its size and color. Without the
private font file, bundled theme/locale typography is preserved. Font changes take effect on process
restart, and no proprietary font is included in the repository or APK.

Validation: 361 JVM tests, release lint and assembly pass (614,445-byte APK); fork documentation
checks and whitespace checks pass. Installed on the Sony Android TV 12; private screenshots cover
resting/selected dock tiles, Continue Watching, clock focus and sidebar. Injected navigation preserved
the launcher process, and the TV was returned to sleep. Physical-remote acceptance and performance benchmarking remain unverified. The owner subsequently
supplied the exact Radiosity color, opacity, dimensions, offset and blur used above.

`styles.xml` gives the Continue watching caption normal (0em) tracking to match the greeting and
removes forced uppercase, using the existing sentence-case string. Its size and color stay unchanged.

The caption now also matches the greeting at 15sp, with white opacity increased from approximately
55% to 70% (`#B3FFFFFF`). Its sentence case, font and normal tracking are unchanged.

The visible English shelf heading is now “Watch Next” (`strings.xml`, existing `continue_watching` key).
This is a label change only; data sources, sorting, dismissal actions and card layout are unchanged.

### Watch Next layout variants

The approved large-card design keeps 16:9 art, two or three complete cards plus a partial next card,
10% focus enlargement, a bottom gradient, show title and installed service name. On focus, the title
rises and a single-line `S2 E4 · Episode title` fades into the gap; long text ellipsizes at the end.
Missing episode data leaves the two-line layout, and missing service apps omit the service label.
Compact preserves the earlier card sizing, labels and focus motion. No recommendations are mixed in.

`Config.java` and `config.xml` add `watch_next_layout` (`compact`, `two`, `three`), with Compact as the
safe bundled default. The owner's TV selects Two large cards. Theme JSON and `tools/manage.py` accept
the same values. The existing preference/revision/theme-reset flow carries the on-TV override.
`LauncherSettingsActivity.java`, `activity_launcher_settings.xml` and `strings.xml` expose the three
choices without changing dock columns. `themes/README.md` and `docs/switches.md` document the control.

`WatchNextRow.java`, `item_watch.xml`, `watch_scrim.xml` and `styles.xml` render the overlays, retain
reserved space above the dock, and preserve launch, navigation sounds and long-press removal.
`WatchNext.java` formats episode metadata; `WatchNextRowTest.java` checks missing fields, dot spacing,
layout sizing/peek and invalid choices. `TileFocus.java` accepts an explicit scale while keeping the
existing shared motion/shadow renderer; `MainActivity.java` fits the shared highlight to that scale.
Artwork remains provider-supplied. The agreed written behavior and reviewed Figma frames are the
reference; Figma API rate limits prevented an exact layer/spec comparison for this implementation.

Large-card validation: release lint/assembly and 364 JVM tests pass (617,849-byte APK), plus 38
focused Python checks for enum handling, shipped config values, settings docs and fork tracking.
Installed on the Sony Android TV 12. Injected D-pad checks switch through Three, Compact and Two,
return to Home, traverse cards and restore the first-card margin. Screenshots cover all three modes
at rest and with one card focused; the three-card state visibly ellipsizes a long episode title.
Two large cards remains selected. A touch-injected settings selection initially left the dock
unparked until Home; the actual D-pad settings path returned correctly without that workaround.
The native injected long-press was too short for the existing 550ms hold handler and opened playback;
Home was restored immediately. Physical-remote hold-menu acceptance therefore remains pending.

The same short injected navigation sample before/after collected 119 GPU frames each: p50/p90
4.7/6.7ms before and 6.4/8.4ms after, with 0/119 over 16.7ms in either sample. This is a small local
regression sample, not a general device-performance claim. No recommendations, launch permissions,
Home helpers, dock settings or streaming packages were changed. TV returned to sleep.

### Sidebar opening work off the UI thread

`PopupMaterial.java` now runs captured-backdrop blur and sidebar lighting masks on one shared
`TVHomeGlass` worker. `SidebarButtonMaterial.java` uses that same worker for its normal/focused
bitmaps, with a private drawing paint per bake. `HomeHeader.java` samples the buttons only after the
container lighting is ready. View coordinates, geometry, state changes and final bitmap publication
stay on the UI thread; detached panels and superseded button sizes discard unfinished results.
The glass formulas, resolution, 420ms morph and symmetric 90ms material phase are unchanged.
Other glass popovers also use the background blur worker. `PopupRenderTest.java` checks that material
jobs leave their caller and use one worker rather than starting a thread per bitmap.

Opening-performance validation: release build, zero-error lint and 365 JVM tests pass (618,669-byte
APK), including the worker check. Five identical injected sidebar open/close traces on Sony Android
TV 12 recorded median per-run p90 frame time 44.34ms before versus 13.84ms after warm-up. The median
worst main-thread draw fell from 66.96ms to 5.61ms; median worst frame from 188.90ms to 40.99ms.
Frames over 33.4ms per run fell from five to one. All five measured runs are retained in ignored
`work/`, along with a representative peak-draw-median flame chart. A separate immediate-after-install
batch was noisy (p90 16.03–147.96ms across its five runs); it is retained too. These are short warm
sidebar samples, not a cold-start or whole-launcher performance guarantee. Physical remote feel
still needs the owner's check.
TV checks confirm the settled glass appearance, all three focused sidebar rows, an early close
100ms after opening, a subsequent reopen, and the App Library action returning focus to the clock.
No launcher crashes were logged. Home remains on screen; the TV was already awake when testing began.

The owner’s latest menu-pill trial lowers its focused white tint to 60% (`0x99FFFFFF`),
including activated/fallback states and both sidebar morph endpoints. Blur, stroke, button
lighting and animation timing are unchanged. `HomeHeaderTest.java` uses the matching tint.

### Stable sidebar button fills during expansion

`HomeHeader.java` waits for all three final button materials before revealing the row group.
The buttons then remain fully opaque: the existing growing/shrinking container clip reveals them,
so the focused fill no longer fades through gray. `SidebarButtonMaterial.java` reports completion
only after publishing the current-size normal/focused images, skips the unused initial placeholder
bake, and prepares hidden buttons from their measured view sizes. Its inset-distance calculation
uses the equivalent square root for screen-sized coordinates instead of the slower robust hypotenuse.
`PopupMaterial.java` supplies an explicit Classic/capture-failure fallback, keeping buttons available
when glass cannot be captured. Superseded/detached jobs still discard their results. The 60% focused
clock tint, final button styling, expansion geometry and material timing are unchanged.
`HomeHeaderTest.java` checks hidden-until-ready and full-opacity-throughout-expansion behavior.
The container/button lighting shapes are prewarmed on the material worker when the header is created.
`SidebarButtonMaterial.java` retains only two geometry-keyed masks (about 1.3MB combined at 1080p),
shared by all three button bakes and the container, avoiding repeated inset-distance calculations
while opening. A changed size, corner or density gets a new mask; old geometries are evicted.
`SidebarButtonMaterialTest.java` covers reuse, geometry keys and bounded retention.
The first-opening recording exposed a late material arrival even with cached masks. Expansion now
starts only after all final button images are published, keeping the clock-shaped header in place
during preparation. This trades a short preparation wait before opening for stable fills throughout
the morph; no empty expanded container or replacement fill is shown. Reduced motion takes the final
state once ready, and Back can still dismiss the pending opening. Closing keeps the existing timing.
Validation: release build, zero-error lint and 367 JVM tests pass (619,557-byte APK), plus fork-map
and whitespace checks. Installed on Sony Android TV 12. A frame-by-frame native recording confirms
stable full-opacity button fills as the container reveals them, with no placeholder replacement or
empty expanded panel. The recorded first opening waits roughly 0.4s in its clock-shaped state before
expanding; this preparation latency remains a known tradeoff, not a performance improvement claim.
Injected checks cover Back during preparation, reopening, all rows and App Library return; no launcher
crashes were logged. Home is left visible. Physical-remote feel and forced capture-failure testing
remain unverified.

### Watch Next typography from the live Figma frames

The host Desktop Bridge supplied the exact two- and three-card text specifications after the cloud
API quota block. `WatchNextRow.java` now uses 28px/22px title/service text with 33px/26px line boxes
for Two, and 26px/20px text with 120% line boxes for Three, at the 1920px design width. Both use
SF Pro Medium, -0.2px tracking and a 2px text-stack gap. The existing 10% card focus scale also scales
the text. `item_watch.xml` uses the service fill #EBEBF5 at 30% opacity. Text and progress have 40px
side insets; the progress bottom inset is 32px for Two and 24px for Three, with 24px/16px between
the service line and progress. Progress fill measures the inset track width rather than the full card.
The focus-only episode line keeps its earlier color and behavior, takes the service text metrics,
and moves the title by its line height plus the shared gap. Those episode details are not specified
in the current Figma frames. Compact retains its original presentation.
Validation: release assembly, zero-error lint and all 367 JVM tests pass (620,017-byte APK), plus
the fork documentation checks. Installed on Sony Android TV 12; screenshots verify Two and Three
at rest and focused, including the episode reveal. Three was selected on arrival and restored after
testing. Physical-remote feel and enlarged accessibility font settings were not checked in this pass.

### Refined Watch Next gradients and progress

The updated Two and Three Figma cards limit the scrim to the bottom half of the poster, with a
transparent-to-60%-black ramp ending at 80% of that half, then remaining constant to the bottom.
`watch_scrim.xml` defines the ramp; `WatchNextRow.java` sizes and bottom-aligns it. Two retains 40px
side padding, an 8px progress bar, 32px bottom inset and 24px service-to-progress gap. Three uses
32px side padding, a 6px progress bar, 24px bottom inset and 16px service-to-progress gap. These are
1920px design values, converted to dp. The track is white at 30%, the fill is #D9D9D9, and both ends
are rounded to half the bar height. Existing per-layout typography and 10% focus scale remain.
Compact deliberately retains its earlier external-label/edge-progress presentation.

### Sidebar sizing from the updated focus variants

`HomeHeader.java` matches all three revised Figma states with a 440x413px panel, 416x84px buttons,
20px row gaps, 12px side/13px bottom padding around the list, and the existing 108px profile area.
Button text is 30px SF Pro Medium, icons occupy 48px boxes, icon/text gap is 16px, and row left
padding is 21px. The profile name is now 25px like the clock; the 50px avatar, profile insets and
avatar screen anchor remain unchanged. Worker prewarming uses the new geometry. `strings.xml`
adds sidebar-only App Library and System Settings labels, leaving other Settings labels alone.
Normal/focused symbols were exported directly from the corresponding Figma text layers to private
`header-assets` PNGs, including the new four-square App Library icon. The prototype assets remain
outside the APK and repository, with the existing bundled icon fallbacks if absent. Material formulas,
focus fills, opening/closing timing and actions are unchanged.
Validation: release assembly, zero-error lint, all 367 JVM tests, fork-map checks and whitespace
checks pass (620,485-byte APK). Installed on Sony Android TV 12. TV captures verify both large-card
layouts, their focus episode reveal, and all three sidebar focus states. UI hierarchy assertions
confirm exact panel/button bounds and spacing; all six icon exports retain their 48px design boxes
at 2x resolution. Three-card layout is restored. The TV entered sleep during the final App Library
action check, so that action and performance sampling were not completed in this pass; no changes
to those action handlers were made. Sleep was explicitly requested and confirmed with
`mWakefulness=Asleep`. Physical-remote feel and enlarged accessibility fonts remain unverified.

The large-layout Watch Next heading has its own 32px start inset from the resting card edge, as in
both Figma frames. `WatchNextRow.java` applies that as text padding so the existing dock-aligned
row margin and scroll origin remain intact. Card content insets and Compact are unchanged.
Validation: release build, zero-error lint and all 367 JVM tests pass (620,493-byte APK). Installed
and checked in the resting three-card TV screenshot; the first card starts at x90 and heading text
has the intended 32px inset. TV returned to confirmed sleep. Two uses the same heading padding;
it was not separately captured again for this padding-only correction.
