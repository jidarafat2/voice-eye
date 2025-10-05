VoiceEye - README

How to build:
1. Open Android Studio.
2. File -> Open -> select the VoiceEye_Android_Source folder.
3. Let Gradle sync. If prompted, install recommended SDK/Gradle components.
4. In MainActivity (or at runtime), set your Telegram Bot Token and Chat ID in the UI.
   Do NOT hardcode your token in public places.
5. Build -> Build Bundle(s) / APK(s) -> Build APK(s)
6. Install the generated APK on your device.

Security:
- Do NOT share your Bot Token publicly.
- This app shows a notification while recording and requests RECORD_AUDIO permission.

