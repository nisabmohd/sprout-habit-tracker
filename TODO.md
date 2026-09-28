# To do

Open work, roughly in priority order. Pick one, open an issue to say you're on it, and follow [`AGENTS.md`](AGENTS.md).

## Features

- [ ] Google sign-in and Drive backup in the `play` flavor: built on the `drive-backup` branch (one library, Play Services Auth). Needs the Google Cloud OAuth client, then an end-to-end test and a merge.
- [ ] In-app review (Play In-App Review library) in the `play` flavor, so Rate doesn't leave the app. Needs approval for the dependency.
- [ ] Play build: update from Google Play with the In-App Updates library, after the Play Store listing. Needs approval for the dependency.


## Before the first release

- [ ] Add the four signing secrets to GitHub so the release workflow can sign (the keystore is in the local `keystore/` folder; see `docs/RELEASING.md`).
- [ ] F-Droid metadata (`fastlane/metadata/android/en-US`: descriptions and the screenshots in `docs/screenshots`).
- [ ] Play Store listing text and graphics.

