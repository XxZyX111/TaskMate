# TaskMate

A small native Android application written in Kotlin and Jetpack Compose. Edit it in VS Code and build it with the included Gradle wrapper.

## Included

- Firebase email/password registration, login, persistent session, password reset, logout.
- Add, edit, complete, and delete student tasks.
- Title, subject, optional deadline in YYYY-MM-DD format.
- All / Active / Done filters and an account dialog.
- Local task persistence separated by Firebase user ID.
- Validation, loading indicators, deletion confirmation, and a Firebase setup screen.

## Windows setup — start here

1. Extract this ZIP and open the **TaskMate** folder in VS Code. Open the folder containing `settings.gradle.kts`, not its parent.
2. Install Android Studio from https://developer.android.com/studio for its SDK tools. You can keep writing code in VS Code.
3. In Android Studio's **SDK Manager**, install **Android SDK Platform 35**, **Android SDK Build-Tools 35.0.0**, **Android SDK Platform-Tools**, and **Android SDK Command-line Tools (latest)**. For an emulator, also install Android Emulator and an API 35 system image and create a device in Device Manager.
4. Use **JDK 17**. Install a JDK 17 distribution and set `JAVA_HOME` to its installation folder; add `%JAVA_HOME%\bin` to PATH. Restart VS Code after changing environment variables. Run `java -version` in its terminal to confirm. If Android Studio asks for a Gradle JDK, select the same JDK 17.
5. Create `local.properties` in the TaskMate root with your actual SDK path. Use forward slashes on Windows. For example:

```properties
sdk.dir=C:/Users/YOUR_WINDOWS_USERNAME/AppData/Local/Android/Sdk
```

Find the actual SDK location at the top of Android Studio's SDK Manager. `local.properties` is machine-specific and is not committed.

## Connect Firebase — required for real login

1. Open https://console.firebase.google.com/ and create a project. Google Analytics is optional for this app.
2. Add an **Android** application with the exact package name **com.aqeela.taskmate**. SHA fingerprints are not required for email/password login.
3. Download **google-services.json** and place it at **TaskMate/app/google-services.json**. Do not place it in the root or rename it. Firebase SDK and plugin dependencies are already configured.
4. In **Build → Authentication**, select **Get started**, then **Sign-in method → Email/Password** and enable the email/password provider. Email-link authentication is not needed.
5. If Firebase asks for an Android Studio sync, simply build this project with the terminal command below instead.

Without that config, the app can build but displays setup instructions. There is no fake login or embedded account. Firebase manages passwords; this app never stores them in its task database. Registration uses a minimum six-character check; Firebase may enforce a stricter password policy if you configure one.

## Build and run from VS Code

In the integrated PowerShell terminal, from the TaskMate folder:

```powershell
.\gradlew.bat assembleDebug
```

The first build downloads Gradle and dependencies, so internet access is required. The APK appears at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

For an **Android phone**, enable Developer options and USB debugging, connect it by USB, and accept the debugging prompt on the phone. Windows may also need the manufacturer's USB driver. For an **emulator**, start the device in Android Studio's Device Manager first. Then:

```powershell
.\gradlew.bat installDebug
```

Open **TaskMate** from the device's app launcher. VS Code also provides build/install tasks under **Terminal → Run Task**. On macOS/Linux use `./gradlew` instead of `.\gradlew.bat`.

This is an Android app. To run it from an iPhone, you would need a separate iOS project or a cross-platform implementation.

## Where to edit

| File | Responsibility |
|---|---|
| `MainActivity.kt` | Compose screens, theme, task editor |
| `TaskMateViewModel.kt` | Authentication, validation, application state |
| `TaskStore.kt` | Task model and per-user local JSON storage |
| `app/build.gradle.kts` | Android settings and app dependencies |

Kotlin source files are inside `app/src/main/java/com/aqeela/taskmate/`. The `java` directory also supports Kotlin sources.

## Manual verification checklist

1. Register account A, add two tasks, edit one, complete one, and switch filters.
2. Try a blank title and an impossible date such as 2026-02-30. Neither should save.
3. Cancel a deletion, then confirm a deletion.
4. Close and reopen the app. Account A should remain signed in and its tasks should remain.
5. Log out, register account B, and confirm A's tasks are not shown. Log back into A and confirm its remaining tasks return.
6. Try an incorrect login password. Confirm an error appears and login controls become usable again.
7. Request a password reset with an account you own and verify receipt of the email.
8. Rotate the phone while adding a task and check the input remains. Password fields intentionally clear on Activity recreation.

## Limits of this first version

Tasks stay on this device. Clearing app data or uninstalling deletes them; they are not backed up or synced. A Firebase account remains until deleted in Firebase. Login, registration, and reset require internet. An already authenticated user can manage local tasks offline. Task separation is an application-level feature, not encryption; shared-device users should log out. This version does not include notifications, Google sign-in, cloud task storage, email verification enforcement, or account deletion.

The project pins Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20, and compile/target SDK 35 to keep the starter reproducible. These are not claimed to be the latest releases. Target SDK requirements should be checked before a future Play Store release.

## Troubleshooting

- **SDK location not found:** create or fix `local.properties` using the actual SDK location.
- **JAVA_HOME / Java version errors:** select JDK 17, reopen VS Code, and use `.\gradlew.bat --version` to check the JVM.
- **No devices found:** start an emulator or connect an Android phone with USB debugging enabled and authorized.
- **Setup screen still visible:** confirm the Firebase JSON is inside `app/`, then rebuild and reinstall.
- **Login/registration fails:** enable Firebase's Email/Password provider and check the device's internet connection. Firebase Console → Authentication → Users helps confirm account creation.
- **Gradle dependency download fails:** check your internet/proxy settings and retry. Do not install npm packages; this project uses Gradle.

## Validation in the generating environment

The ZIP and XML/JSON configuration were checked structurally. Full APK compilation and device testing require an Android SDK, which was not available in the generating environment. Run the build and manual checklist on your machine before treating this as a tested release.

## Official references

- https://developer.android.com/build/building-cmdline
- https://developer.android.com/build/releases/past-releases/agp-8-9-0-release-notes
- https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler
- https://firebase.google.com/docs/android/setup
- https://firebase.google.com/docs/auth/android/password-auth
