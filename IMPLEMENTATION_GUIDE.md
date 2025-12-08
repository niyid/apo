# Implementation Guide for TODO Methods

This guide shows you how to implement the missing wallet methods in your WalletSuite class.

## 1. Get View Key

Add this method to your `WalletSuite` class:

```kotlin
/**
 * Get the wallet's view key
 * @return The secret view key as a string
 */
fun getViewKey(): String {
    if (!isReady || wallet == null) {
        throw IllegalStateException("Wallet not ready")
    }
    return wallet!!.secretViewKey
}
```

## 2. Get Spend Key

Add this method to your `WalletSuite` class:

```kotlin
/**
 * Get the wallet's spend key
 * WARNING: This exposes the private spend key - handle with extreme care!
 * @return The secret spend key as a string
 */
fun getSpendKey(): String {
    if (!isReady || wallet == null) {
        throw IllegalStateException("Wallet not ready")
    }
    return wallet!!.secretSpendKey
}
```

## 3. Get Seed Phrase

Add this method to your `WalletSuite` class:

```kotlin
/**
 * Get the wallet's mnemonic seed phrase
 * WARNING: This exposes the mnemonic seed - never share this!
 * @return The 25-word seed phrase as a string, or null if not available
 */
fun getSeed(): String? {
    if (!isReady || wallet == null) {
        throw IllegalStateException("Wallet not ready")
    }
    
    // The seed method returns the mnemonic seed phrase
    // Returns empty string if wallet is view-only
    val seed = wallet!!.seed
    
    if (seed.isNullOrEmpty()) {
        return null
    }
    
    return seed
}
```

## 4. Update the ExportKeysDialog

Replace the TODO lines in `ExportKeysDialog`:

```kotlin
// OLD:
val viewKey = remember { "TODO: Implement walletSuite.getViewKey()" }
val spendKey = remember { "TODO: Implement walletSuite.getSpendKey()" }

// NEW:
val viewKey = remember { 
    try {
        walletSuite.getViewKey()
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}
val spendKey = remember { 
    try {
        walletSuite.getSpendKey()
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}
```

## 5. Update the SeedPhraseDialog

Replace the TODO line in `SeedPhraseDialog`:

```kotlin
// OLD:
val seedPhrase = remember { 
    try {
        // TODO: Implement walletSuite.getSeed() method
        "Seed phrase retrieval not yet implemented"
    } catch (e: Exception) {
        "Error retrieving seed: ${e.message}"
    }
}

// NEW:
val seedPhrase = remember { 
    try {
        walletSuite.getSeed() ?: "View-only wallet - no seed available"
    } catch (e: Exception) {
        "Error retrieving seed: ${e.message}"
    }
}
```

## Security Considerations

1. **Never log these values** - They give complete access to the wallet
2. **Never send over network** - Unless encrypted with user's explicit consent
3. **Never store in plaintext** - If you must persist, encrypt properly
4. **Warn users** - Always display warnings when showing sensitive keys
5. **Clipboard safety** - Clear clipboard after a timeout when copying keys

## Testing

After implementation, test:

1. Export keys dialog shows actual keys (not TODOs)
2. Seed phrase dialog shows actual 25-word phrase
3. Copy functionality works for all keys
4. Warning messages are displayed
5. Error handling works when wallet is not ready

## Additional Features (Optional)

Consider adding:

1. **QR Code generation** for keys/seed
2. **Export to file** with encryption
3. **Biometric authentication** before showing keys
4. **Screenshot prevention** when displaying sensitive data
5. **Time-limited display** that auto-closes after X seconds

---

**Remember**: These keys provide complete access to the wallet funds. 
Handle them with the same care as you would physical cash!
