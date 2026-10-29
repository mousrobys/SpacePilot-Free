# SpacePilot Free Privacy Policy

**Effective date:** October 29, 2026
**App version:** 1.2.0
**Developer:** mousrobys
**Contact:** fandiplegend@gmail.com

This policy explains how SpacePilot Free ("the app", package `com.spacepilot.free`) handles data on the user's device.

## Summary

- The app works on the device. Internet access is used only for an optional cloud APK lookup by SHA-256, which is off by default.
- The app does not collect, transmit, or sell personal data.
- No accounts, ads, analytics, telemetry, or third-party advertising/tracking SDKs.
- Settings, history, and scan cache are stored only on the device, in the app's private storage.
- The user explicitly selects the folder to access and confirms every deletion.

## 1. Data processed

The app accesses file information only as needed for its features:

- file name, size, modification date, MIME type, path or URI;
- file contents — only to compute a SHA-256 checksum when finding duplicates, inspecting an APK, or when the user taps "Calculate SHA-256";
- APK metadata — package name, version, min/target SDK, app label, signature, permissions, and exported components;
- device storage capacity (total, used, free);
- the folder selected by the user via the Storage Access Framework (URI).

All processing happens locally on the device. The only exception is the optional cloud APK lookup: when it is enabled and a user-supplied API key is present, only the SHA-256 hash of the inspected file is sent to VirusTotal; the file itself, its name, and its path are not.

## 2. Permissions and access

The app requests:

- `READ_EXTERNAL_STORAGE` — Android 8–12L only, to read media (images, video, audio).
- `WRITE_EXTERNAL_STORAGE` — Android 8–9 only, for media file operations.
- `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO` — Android 13+, for the corresponding media collections.
- `INTERNET` — only for the optional cloud APK lookup by SHA-256 via VirusTotal with the user's own key. The feature is off by default; no requests are made without a key.

The app does **not** request `MANAGE_EXTERNAL_STORAGE`, contacts, SMS, location, camera, or microphone access.

The widget library (Glance) adds the transitive permissions `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `RECEIVE_BOOT_COMPLETED`, and `FOREGROUND_SERVICE` from `androidx.work`. The app's own code starts no background services and does not use autostart; these permissions are not used to collect data.

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
- a temporary APK copy for metadata inspection — in the app cache, deleted after analysis;
- the VirusTotal API key — only if the user enters one; stored in private `SharedPreferences` and removable in settings.

This data never leaves the device. The operations log and scan cache are removed by "Clear history and cache"; theme and language settings are reset separately. All local data is also removed when the user clears app data in Android settings or uninstalls the app.

## 5. Third parties

The app does not share data with third parties, except for the optional cloud APK lookup. If the user enables it and provides their own API key, the app sends only the SHA-256 hash of the inspected file to VirusTotal (`virustotal.com`) to retrieve public detection statistics. The request uses HTTPS and the key is sent in a request header. VirusTotal's policy is available on its website. The feature can be disabled and the key removed in the app settings.

The app has no backend, cloud sync, ad networks, or third-party data-collecting SDKs.

If the user opens a file or starts an APK installation, control is handed to another app through the Android Intent mechanism. That app may have its own privacy policy; SpacePilot Free does not control its actions.

## 6. File deletion

Files are deleted only after the user confirms. For folders selected through the Storage Access Framework, the system document deletion API is used. For media on Android 11+, the system deletion confirmation dialog is shown. The app does not bypass Android restrictions or delete files silently.

Deleted files are not moved to a recycle bin and cannot be restored by the app.

## 7. Security

The app minimizes the data it processes. File access is limited by Android permissions and the folder chosen by the user; network requests are possible only as part of the optional cloud APK lookup. No device or software can guarantee absolute security; the user installs the app and works with files at their own risk.

## 8. Children

The app is not designed to collect data about children and does not collect personal data from users of any age. Age rating: 6+.

## 9. Changes

This policy may be updated when app features change. The current version is always published at the URL listed in the store listing. The effective date is shown at the top.

## 10. Contact

Privacy questions: fandiplegend@gmail.com.

---

_This document is current for version 1.2.0 and must be hosted at a public URL._
