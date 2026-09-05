# Gamia23 Blocks

An installable Android app for the Gamia23 block puzzle.

## Getting the APK

Every push to `main` builds it. Download the latest from the
[**latest release**](../../releases/tag/latest) — open that page on your phone,
tap `gamia23-blocks.apk`, and allow installing from this source when prompted.

## What this app is

A thin WebView shell around `https://gamia23.vercel.app/game`. The game itself
lives on the website, on purpose:

- Coins are awarded by the server, so the game has to be online regardless.
- Changing the game is a website deploy, not a new APK that every player has to
  reinstall.

What the shell adds over a browser tab: it opens straight into the game, keeps
the login cookie between launches, makes the Android back button step back
through the game instead of closing the app, and shows a real message when the
phone is offline.

## Signing

CI publishes a **debug-signed** APK. That is what makes it installable by
sideloading with no keystore to manage. It is not suitable for the Play Store —
that needs a real upload key, which should never be committed to a repo.

## Building it yourself

```
gradle wrapper --gradle-version 8.7
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/`.
