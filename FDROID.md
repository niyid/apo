# F-Droid Build Instructions

This is the F-Droid compatible version of Àpò.

## Building

```bash
./gradlew assembleFdroidRelease
```

## Differences from Play Store version

- No Firebase Analytics or Crashlytics
- Uses ZXing instead of ML Kit for barcode scanning
- No proprietary Google dependencies

## Testing

```bash
./gradlew assembleFdroidDebug
adb install app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk
```
