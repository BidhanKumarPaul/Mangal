# Mangal — Release & Deployment Guide (Phase 6)

## 1. Generate an upload keystore (once, keep this file forever)
```bash
keytool -genkey -v -keystore mangal-upload.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias mangal-upload
```
Store `mangal-upload.jks` OUTSIDE the git repo. Losing it means you can never
update your app under the same package name again (unless you're enrolled in
Play App Signing, which is why step 2 below matters).

## 2. Wire signing into the build (already stubbed in app/build.gradle.kts)
Do NOT hardcode the keystore path/passwords in build.gradle.kts. Instead,
pass them as Gradle properties, e.g. in `~/.gradle/gradle.properties`
(outside the repo) or as CI secrets:
```properties
MANGAL_STORE_FILE=/absolute/path/to/mangal-upload.jks
MANGAL_STORE_PASSWORD=...
MANGAL_KEY_ALIAS=mangal-upload
MANGAL_KEY_PASSWORD=...
```
`app/build.gradle.kts` already reads these via `project.findProperty(...)`
and only applies the release signing config if they're present.

## 3. Build the release AAB
```bash
./gradlew bundleRelease
# output: app/build/outputs/bundle/release/app-release.aab
```
I have not run this command — no NDK/network in my environment. Run it
yourself and send me the actual output if it fails; I'll fix real errors
rather than guess at hypothetical ones.

## 4. Test the AAB locally before uploading anywhere
```bash
# bundletool: https://github.com/google/bundletool/releases
java -jar bundletool.jar build-apks \
  --bundle=app-release.aab \
  --output=mangal.apks \
  --ks=mangal-upload.jks --ks-key-alias=mangal-upload \
  --mode=universal

java -jar bundletool.jar install-apks --apks=mangal.apks
```

## 5. Play Console — restricted permissions review (READ THIS FIRST)
Mangal requests **SEND_SMS** and **CALL_PHONE**. These trigger Google's
restricted-permissions declaration flow:
- Play Console > App content > Permissions declaration form.
- Google generally expects apps using these permissions to be a genuine
  default handler candidate (SMS or dialer app) for the *core* use case, not
  a side feature of an assistant app. There is a real chance this gets
  rejected or requires removing these two permissions/tools for the Play
  Store build specifically (you could ship a sideload-only build with them
  enabled, and a Play Store build without SendSmsTool/AlarmTool's calling
  path registered).
- Decide before submitting: (a) apply and hope the declaration is accepted,
  (b) ship two build flavors (Play-safe vs. full/sideload), or (c) drop
  SMS/calling from v1 entirely. I'm flagging this rather than deciding it
  for you, per your own instructions.

## 6. Upload
- Play Console > your app > Production (or Testing > Closed testing, do
  this FIRST) > Create new release > upload the .aab.
- Fill in the Data Safety form using PRIVACY_POLICY.md as your source of
  truth — keep them consistent or review will bounce it back.
- Start with a **closed testing track** with a small tester list before any
  production rollout, especially given the SMS/CALL_PHONE situation above.

## 7. Native library check before you ship
Confirm `app/build/outputs/bundle/release/app-release.aab` actually contains
`lib/arm64-v8a/libmangal_llama.so` and `libmangal_whisper.so` — unzip/inspect
it. If ProGuard/R8 stripped or renamed anything under
`com.bkpit.mangal.llm` / `com.bkpit.mangal.stt`, JNI calls will throw
`NoSuchMethodError` in release builds only (this is exactly what
`proguard-rules.pro`'s `-keep` rules for those packages exist to prevent —
verify they're still in effect if you touch that file).
