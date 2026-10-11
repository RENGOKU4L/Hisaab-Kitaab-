# Split Bill 3 — Android Studio Project

A native Android app for splitting expenses among three people.

## Features
- Persistent bill history saved locally on the device using SharedPreferences.
- Rename all three friends.
- Add an expense and choose who paid.
- Equal split among selected people, with rounding handled in paise.
- Custom share amounts for each person; custom amounts must sum to the bill total.
- Delete a single bill or reset all bills.
- Calculate net balances and suggested settlement payments.
- Share settlement text through Android's share sheet (choose WhatsApp if installed).

## Build an APK
1. Install **Android Studio** on a computer.
2. Extract `SplitBill3Android.zip`.
3. In Android Studio, choose **Open** and select the extracted `SplitBill3Android` folder.
4. Allow Gradle sync to complete. Use JDK 17 if Android Studio asks.
5. If Android Studio prompts you to install Android SDK Platform 35 / Build Tools, accept.
6. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
7. When the build finishes, select **locate** in the notification. The APK is usually at:
   `app/build/outputs/apk/debug/app-debug.apk`
8. Transfer the APK to your Android phone and open it. If prompted, allow your file manager/browser to install unknown apps, then install.

## Notes
- This source project is provided; an APK binary is not included because it must be built with Android's SDK/Gradle toolchain.
- No account or internet connection is required for app functionality. Bills and names are stored locally on the phone; uninstalling the app or clearing its storage removes saved data.
- For custom split, enter a share amount for each selected participant. Unselected friends must have zero share. The custom shares must add up exactly to the bill total.
- WhatsApp sharing uses Android's standard share chooser, so you can select WhatsApp or another compatible app.


## Build from your phone using GitHub Actions (no computer required)
This project includes `.github/workflows/android-apk.yml` so GitHub can build the APK in the cloud.

1. Sign in or create an account at https://github.com using your phone browser.
2. Create a **new repository** named `SplitBill3Android`. For easiest setup, choose Public (you can choose Private if you prefer).
3. Upload the *contents* of this folder into the repository, preserving folders and paths. Ensure `.github/workflows/android-apk.yml` is uploaded. If GitHub mobile web makes multi-file upload awkward, use the repository's **Add file → Upload files** option in the browser and select all project files/folders from the ZIP extraction app if supported.
4. Open the repository's **Actions** tab. If prompted, enable workflows.
5. Select **Build Split Bill 3 APK** in the workflow list, tap **Run workflow**, then tap the green **Run workflow** button.
6. Wait for the workflow to finish successfully. Open that completed run and scroll to **Artifacts**.
7. Download `SplitBill3-debug-apk`, then extract the downloaded artifact ZIP. Inside is `app-debug.apk`.
8. Tap the APK to install it on your phone. Android may ask you to allow your browser or file manager to install unknown apps.

Notes:
- GitHub Actions performs the build in the cloud; you do not need Android Studio on your phone.
- GitHub's interface on mobile may be awkward for uploading a complete multi-folder Android project. Use the browser's desktop-site option if needed.
- The workflow artifact is temporary and expires after 14 days; save the APK somewhere safe.
