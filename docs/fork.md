# This checkout is a fork

This repository is a **personal fork** of [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv).
It keeps the whole CleanYourTV launcher and adds one feature: a **Continue watching** (Watch Next) row on
Home, above the dock, ported from [dhruv2003/sony-tv-launcher](https://github.com/dhruv2003/sony-tv-launcher)
(MIT). Everything else, and every future upstream feature, comes from upstream.

Coding agents: read this page before changing `launcher/`, and follow it when updating from upstream.

## Remotes and branches

- `upstream` = `Gathin23/cleanyourtv` (the original). Nothing is pushed to it.
- `origin` = the fork (`johnny-tran/cleanyourtv` on this checkout). `main` and `continue-watching` are here.
- The feature lives on branch **`continue-watching`**, as one commit. **`main` is a clean copy of
  `upstream/main`** with no local changes, so a pull is always a fast-forward and the feature is a single
  commit to rebase.

## Update the fork (every upstream update)

```sh
git checkout main
git fetch upstream && git merge --ff-only upstream/main     # fast-forward, no merge commit
git checkout continue-watching
git rebase main                                             # replay the one feature commit
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
- `launcher/app/src/main/java/com/gathin/tvhome/WatchNext.java` — the Watch Next provider query.
- `launcher/app/src/main/java/com/gathin/tvhome/PosterLoader.java` — poster download and cache.
- `launcher/app/src/main/java/com/gathin/tvhome/WatchNextRow.java` — the row: its view, render and focus.
- `launcher/app/src/main/res/layout/item_watch.xml` — one watch card.

Edited files, each marked with a `continue watching (fork)` comment where possible:
- `launcher/app/src/main/AndroidManifest.xml` — the `READ_TV_LISTINGS` and `INTERNET` permissions.
- `launcher/app/src/main/java/com/gathin/tvhome/Config.java` and `res/values/config.xml` — the `watch_next`
  boolean, off by default.
- `launcher/app/src/main/java/com/gathin/tvhome/MainActivity.java` — build the row, refresh it in
  `onResume`, lay it above the dock, route the D-pad in and out.
- `launcher/app/src/main/res/layout/activity_main.xml` — the row's container.
- `launcher/app/src/main/res/values/strings.xml`, `styles.xml` — the header string and its `Caption` style.
- `themes/README.md`, `docs/switches.md` — the `watch_next` key's rows, which the repository's own tests
  require of any new key.

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
