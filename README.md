# Apo - Monero Wallet for Android

A native Android Monero wallet application built with Kotlin and Jetpack Compose.

## Features

- ✅ Create and restore Monero wallets
- ✅ Send and receive XMR transactions
- ✅ Real-time blockchain synchronization
- ✅ Transaction history with search
- ✅ Blockchain rescan functionality
- ✅ QR code support for addresses
- ✅ Modern Material Design 3 UI

## Technology Stack

- **Language**: Kotlin + Java
- **UI**: Jetpack Compose with Material Design 3
- **Wallet**: Monero C++ libraries (via JNI)
- **Architecture**: MVVM with coroutines

## Building

1. Clone the repository
2. Add `wallet.properties` configuration file
3. Build with Android Studio or Gradle

```bash
./gradlew assembleDebug
```

## Security

⚠️ **IMPORTANT**: Never commit wallet files, keys, or seed phrases to version control!

## License

MIT
