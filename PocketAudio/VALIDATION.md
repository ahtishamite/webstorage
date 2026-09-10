# V5 validation

## Passed

- Release compilation, Android lint, APK signature verification and ZIP alignment: workflow 34461041353, application source commit 57cce5a87d7549fc31cdd5542b83d6d072bfacba.
- Android 15 emulator: MP3, H.264/AAC 1080p60, VP9/Opus and AV1/Opus conversion into H.264/AAC, original-title filenames, separate tabs and About.
- Android-native MediaMetadataRetriever decoded visible frames from all three video outputs (ANDROID_VISIBLE_FRAMES_PASS count=3).
- Android 10 emulator: fresh installation, same-certificate update and crash-free launch after both, workflow 34462744603, job 102824139004.
- Final distribution APK independently signed and verified with the permanent release certificate. Application payload is unchanged from the tested release.
- Distribution SHA-256: fb066595cd567c815cfb2bae296e0c34bde64d73da80289582ef3a264bf12f1c.
- Release certificate SHA-256: 5271ac53585666226567fdaf1cf326ace068b9cb12aa79945d14aab278f8810b.

## Scope and limitations

Tests ran on emulators, not the user's second physical phone. CI install tests used the CI certificate; the final certificate was verified separately. The original Android 10 combined media/UI test installed and opened the app but failed to obtain a UIAutomator XML dump. The independent install/update test then passed; Android 10 full media automation is not claimed.

The user's failing YouTube file was not supplied. Codec compatibility is the inferred black-screen cause; Gallery-safe conversion addresses this class and passed AV1/VP9 fixture tests. Re-download using 1080p Gallery safe to obtain a compatible file. High-resolution Original modes retain device/player limitations.

No claim is made of instant downloads, every platform, private/restricted links, or successful installation on every Android device. Android 9 and older are unsupported.
