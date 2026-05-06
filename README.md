# LaunchCal

A minimal Android launcher with a built-in calendar agenda view.

## Features

- App list with accent-insensitive search
- Calendar agenda view (swipe left) showing upcoming 7 days
- Quick-add events from agenda with + button
- Bottom dock with phone, browser, and camera shortcuts
- Auto-refreshes app list on install/uninstall

## Building

Requires JDK 17.

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Permissions

- `READ_CALENDAR` — agenda view
- `QUERY_ALL_PACKAGES` — full app list
