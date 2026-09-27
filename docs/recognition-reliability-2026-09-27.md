# Recognition callback fixes — 2026-09-27

- Correct `onRecognitionSkipped`: ordinary NNFP skips must notify NNFP callbacks,
  not On Demand callbacks. Forced audio errors still notify both sources.
- Handle the no-music secondary-detector exit through the existing LoggingHooks
  `(String, Object)` hook. The exact message and signature were inspected in the
  supported original base APK (`pvy`, call to `ono.w(String,Object)`). This path
  returns without NnfpRecognizer.recognize, so the normal result hook cannot fire.
  It now reports NNFP NoMatch with no audio. No threshold or audio algorithm changed.
- Use ConcurrentHashMap for callbacks: Binder registration/removal and worker-thread
  dispatch can overlap. Tests also cover unregistering during callback delivery.
  No lock is held while invoking remote callbacks.

Base APK SHA-256:
`cd925103149cfe9f688001c4c31191c1399961252c89d182a70396fa21f74bb5`.

Validation:

```sh
bash gradlew :overlay:testDebugUnitTest buildApkRelease --no-daemon -Dos.arch=x86_64 --init-script /private/tmp/nowplaying-jvm17.init.gradle --console=plain -q
adb -s 3B162T00UET00000 install -r build/out-release.apk
```

Four unit tests passed. APK build, alignment, signature verification and USB update
succeeded. The existing local init script aligns Kotlin JVM target with Java 17 on
this macOS Java-21 environment; x86_64 selects the available protoc binary via Rosetta.
No runtime dependency was updated. JUnit 4.13.2 is test-only.

The paired AMM update accepts early failures and replayed progress instead of leaving
the dialog active. A real-device no-music recognition reached No Track Matched.
This does not verify successful identification or the live Google On Demand backend.

These changes do not repair the native Pine engine itself. The previous removal of
obsolete hooks remains the native-crash mitigation; LSPlant/Pine Android 16 stability
still needs longer real-world testing. No APK, base APK or signing secret is committed.
