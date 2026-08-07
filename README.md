# homecheck

homecheck is a private, offline-first Android app for keeping appliance and home-equipment details, documents, warranties, and maintenance together. It has no account, ads, analytics, or homecheck cloud service.

## Stack and structure

- Kotlin, Jetpack Compose, Material 3, Navigation Compose, Hilt, and WorkManager
- Room for structured records and app-private storage for imported photos and documents
- DataStore for appearance, reminders, onboarding, and the last successfully verified premium entitlement
- Google Play Billing for one lifetime in-app product

Production code is grouped under `data`, `domain`, `backup`, `billing`, `notifications`, and `ui`. Unit tests cover business rules and backup serialization. Instrumented tests cover Room transactions, internal files, backup restore, and key Compose states.

## Local data and backup

All home records stay on the device. Imported attachments are copied into app-controlled storage and shared only through short-lived `FileProvider` grants. Android system backup is disabled.

Settings can export or restore a versioned ZIP through the Storage Access Framework. `manifest.json` contains schema version 1 and the structured records; attachment bytes live under `attachments/`. Restore validates the schema, relationships, dates, enum values, file count, sizes, duplicate paths, and ZIP paths in a staging directory before replacing current data. If replacement fails, the previous database and attachments are restored.

## Google Play Billing

Create a one-time in-app product in Play Console with the exact ID:

`homecheck_premium_lifetime`

Activate the product, add localized price and listing data, then test with a signed build uploaded to an internal test track and a licensed tester account. The app always displays the localized price returned by Google Play; no price is hard-coded. A release build must use your normal private signing configuration, which is intentionally not committed here.

## Build and test

Use JDK 17 or newer and an Android SDK containing API 37:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat lintDebug
.\gradlew.bat connectedDebugAndroidTest  # when a device or emulator is connected
```

Open the root folder in Android Studio to run and inspect Compose previews. Before publishing, configure release signing, Play App Signing, the billing product, store listing, screenshots, and the privacy-policy URL.
