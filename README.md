# Japa Meditation — Android

A free, ad-free native Android app for chanting the Hare Krishna maha-mantra. It's a Kotlin + Jetpack Compose rewrite of the [Japa Meditation Online](https://github.com/vraja-pro/japa-meditation-online) WordPress plugin.

## Features

- **Voice detection.** Android's built-in speech recognizer listens in `hi-IN` and counts each complete 16-word maha-mantra. The word position carries over between utterances and noise is ignored, so the counting works the same way as on the web.
- **Guided chanting.** The 16 words light up in turn at Slow, Medium or Fast pace.
- **Manual counting.** Tap the big button, or use the **volume-up key** (volume-down takes one count off). The keys work with the screen in your pocket or with your eyes closed.
- **Lock tap mode.** A tap anywhere on the screen counts one mantra.
- **Progress ring** toward 108 (one mala), a rounds counter and a session timer.
- **Haptic tick** on every counted mantra. The screen stays on while a session runs.
- **Session dialog** with notes, and history for today (the last 10 sessions plus totals), stored on the device only.
- **Auto-end** after 1 minute of inactivity. The session is auto-saved if you don't respond.
- **English + Hebrew.** The app follows the phone language, and there is also a switch in the app header. Hebrew uses an RTL layout. On Android 13+ you can also pick the language per app in system settings.

## Project layout

```
app/src/main/java/com/vrajapro/japa/
  MainActivity.kt          volume keys, lifecycle
  JapaViewModel.kt         session state, timer, inactivity, guided pace
  speech/MantraCounter.kt  16-word counting logic (port of app.js) + unit tests
  speech/SpeechController.kt  continuous Android SpeechRecognizer wrapper
  data/SessionRepository.kt   today's history in SharedPreferences
  ui/                      Compose screens and theme (saffron & cream palette)
app/src/main/res/values{,-he}/strings.xml   English / Hebrew strings
```

## Build

**Android Studio:** open this folder and press Run. Use Android Studio Narwhal or newer with JDK 17+.

**Command line:**

```bash
./gradlew testDebugUnitTest   # counting-logic tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

**GitHub Actions:** every push to `main` builds a debug APK and uploads it as a workflow artifact. You can download it from the run's page and sideload it onto a phone. If signing secrets are configured, the workflow also builds a signed Play Store bundle (see [PUBLISHING.md](PUBLISHING.md)).

## Adding a language

1. Copy `app/src/main/res/values/strings.xml` to `values-<code>/strings.xml` and translate it.
2. Add a chip for the language in `LanguageSwitch()` in `ui/JapaScreen.kt`.

## Notes on voice detection

- The app uses whichever speech service is installed on the phone, usually Google. It needs internet unless the offline Hindi speech pack is installed (Settings → Google → Voice → Offline speech recognition).
- Android stops the recognizer after each pause and the app restarts it right away. On some phones this makes a short system "beep". Lowering media volume usually silences it.
- Listening pauses while the app is in the background, because Android blocks background microphone use.

## License

GPL-2.0+, same as the original plugin.
