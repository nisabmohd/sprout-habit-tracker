# To do

Open work, roughly in priority order. Pick one, open an issue to say you're on it, and follow [`AGENTS.md`](AGENTS.md).

## Before the first release

- [ ] Create the release keystore, add the four signing secrets and tag the release (see `docs/RELEASING.md`).
- [ ] Check scrolling on a real low-end phone with 20+ habits. The emulator showed dropped frames, but emulator timing isn't reliable.
- [ ] F-Droid metadata (`fastlane/metadata/android/en-US`: descriptions and the screenshots in `docs/screenshots`).
- [ ] Play Store listing text and graphics.

## Features

- [ ] Google sign-in and Drive backup in the `play` flavor: back up now, restore, daily automatic backup to Drive's hidden app folder. Needs a Google Cloud OAuth client and four `play`-only libraries (Credential Manager, its Play Services bridge, Google ID, Play Services Auth).
- [ ] Move UI text into `strings.xml` so the app can be translated.
- [ ] In-app review (Play In-App Review library) in the `play` flavor, so Rate doesn't leave the app. Needs approval for the dependency.

## Quality

- [ ] QA & bug fixes