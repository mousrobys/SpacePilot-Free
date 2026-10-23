# SpacePilot Free Privacy Policy

**Effective date:** October 23, 2026
**App version:** 1.0.0
**Developer:** _to be filled in_
**Contact:** _to be filled in_

This policy explains how SpacePilot Free ("the app", package `com.spacepilot.free`) handles data on the user's device.

## Summary

- The app works fully offline and **has no INTERNET permission**.
- The app does not collect, transmit, or sell personal data.
- No accounts, ads, analytics, telemetry, or third-party advertising/tracking SDKs.
- Settings, history, and scan cache are stored only on the device, in the app's private storage.
- The user explicitly selects the folder to access and confirms every deletion.

## 1. Data processed

The app accesses file information only as needed for its features:

- file name, size, modification date, MIME type, path or URI;
- file contents — only to compute a SHA-256 checksum when finding duplicates or when the user taps "Calculate SHA-256";
- APK metadata — package name, version, min/target SDK, app label;
- device storage capacity (total, used, free);
- the folder selected by the user via the Storage Access Framework (URI).

All processing happens locally on the device. Nothing is sent anywhere.

## 2. Permissions and access

The app requests:

- `READ_EXTERNAL_STORAGE` — Android 8–12L only, to read media (images, video, audio).
- `WRITE_EXTERNAL_STORAGE` — Android 8–9 only, for media file operations.
- `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO` — Android 13+, for the corresponding media collections.

The app does **not** request `INTERNET`, `MANAGE_EXTERNAL_STORAGE`, contacts, SMS, location, camera, or microphone access.

Access to an arbitrary folder is granted manually by the user through the Android Storage Access Framework dialog. The app can access only the selected folder and its contents. The user can revoke this access in the app or in Android settings.

## 3. How data is used

Data is used solely to:

- show used space and file categories;
- find large files and duplicates;
- browse the selected folder, search, copy, and move files;
- display file and APK details;
- delete files after explicit user confirmation;
- remember interface settings and the last scan result.

The app does not use data for advertising, profiling, analytics, or automated decision-making.

## 4. Local storage

The app stores on the device:

- settings: theme, language, selected folder URI — in private `SharedPreferences`;
- recent operations log (up to 25 records: type, time, description, size) — in private `SharedPreferences`;
- last scan cache (names, paths/URIs, sizes, dates, categories, up to 40,000 records) — in a compressed JSON file in the app's private storage;
- a temporary APK copy for metadata inspection — in the app cache, deleted after analysis.

This data never leaves the device. The operations log and scan cache are removed by "Clear history and cache"; theme and language settings are reset separately. All local data is also removed when the user clears app data in Android settings or uninstalls the app.

## 5. Third parties

The app does not share data with third parties. It has no backend, cloud sync, ad networks, or third-party data-collecting SDKs.

If the user opens a file or starts an APK installation, control is handed to another app through the Android Intent mechanism. That app may have its own privacy policy; SpacePilot Free does not control its actions.

## 6. File deletion

Files are deleted only after the user confirms. For folders selected through the Storage Access Framework, the system document deletion API is used. For media on Android 11+, the system deletion confirmation dialog is shown. The app does not bypass Android restrictions or delete files silently.

Deleted files are not moved to a recycle bin and cannot be restored by the app.

## 7. Security

The app minimizes the data it processes and has no network access. File access is limited by Android permissions and the folder chosen by the user. No device or software can guarantee absolute security; the user installs the app and works with files at their own risk.

## 8. Children

The app is not designed to collect data about children and does not collect personal data from users of any age. Age rating: 6+.

## 9. Changes

This policy may be updated when app features change. The current version is always published at the URL listed in the store listing. The effective date is shown at the top.

## 10. Contact

Privacy questions: _developer e-mail to be filled in_.

---

_Before publishing, replace the italicized placeholders with real developer details and host this document at a public URL._
