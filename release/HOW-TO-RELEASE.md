# How to release Sprout on Google Play

This walks through every screen the Play Console shows for a new app, with the answer for Sprout. The answers were checked against the `play` build on `main` (1.1.0, versionCode 7): no internet permission, no Google libraries, no ads, no analytics, no account.

Play Console screens move around now and then. If a question below doesn't match what you see, answer it from the facts in [Sprout in one table](#sprout-in-one-table).

## Before you start: two things that can block a release tomorrow

1. **Testing rule for new personal accounts.** A personal developer account created after 13 November 2023 can't publish to Production straight away. You must first run a **closed test with at least 12 testers who stay opted in for 14 days in a row**, then apply for production access, which Google reviews (about 7 days). Organisation accounts skip this step. Check which one you have in **Play Console → Settings → Developer account → Account details**, or look for the "Apply for production" card on the dashboard. On a personal account, "tomorrow" means starting the closed test, not going public.
2. **Developer account verification.** Identity (and for organisations, D-U-N-S) verification has to be finished before you can publish. The dashboard shows a banner if anything is pending.
3. **The Sponsor link.** Done in build 7: the `play` build has no Sponsor row in About and no Sponsor button in the support prompt (`!BuildConfig.PLAY_STORE`), because Play's Payments policy restricts asking for money outside Google Play billing. The `foss` and `github` builds keep it.

## Fill in before you start

| What | Where it's used | Value |
| --- | --- | --- |
| Contact email (shown publicly on the listing) | Store settings, `PRIVACY.md` | ______ (replace `CONTACT_EMAIL` in `PRIVACY.md`) |
| Privacy policy URL | App content → Privacy policy | `https://github.com/nisabmohd/sprout-habit-tracker/blob/main/PRIVACY.md` (push `PRIVACY.md` first, and the repo must be public) |
| Website (optional) | Store settings | `https://github.com/nisabmohd/sprout-habit-tracker` |

## Sprout in one table

| Item | Value |
| --- | --- |
| App name on Play (max 30) | `Sprout: Habit Tracker` |
| Package name | `app.sprout.habits` (can never change after the first upload) |
| Version | `1.1.0`, versionCode `7` (build 6 went to Internal testing and still shows Sponsor; use 7 for Closed testing) |
| minSdk / targetSdk | 26 (Android 8.0) / 36 |
| Upload file | Android App Bundle: `app/build/outputs/bundle/playRelease/app-play-release.aab` (about 6 MB) |
| Upload key | `keystore/sprout-release.jks`, alias `sprout`. Certificate SHA-256 `2A:71:9A:FB:54:66:B5:74:65:8C:41:D4:DA:9F:5D:84:12:C2:FA:F5:65:36:3C:00:AC:93:C8:91:AF:B6:D6:C0` |
| Price | Free |
| Ads | None |
| Account / login | None |
| Network | None in the play build (no `INTERNET` permission) |
| Data collected or shared | None. Everything stays on the device |
| Permissions | `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`; plus `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE` added by WorkManager (used by the Glance widgets). No foreground service type is declared |
| Advertising ID | Not used (no `AD_ID` permission) |
| In-app purchases | None |
| Languages in the app | English, Hindi, Spanish, German, French, Portuguese (Brazil), Japanese |

## Step 1: Build the bundle

```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:bundlePlayRelease
```

Output: `app/build/outputs/bundle/playRelease/app-play-release.aab`. It's signed with the upload key from `keystore/keystore.properties`. The R8 mapping file is inside the bundle, so Play deobfuscates crash reports without a separate upload.

Check the signature (the SHA-256 must match the table above):

```sh
"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" -printcert -jarfile app/build/outputs/bundle/playRelease/app-play-release.aab
```

Back up `keystore/` (both files) somewhere outside this folder before uploading. Lose it and you'll need Google support to reset the upload key.

## Step 2: Create the app

