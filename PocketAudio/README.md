# Pocket Media 5.0 — source awaiting build verification

This revision has not yet been compiled or device-tested. Automated approval review blocked publication of the changes to the public build repository. No v5 APK has been produced.

## Changes ready for review

- Liquid-glass-inspired surfaces and a media play/download icon replace the water-drop graphics.
- A 1.6-second launch overlay animates the icon and equalizer, then fades for 220 ms. It is skipped when receiving shared links or when a download is active. Tools warm in the background.
- The manual filename field is removed. yt-dlp metadata supplies the original title during the same download request. The save layer sanitizes unsafe filename characters and limits UTF-8 filename length.
- 720p and 1080p selections prefer H.264 video and AAC audio. The app probes downloaded tracks and converts incompatible codecs/pixel formats only when needed. 1080p60 has its own selection.
- Best, 4K, 6K and 8K preserve original codecs. The interface explains that some Gallery players cannot decode these formats. These modes do not promise universal playback.
- Extractor cache and four parallel fragments are enabled. No online update check is inserted before downloads. Platform response and transfer time cannot be made instant.
- About is organized into readable sections, separate full-license readers and upstream project links.

## Build and test

Use repository-root `.github/workflows/build-apk.yml`, with working directory `PocketAudio`. It builds the main APK and instrumentation APK, runs lint/signature checks and launches Android 15 tests.

Local build requires JDK 17, Gradle 8.11.1, Android SDK 35, NDK 28.0.13004108 and CMake 3.22.1.

```sh
cd PocketAudio
python3 scripts/prepare_lame.py
gradle --no-daemon assembleDebug assembleDebugAndroidTest lintDebug
```

`smoke5.py` exercises MP3, H.264 1080p60, VP9-to-H.264 and AV1-to-H.264 downloads using generated local samples. It checks title-derived filenames and invokes `PlaybackChecks` to decode visible frames through Android's own MediaMetadataRetriever. This test suite is prepared but has not run for v5.

The workflow uses debug signing. A previous installation may require uninstalling before a new APK can be installed. Shared Gallery/Music files remain, while the app's history/draft is reset. This is not a Play Store release bundle.

License: GPL-3.0; see LICENSE and applicable upstream licenses.
