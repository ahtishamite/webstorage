# Pocket Audio — Android source project

**Build status: APK compilation and device testing have NOT been completed.**
The delivery environment had no Android SDK, JDK compiler, Gradle or emulator, and the Android SDK download was blocked by network approval. This ZIP is a source project, not an installable APK. The workflow below is provided but has not been run.

## App behavior

- No app account, API key, backend, ads or analytics.
- Paste a public HTTP/HTTPS direct MP4 file link; choose 128, 192 or 320 kbps.
- Downloads video to temporary private storage; decodes its audio using Android MediaCodec and encodes a real MP3 with LAME 3.100 on the phone.
- Saves the result under **Downloads / PocketAudio**, then provides Play and Share.
- Progress, cancellation, network timeouts, bounded redirects, size/storage checks and error messages are included.
- Conversion runs in a foreground service. Closing the screen does not intentionally cancel it; force-stop, process termination or system limits may interrupt it. No automatic resume.
- Android 10 or later; ARM64, ARM32 and x86_64. Native build uses 16 KB alignment.
- Maximum input size 1 GiB; first supported mono/stereo audio track only. Decoding support depends on the device. Output is 44.1 kHz MP3. Higher bitrate does not restore quality absent in the source.
- Supports direct video files, not YouTube/Instagram webpage links, DRM, authenticated URLs or playlists. Expiring signed direct URLs can work while valid.
- App requests internet, foreground-service, wake-lock and optional notification permissions. It does not request access to your existing media files.

## Build an APK using GitHub Actions

A GitHub account is needed only if you choose this build route; using the installed app requires no credentials.

1. Create a repository and upload the **contents** of this project, including `.github/workflows/build-apk.yml`. The `app` folder must be at the repository root.
2. Open **Actions → Build installable APK → Run workflow**. A push also triggers it.
3. The workflow installs the SDK, downloads upstream LAME source, compiles the three architectures, runs Android lint and verifies the APK signature.
4. After a successful run, download the **PocketAudio-APK** artifact, unzip it and copy `app-debug.apk` to your phone.
5. Open the APK and permit installation from your browser/Files app when Android asks.

This produces a debug-signed APK for personal testing. Each clean runner may generate a different debug key, so installing a later build might require uninstalling the previous build first. For ongoing distribution, configure a stable private release-signing key; do not commit that key. No repository or workflow has been created or executed on your behalf.

## Build locally (Linux)

Install JDK 17, Gradle 8.11.1, Python 3.12+, Android SDK tools, NDK 28.0.13004108 and CMake 3.22.1. The native setup uses POSIX shell and is intended for Linux, including the supplied CI runner. Build dependencies and LAME source need internet access only at build time.

```sh
sdkmanager 'platforms;android-35' 'build-tools;35.0.0' 'ndk;28.0.13004108' 'cmake;3.22.1'
python3 scripts/prepare_lame.py
gradle --no-daemon assembleDebug lintDebug
```

APK path: `app/build/outputs/apk/debug/app-debug.apk`.
A Gradle wrapper is not bundled; use the pinned Gradle installation above. Set `ANDROID_HOME` or a local `sdk.dir` in `local.properties`.

## Verification still required before calling this production-ready

The supplied `VALIDATION.md` records the checks possible here. Compilation, dependency downloads, JNI loading, a real conversion and phone UI behavior must be verified after a build succeeds.

Device acceptance checklist:

1. Convert known AAC-in-MP4 samples with mono and stereo audio at all three quality settings. Open the saved MP3 and compare audible start/end and duration with the source.
2. Verify 44.1 kHz output and MP3 codec with `ffprobe output.mp3`; a filename extension alone is not proof of MP3 encoding.
3. Test an invalid URL, HTML page, expired link, redirect loop, video without audio, unsupported surround audio, interrupted download and low-storage condition.
4. Cancel during download and encoding. Confirm no partial visible MP3 and no leftover temporary files after handled cancellation.
5. Rotate the phone, turn the screen off during conversion, deny notification permission, then return to the app.
6. Verify Play/Share, Downloads visibility and a second conversion. Test at least Android 10 and Android 15+ on an ARM64 device, including a 16 KB page-size device if applicable.

## Sources and licensing

Android APIs: https://developer.android.com/reference/android/media/MediaCodec
and https://developer.android.com/reference/android/media/MediaExtractor

LAME: https://lame.sourceforge.io/ — LAME 3.100, LGPL 2.0 or later.
The build downloads the versioned upstream archive and copies its complete `COPYING` license into the app's About screen. Encoder source remains in `third_party/lame-3.100` for inspection/rebuilding. This project currently downloads the versioned source without a pinned archive checksum; review and pin it before release distribution. Preserve applicable LAME source/relinking and license obligations when distributing binaries.

The application-specific code in this project is provided for your use and modification. No third-party source or compiled dependency is bundled in this source ZIP.
