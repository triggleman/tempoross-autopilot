# Submitting Tempoross Autopilot to the RuneLite Plugin Hub

## Checklist (done in this repo)

- [x] Public GitHub repo: https://github.com/triggleman/tempoross-autopilot
- [x] `runelite-plugin.properties` with displayName, author, description, tags, plugins, support
- [x] `icon.png` at the repo root (48x48, the limit is 48x72)
- [x] `README.md` describing the features for players
- [x] `LICENSE` (BSD 2-Clause) and `NOTICE` (credits, Jagex trademark line)
- [x] No third-party dependencies (only RuneLite, which the Hub provides)
- [x] Resources loaded from the jar (`ImageUtil.loadImageResource`), no files on disk
- [x] No automation: the plugin only shows information, highlights and notifications
- [x] No network calls, threads or file access of its own
- [x] Unit tests pass (`./gradlew test`), and GitHub Actions builds every push

## Submitting

The submission branch is `tempoross-autopilot` on https://github.com/triggleman/plugin-hub. It adds
`plugins/tempoross-autopilot` with:

```
repository=https://github.com/triggleman/tempoross-autopilot.git
commit=THE_40_CHARACTER_HASH
```

Open a pull request from that branch to `runelite/plugin-hub` master, titled `Add Tempoross Autopilot`.

1. Wait for the checks:
   - **build (pull_request)**: if it fails, click Details and send the log.
   - **RuneLite Plugin Hub Checks**: if it lists changes, send the list.
2. To update before approval: push here, then change the `commit=` line on the same branch.
   Don't open a new pull request.

## Updating later (after it's live)

Every new version is a small pull request to plugin-hub that only changes the `commit=` line to
the newest hash.
