"""docs/fork.md names every file this fork changes, so the next upstream rebase is not a surprise.

This branch is a personal fork of `upstream/main`. Its own divergence is the whole rebase surface: a
file changed here that upstream also changes is a conflict waiting to happen, and the fork guide
(`docs/fork.md`) is the map of that surface. An agent that adds a feature and forgets the map turns the
next `git rebase upstream/main` into a blind merge.

The check diffs this branch against `upstream/main` (committed and working-tree changes, untracked
files included) and fails until each changed path is named in `docs/fork.md`. A path counts as named
when its file name appears on the page, which is how the page already lists them (`WatchNext.java`,
`MainActivity.java`). Screenshots, the guide itself and the fork's own JVM tests are exempt: a `.jpg`,
`docs/fork.md` and `launcher/app/src/test/` are never worth a line each.

No `upstream` remote (a plain clone), no git and an unchanged tree all pass: the check only speaks when
there is a divergence to speak about.

Run from the repository root: python3 -m unittest discover -s tests -v
"""
import subprocess
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
GUIDE = ROOT / "docs" / "fork.md"
UPSTREAM = "upstream/main"

# A changed file of one of these kinds needs no line in the guide.
EXEMPT_SUFFIX = (".jpg", ".jpeg", ".png")
EXEMPT_PREFIX = ("launcher/app/src/test/",)
EXEMPT_EXACT = {"docs/fork.md"}


def git(*args):
    """git's stdout, or None when git is not installed (a source tarball has no .git)."""
    try:
        done = subprocess.run(["git", *args], cwd=str(ROOT), capture_output=True, text=True)
    except (OSError, subprocess.SubprocessError):
        return None
    return done.stdout if done.returncode == 0 else None


def base_commit():
    """The upstream commit this branch diverged from, or None when there is no upstream/main."""
    if git("rev-parse", "--verify", UPSTREAM) is None:
        return None
    out = git("merge-base", "HEAD", UPSTREAM)
    return out.strip() if out else None


def changed_files(base):
    """Paths that differ from `base`: committed, staged, working-tree and untracked."""
    names = set()
    for args in (("diff", "--name-only", base),                 # working tree (committed + staged + unstaged)
                 ("ls-files", "--others", "--exclude-standard")):  # new files git does not track yet
        out = git(*args)
        if out:
            names.update(line.strip() for line in out.splitlines() if line.strip())
    return names


def exempt(path):
    return path in EXEMPT_EXACT or path.endswith(EXEMPT_SUFFIX) or path.startswith(EXEMPT_PREFIX)


class ForkDocsTrackChangesTest(unittest.TestCase):
    def test_every_fork_change_is_named_in_the_guide(self):
        base = base_commit()
        if base is None:
            self.skipTest("no upstream/main in this checkout, so there is no divergence to check")
        guide = GUIDE.read_text(encoding="utf-8")
        undocumented = sorted(
            path for path in changed_files(base)
            if not exempt(path) and Path(path).name not in guide
        )
        self.assertEqual([], undocumented,
                         "these files differ from upstream/main but docs/fork.md does not name them; "
                         "add each to 'What the feature touches' (or a short section) so the next "
                         "rebase stays conflict-free:\n  " + "\n  ".join(undocumented))

    def test_the_guide_states_the_rule(self):
        # A checker the guide does not mention is one an agent never reads about.
        self.assertIn("tests/test_fork_docs_track_changes.py", GUIDE.read_text(encoding="utf-8"),
                      "docs/fork.md must name this test, so the rule is visible to the agent it binds")


if __name__ == "__main__":
    unittest.main()