**Home → Create app**

| Question | Answer |
| --- | --- |
| App name | `Sprout: Habit Tracker` |
| Default language | English (United States) – en-US |
| App or game | App |
| Free or paid | Free (a free app can't be changed to paid later) |
| Declarations | Tick Developer Program Policies and US export laws |

## Step 3: App content (Policy and programs → App content)

Answer every item. A release can't be sent for review while one is still open.

### Privacy policy
URL: `https://github.com/nisabmohd/sprout-habit-tracker/blob/main/PRIVACY.md`

### App access
**All functionality in my app is available without any access restrictions.** There's no login.

### Ads
**No, my app does not contain ads.**

### Content rating
Start the questionnaire.
- Email: your contact email
- Category: **All other app types** (it isn't a game, social or communication app)
- Violence, sexuality, language, controlled substances, crude humour, gambling: **No** to all
- Does the app let users interact or exchange content with other users? **No** (notes stay on the device)
- Does the app share the user's current physical location with other users? **No**
- Does the app allow users to buy digital goods? **No**
- Is it a web browser or search engine? **No**
- Expected result: **Everyone** / PEGI 3 / USK 0. Save, then **Submit**.

### Target audience and content
- Target age groups: **13–15, 16–17, 18 and over**. Don't tick any group under 13; that brings in the Families policy and extra review.
- Could your store listing unintentionally appeal to children? **No**

### Data safety
- Does your app collect or share any of the required user data types? **No**
- Preview and **Submit**. The listing will say "No data collected" and "No data shared with third parties".

That answer is accurate for this build: it has no internet permission, and everything stays in the app's private storage. If Drive backup (branch `drive-backup`) or any network feature is added to the play build later, this form must be redone before that release.

### Government apps
**No**

### Financial features
**My app doesn't provide any financial features.**

### Health apps
Sprout is a general habit tracker. Users name their own habits, and it doesn't read sensors, health records or Health Connect. Answer **My app does not have any health features**. (Choosing the Health & Fitness category instead of Productivity doesn't change this, but keeping the category Productivity keeps it consistent.)

### News apps
**No**

### Advertising ID
**No**, the app doesn't use advertising ID. The merged manifest has no `com.google.android.gms.permission.AD_ID`.

### Permission declarations that are not needed
- Exact alarms: Sprout uses `SCHEDULE_EXACT_ALARM`, which needs no declaration. Only `USE_EXACT_ALARM` does.
- Foreground services: no declaration, because no service declares a `foregroundServiceType`.
- No photo, video, location, SMS, call log, accessibility or install-packages permissions in the play build. `REQUEST_INSTALL_PACKAGES` exists only in the `github` flavor; never upload that build to Play.

## Step 4: Store settings (Grow users → Store presence → Store settings)

| Field | Answer |
| --- | --- |
| App category | Productivity |
| Tags | Habit tracker, Journal, Productivity (pick up to 5 from Google's list) |
| Email | your contact email (required) |
| Phone | leave empty |
| Website | `https://github.com/nisabmohd/sprout-habit-tracker` |
| External marketing | leave on |

## Step 5: Main store listing (Grow users → Store presence → Main store listing)

| Field | Value |
| --- | --- |
| App name | `Sprout: Habit Tracker` |
| Short description (max 80) | `Track habits, log part of a day, and keep a journal. Free, open source, no ads.` |
| Full description (max 4000) | the text in [`README.md`](README.md#listing-text-draft) |
| App icon | `icon-512.png` |
| Feature graphic | `feature-graphic-1024x500.png` |
| Phone screenshots | `phone-screenshots/01-today.png` … `08-today-dark.png`, in that order |
| Tablet screenshots, video | skip |

Things that get listings rejected, all avoided here: no "best/#1/free download" claims, no other app names or Google trademarks, no keyword lists, and screenshots that show the real app.

## Step 6: Release

### Play App Signing
On the first release Play asks how to sign the app. Keep **Use Google-generated key** (the default). The upload key above only proves that uploads come from you.

Side effect: the Play install and the GitHub APK will then be signed with different keys, so a user can't update from one to the other without uninstalling first. That's normal for apps on several stores. If you want one signature everywhere, choose **Use existing app signing key from Java keystore** instead and follow Google's PEPK steps with `keystore/sprout-release.jks`. Decide before the first upload; it can't be undone.

### Personal account: closed test first
1. **Test and release → Testing → Closed testing → Create track** (or use the default "Closed testing – Alpha").
2. **Testers:** add an email list with at least 12 Google accounts (Gmail addresses the testers use on their phones), or a Google Group. Save, and copy the **opt-in link**.
3. **Countries/regions:** add the countries your testers are in (or all).
4. **Create new release** → upload `app-play-release.aab` → release name `1.1.0 (7)` → release notes:
   ```
   <en-US>
   First release on Google Play.
   - Track habits as Done, Partial or Skip, with swipe, tap or press and hold
   - Measure habits with a target and step (pages, minutes, glasses)
   - Journal notes for each habit and day
   - Week view, 26-week heatmap, streaks and Insights
   - Reminders and four home-screen widgets
   </en-US>
   ```
5. **Review release → Start rollout to Closed testing.** The first review usually takes a few hours to a few days.
6. Send the opt-in link to the testers. Each one opens it, taps **Become a tester**, then installs from the Play link on the same page. They must stay opted in for **14 days in a row**. Keep 14–15 testers in case someone drops out.
7. After 14 days, **Dashboard → Apply for production**. Google asks how you recruited testers, what feedback you got and what you changed, and whether the app is ready. Answer plainly (for example: friends and colleagues; feedback on the amount sheet and reminders; fixed X; ready because the features are complete and tested on Android 8 to 16).
8. Once approved, do the Production steps below, promoting the same release or uploading a newer versionCode.

### Organisation account (or after production access)
1. **Test and release → Production → Countries/regions → Add countries/regions** → select all (or the ones you want).
2. **Create new release** → upload the `.aab` → release name `1.1.0 (7)` → the release notes above.
3. **Next → Save → Go to overview → Send changes for review.** If **Managed publishing** is on, you press **Publish** yourself after approval; if it's off, the app goes live as soon as it's approved.
4. A new app's first review often takes 3–7 days. Updates are usually faster.

## Step 7: After it's live

- Open the listing at `https://play.google.com/store/apps/details?id=app.sprout.habits`. Then set `PLAY_LISTED` to `"true"` in the `play` flavor in `app/build.gradle.kts` and ship that in the next upload: it turns on **Rate on Play Store** (About and the support prompt) and About's **Check for updates**, which both open this listing. They stay hidden until then because the listing doesn't exist yet.
- Every later upload needs a higher `versionCode` in `app/build.gradle.kts`. GitHub and Play share the same numbers, so bump it once per release for both.
- Look at **Quality → Android vitals** and the **Pre-launch report** after the first rollout for crashes on Google's test devices.

## Checklist

- [ ] Account type checked (personal → closed test with 12 testers for 14 days first)
- [ ] Developer account verification complete
- [ ] `CONTACT_EMAIL` replaced in `PRIVACY.md`, committed and pushed; the privacy URL opens in a private browser window
- [ ] `keystore/` backed up outside the repo
- [ ] `./gradlew :app:bundlePlayRelease` built, signature checked
- [ ] App created with name, language, free
- [ ] App content: privacy, app access, ads, content rating, target audience, data safety, government, financial, health, news, advertising ID all show ✓
- [ ] Store settings: category, email, website
- [ ] Main store listing: texts, icon, feature graphic, 8 screenshots
- [ ] Play App Signing choice made
- [ ] Release uploaded to Closed testing (personal) or Production (organisation), countries selected, sent for review
