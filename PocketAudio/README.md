# Pocket Media 5.0

Android audio/video downloader with a liquid-glass-inspired interface, separate Download / Recent / Settings tabs, original-title filenames and an approximately 1.8-second animated cold-launch splash.

## Install

Android 10 (API 29) or later is required. The universal APK includes arm64-v8a, armeabi-v7a and x86_64 libraries. Version code 5, version name 5.0.

The delivered APK is a non-debuggable release signed with a permanent private release key. Uninstall v4 before installing this release because the signing certificate changed. App history and preferences reset; shared Gallery/Music downloads remain. Future updates must use the same release key. A generic warning when opening an APK received through WhatsApp may remain. The other reported phone's exact installation failure was not identified without its Android version and installer log.

## Features

- Original platform title is retrieved during download and sanitized for the saved filename.
- 720p and 1080p Gallery-safe modes prefer H.264/AAC and convert incompatible tracks when necessary. 1080p60 has a separate option.
- Best / 4K / 6K / 8K Original modes retain source codecs and require a compatible player/device.
- Extractor caching, early initialization and four parallel fragments reduce avoidable waiting. Network and platform response time still apply.
- About uses readable sections and separate full-license readers.
- Saved media appears in Movies/PocketMedia or Music/PocketMedia.

## Build

Requires JDK 17, Gradle 8.11.1, Android SDK 35, NDK 28.0.13004108 and CMake 3.22.1.

```sh
cd PocketAudio
python3 scripts/prepare_lame.py
gradle --no-daemon assembleRelease assembleReleaseAndroidTest lintRelease
```

CI uses a temporary test certificate so instrumentation can run against the release build. Distribution requires re-signing with the privately retained release key; no private signing material belongs in this public repository.

See VALIDATION.md for test evidence and limits. License: GPL-3.0 and applicable upstream notices.
