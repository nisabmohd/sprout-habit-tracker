# To do

Open work, roughly in priority order. Pick one, open an issue to say you're on it, and follow [`AGENTS.md`](AGENTS.md).

## Before the first release

- [ ] Create the release keystore, add the four signing secrets and tag `v1.0.0` (see `docs/RELEASING.md`).
- [ ] Check scrolling on a real low-end phone with 20+ habits. The emulator showed dropped frames, but emulator timing isn't reliable.
- [ ] F-Droid metadata (`fastlane/metadata/android/en-US`: descriptions and the screenshots in `docs/screenshots`).
- [ ] Play Store listing text and graphics.

## Features

- [ ] Google sign-in and Drive backup in the `play` flavor: back up now, restore, daily automatic backup to Drive's hidden app folder. Needs a Google Cloud OAuth client and four `play`-only libraries (Credential Manager, its Play Services bridge, Google ID, Play Services Auth).
- [ ] Store the time a habit was logged, so a done card can say "Done at 6:52 AM" as in the design.
- [ ] Move UI text into `strings.xml` so the app can be translated.

## Quality

- [ ] QA & bug fixes