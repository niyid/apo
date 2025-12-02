package com.m2049r.xmrwallet.ledger;

public class Ledger {

    // Ledger device states
    public enum DeviceState {
        DISCONNECTED,
        CONNECTED,
        APP_NOT_OPENED,
        APP_OPENED,
        BUSY,
        ERROR
    }

    // Ledger response codes
    public static final int SW_OK = 0x9000;
    public static final int SW_WRONG_LENGTH = 0x6700;
    public static final int SW_SECURITY_CONDITIONS_NOT_SATISFIED = 0x6982;
    public static final int SW_CONDITIONS_OF_USE_NOT_SATISFIED = 0x6985;
    public static final int SW_WRONG_DATA = 0x6A80;
    public static final int SW_NOT_ALLOWED = 0x6986;
    public static final int SW_APP_NOT_SELECTED = 0x6999;
    public static final int SW_UNKNOWN = 0x6F00;

    private long handle;
    private DeviceState state;
    private String descriptor;

    private Ledger(long handle) {
        this.handle = handle;
        this.state = DeviceState.DISCONNECTED;
    }

    // Device management
    public static native Ledger connect(String descriptor);
    public native boolean disconnect();
    public native boolean isConnected();
    public native DeviceState getDeviceState();
    public native String getDescriptor();
    public native String getDeviceInfo();
    public native String getFirmwareVersion();
    public native boolean openApp();
    public native boolean closeApp();

    // Authentication and setup
    public native boolean checkDevice();
    public native boolean authenticateDevice();
    public native String getPublicAddress(int accountIndex, int addressIndex);
    public native String getPublicViewKey();
    public native String getPublicSpendKey();

    // Transaction operations
    public native boolean initTransaction();
    public native boolean addTransactionInput(String keyImage, int realOutputIndex, String[] outputKeys, String[] outputAmounts);
    public native boolean setTransactionOutput(String address, long amount);
    public native String finalizeTransaction();
    public native String signTransaction(String unsignedTx);
    public native boolean verifyTransaction(String signedTx);

    // Key operations
    public native String exportPrivateViewKey();
    public native boolean importPrivateViewKey(String viewKey);
    public native String[] generateKeyImage(String publicEphemeral, int outputIndex);
    public native boolean verifyKeyImage(String keyImage, String publicEphemeral, int outputIndex);

    // Subaddress operations
    public native String generateSubaddress(int accountIndex, int addressIndex);
    public native boolean verifySubaddress(String address, int accountIndex, int addressIndex);
    public native String[] getSubaddressKeys(int accountIndex, int addressIndex);

    // Multisig operations (if supported)
    public native boolean isMultisigSupported();
    public native String generateMultisigKeys(int threshold, int participants);
    public native String signMultisigTransaction(String multisigTx);

    // Utility operations
    public native boolean testConnection();
    public native int getLastError();
    public native String getLastErrorString();
    public native void clearError();

    // Security operations
    public native boolean lockDevice();
    public native boolean unlockDevice(String pin);
    public native boolean changePin(String oldPin, String newPin);
    public native boolean factoryReset();

    // Firmware update (if supported)
    public native boolean isFirmwareUpdateAvailable();
    public native boolean updateFirmware(String firmwarePath);

    // Device settings
    public native boolean getDisplaySetting();
    public native boolean setDisplaySetting(boolean enabled);
    public native int getTimeout();
    public native boolean setTimeout(int seconds);

    // Raw APDU communication
    public native byte[] exchangeApdu(byte[] apdu);
    public native byte[] sendCommand(byte cla, byte ins, byte p1, byte p2, byte[] data);

    // Event callbacks (to be implemented by application)
    public interface LedgerCallback {
        void onDeviceConnected();
        void onDeviceDisconnected();
        void onDeviceError(int errorCode, String errorMessage);
        void onUserConfirmationRequired(String message);
        void onTransactionSigned(String signedTransaction);
    }

    private LedgerCallback callback;

    public void setCallback(LedgerCallback callback) {
        this.callback = callback;
        setNativeCallback(callback);
    }

    private native void setNativeCallback(LedgerCallback callback);

    // Native callbacks (called from JNI)
    private void onDeviceConnectedNative() {
        if (callback != null) {
            callback.onDeviceConnected();
        }
    }

    private void onDeviceDisconnectedNative() {
        if (callback != null) {
            callback.onDeviceDisconnected();
        }
    }

    private void onDeviceErrorNative(int errorCode, String errorMessage) {
        if (callback != null) {
            callback.onDeviceError(errorCode, errorMessage);
        }
    }

    private void onUserConfirmationRequiredNative(String message) {
        if (callback != null) {
            callback.onUserConfirmationRequired(message);
        }
    }

    private void onTransactionSignedNative(String signedTransaction) {
        if (callback != null) {
            callback.onTransactionSigned(signedTransaction);
        }
    }

    // Discovery and enumeration
    public static native String[] listDevices();
    public static native String[] getDeviceDescriptors();
    public static native boolean isLedgerDevice(String descriptor);

    // Cleanup
    @Override
    protected void finalize() throws Throwable {
        if (handle != 0) {
            disconnect();
        }
        super.finalize();
    }
}
