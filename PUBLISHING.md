# Publishing to Google Play

The app is free for users. Google Play does charge a **one-time $25 developer registration fee**.

## 1. Developer account

1. Sign up at https://play.google.com/console and pay the $25 fee.
2. Complete identity verification.
3. **Personal accounts created after November 2023** must run a **closed test with at least 12 testers for 14 continuous days** before they can publish to Production. Organization accounts are exempt. Ask your sangha or friends early, because this step takes the longest.

## 2. Create an upload key (once, keep it safe)

```bash
keytool -genkeypair -v -keystore japa-upload.jks -keyalg RSA -keysize 2048 \
  -validity 10000 -alias japa
```

For local release builds, create `keystore.properties` in the project root. This file is git-ignored.

```properties
storeFile=/absolute/path/to/japa-upload.jks
storePassword=...
keyAlias=japa
keyPassword=...
```

Then run `./gradlew bundleRelease`. The output is `app/build/outputs/bundle/release/app-release.aab`.

To build the bundle in **GitHub Actions** instead, add these repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `JAPA_KEYSTORE_BASE64` | output of `base64 -i japa-upload.jks` |
| `JAPA_KEYSTORE_PASSWORD` | keystore password |
| `JAPA_KEY_ALIAS` | `japa` |
| `JAPA_KEY_PASSWORD` | key password |

Enable **Play App Signing** when you upload your first bundle. Google then holds the app signing key, and your key is only the upload key, so you can reset it if you lose it.

## 3. Store listing (copy-paste starter)

**App name:** Japa Meditation: Hare Krishna Counter

**Short description (≤80):**
Count Hare Krishna maha-mantras by voice, tap or guided pace. Free, no ads.

**Full description:**

> A focused, peaceful japa companion for chanting the Hare Krishna maha-mantra.
>
> 🎙 Voice detection: chant aloud and every complete maha-mantra is counted automatically.
> 🪷 Guided chanting: follow the words as they light up at slow, medium or fast pace.
> 👆 Manual and lock-tap counting: tap the screen or press the volume key, even with your eyes closed.
> 📿 108-bead mala ring, rounds counter and session timer.
> 📝 Session notes and today's history with totals.
> 🌐 English and Hebrew.
>
> Free forever. No ads, no account, no tracking. Your practice stays on your phone.
>
> Hare Krishna Hare Krishna Krishna Krishna Hare Hare
> Hare Rama Hare Rama Rama Rama Hare Hare

**Category:** Lifestyle (or Health & Fitness → Meditation). **Tags:** meditation, mantra, japa.

**Graphics needed:**

- App icon, 512×512 PNG. Export it from the adaptive icon in Android Studio (right-click `res` → New → Image Asset), or design one.
- Feature graphic, 1024×500.
- At least 2 phone screenshots. Take them from an emulator or phone in both English and Hebrew.

## 4. App content forms (Play Console → Policy → App content)

- **Privacy policy URL.** Required because the app uses the microphone. Host `PRIVACY.md` (GitHub Pages, or a page on your WordPress site) and paste the URL.
- **Ads:** No.
- **Data safety:** no data is collected or shared. Audio is processed by the system speech service and is not collected by the app.
- **Content rating:** fill in the questionnaire (Everyone).
- **Target audience:** 13+ is the simplest choice. "Kids" triggers the Families policy.
- **Government / financial / health features:** None.

## 5. Release

1. Testing → Closed testing → create a track, upload the `.aab` and add your testers' emails.
2. After 14 days with 12+ testers (personal accounts only), apply for Production access.
3. Production → Create release → upload → roll out.

Bump `versionCode` (and `versionName`) in `app/build.gradle.kts` for every upload.
