package com.m2049r.xmrwallet.model;
import com.m2049r.xmrwallet.data.TxData;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JNI-backed Wallet wrapper.
 * - Uses native methods (declared here) to interact with the C++ wallet implementation.
 * - Provides an async rescan helper that starts native async rescan and notifies via RescanCallback
 *   once refreshed() is delivered by the WalletListener.
 *
 * Note: This class intentionally does NOT declare a WalletListener interface inside it.
 *       It expects a package-level WalletListener (com.m2049r.xmrwallet.model.WalletListener)
 *       to exist elsewhere in the project so wallet.setListener(...) calls use the same type.
 */
public class Wallet {
    final static public long SWEEP_ALL = Long.MAX_VALUE;
    private static final String TAG = "com.bitchat.Wallet";
    private static final long RESCAN_TIMEOUT_MS = 10 * 60 * 1000L;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    public enum Status {
        Status_Ok,
        Status_Error,
        Status_Critical
    }

    public enum ConnectionStatus {
        ConnectionStatus_Disconnected,
        ConnectionStatus_Connected,
        ConnectionStatus_WrongVersion
    }

    public enum Device {
        Device_Software(0, "Software"),
        Device_Ledger(1, "Ledger");

        private final int value;
        private final String name;

        Device(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int getValue() { return value; }
        public String getName() { return name; }

        public static Device fromValue(int value) {
            for (Device device : Device.values()) {
                if (device.value == value) return device;
            }
            return Device_Software;
        }
    }

    /* --- instance fields --- */
    boolean synced = false;
    public int accountIndex = 0;
    private String lastErrorString = null;
    private long handle;
    private long listenerHandle;
    private WalletListener listener; // expects package-level WalletListener

    public Wallet(long handle) {
        this.handle = handle;
    }

    /* ------------------------------------------------------------------------
     * Native method declarations
     * --------------------------------------------------------------------- */
     
    public static native boolean walletExists(String path);
    public static native Wallet openWallet(String path, String password, int networkType);
    public static native Wallet createWalletJ(String path, String password, String language, int networkType, long restoreHeight);
    public static native Wallet recoveryWallet(String path, String password, String mnemonic, int networkType, long restoreHeight);
    public static native Wallet createWalletFromKeys(String path, String password, String language, int networkType, long restoreHeight, String address, String viewKey, String spendKey);
    public static native Wallet createWalletFromDevice(String path, String password, int networkType, String deviceName, long restoreHeight, String subaddressLookahead);

    public native String getSeed();
    public native String getSeed(String seedOffset);
    public native String getSeedLanguage();
    public native void setSeedLanguage(String language);
    public native int getStatusJ();
    private native int statusWithErrorString(int[] outStatus, String[] outError);
    public native boolean setPassword(String password);
    private native String getAddressJ(int accountIndex, int addressIndex);
    public native String getPath();
    public native int getNetworkType();
    public native String getSecretViewKey();
    public native String getSecretSpendKey();
    public native String getPublicViewKey();
    public native String getPublicSpendKey();
    public native long getBalance();
    public native long getBalance(int accountIndex);
    public native long getUnlockedBalance();
    public native long getUnlockedBalance(int accountIndex);
    public native long getBlockChainHeight();
    public native long getApproximateBlockChainHeight();
    public native long getDaemonBlockChainHeight();
    public native long getDaemonBlockChainTargetHeight();
    public native boolean isSynchronized();
    public native String getDisplayAmount(long amount);
    public native long getAmountFromString(String amount);
    public static native long getAmountFromDouble(double amount);
    public native void setRefreshFromBlockHeight(long height);
    public native long getRefreshFromBlockHeight();
    public native void setRestoreHeight(long height);
    public native long getRestoreHeight();

    public native Device getDeviceType();
    public native boolean setDeviceType(Device device);
    public native boolean isHardwareWallet();

    public native boolean refresh();
    public native void refreshAsync();
    public native void rescanSpent();
    public native void startRefresh();
    public native void pauseRefresh();
    public native boolean isRefreshing();


    private PendingTransaction pendingTransaction = null;

    public PendingTransaction getPendingTransaction() {
        return pendingTransaction;
    }

    public void disposePendingTransaction() {
        if (pendingTransaction != null) {
            disposeTransaction(pendingTransaction);
            pendingTransaction = null;
        }
    }

    private native long createTransactionMultDest(String[] destinations, String payment_id, long[] amounts, int mixin_count, int priority, int accountIndex, int[] subaddresses);

    public PendingTransaction createTransaction(TxData txData) {
        disposePendingTransaction();
        int _priority = txData.getPriority().getValue();
        final boolean sweepAll = txData.getAmount() == SWEEP_ALL;
        long txHandle = (sweepAll ? createSweepTransaction(txData.getDestination(), "", txData.getMixin(), _priority, accountIndex) :
                createTransactionMultDest(txData.getDestinations(), "", txData.getAmounts(), txData.getMixin(), _priority, accountIndex, txData.getSubaddresses()));
        pendingTransaction = new PendingTransaction(txHandle);
        return pendingTransaction;
    }

    private native long createTransactionJ(String dst_addr, String payment_id, long amount, int mixin_count, int priority, int accountIndex);
    
    public PendingTransaction createTransaction(String dst_addr, String payment_id, long amount, int mixin_count, int priority, int accountIndex) {
        long txHandle = createTransactionJ(dst_addr, payment_id, amount, mixin_count, priority, accountIndex);
        PendingTransaction pendingTransaction = new PendingTransaction(txHandle);
        return pendingTransaction;
    }    

    private native long createSweepTransaction(String dst_addr, String payment_id, int mixin_count, int priority, int accountIndex);

    public native PendingTransaction createSweepUnmixableTransaction();
    public native void disposeTransaction(PendingTransaction pendingTransaction);
    public native boolean setUserNote(String txid, String note);
    public native String getUserNote(String txid);
    public native String getTxKey(String txid);
    public native String checkTxKey(String txid, String txKey, String address);
    public native String getTxProof(String txid, String address, String message);
    public native boolean checkTxProof(String txid, String address, String message, String signature);
    public native String getSpendProof(String txid, String message);
    public native boolean checkSpendProof(String txid, String message, String signature);
    public native String getReserveProof(boolean all, int accountIndex, long amount, String message);
    public native boolean checkReserveProof(String address, String message, String signature);

    public native Subaddress getSubaddress(int accountIndex, int addressIndex);
    public native SubaddressBook getSubaddressBook();
    public native String addSubaddress(int accountIndex, String label);

    public native void addAccount(String label);
    public native String getSubaddressLabel(int accountIndex, int addressIndex);
    public native void setSubaddressLabel(int accountIndex, int addressIndex, String label);
    public native int getNumAccounts();
    public native int getNumSubaddresses(int accountIndex);
    public native String getAccountLabel();
    public native String getAccountLabel(int accountIndex);
    public native void setAccountLabel(int accountIndex, String label);

    public native AddressBook getAddressBook();

    public native boolean connectToDaemon();
    private native int getConnectionStatusJ();
    public native boolean setTrustedDaemon(boolean arg);
    public native boolean isTrustedDaemon();
    public native long getDaemonConnectionTimeout();
    public native void setDaemonConnectionTimeout(long timeout);

    public native long initJ(String daemonAddress,
                             long upperTransactionLimit,
                             String daemonUsername,
                             String daemonPassword,
                             boolean ssl,
                             boolean lightWallet,
                             String proxy);

    public native synchronized boolean store(String path);
    public native String getFilename();
    public native void rescanBlockchainAsyncJ();

    public native String exportKeyImages();
    public native int importKeyImages(String keyImages);

    public native String exportOutputs();
    public native int importOutputs(String outputs);

    public native String getMultisigInfo();
    public native String makeMultisig(String multisigInfo, int threshold);
    public native boolean finalizeMultisig(String multisigInfo);
    public native boolean isMultisig();
    public native String signMultisigTxHex(String multisigTxHex);
    public native String[] submitMultisigTxHex(String multisigTxHex);
    public native String submitTransaction(String hex);
    
    public long estimateTransactionFee(TxData txData) {
        return estimateTransactionFee(txData.getDestinations(), txData.getAmounts(), txData.getPriority().getValue());
    }

    private native long estimateTransactionFee(String[] destinations, long[] amounts, int priority);
    

    public native void setListenerJ(WalletListener listener);

    public native int getDefaultMixin();
    public native void setDefaultMixin(int mixin);
    public native boolean setAutoRefreshInterval(int millis);
    public native int getAutoRefreshInterval();

    public static native boolean paymentIdValid(String paymentId);
    public static native boolean addressValid(String address, int networkType);
    public static native boolean keyValid(String key);
    public static native String getPaymentIdFromAddress(String address, int networkType);
    public static native long getMaximumAllowedAmount();
    public static native void printConnections();
    public static native boolean isKeyImageSpent(String keyImage);
    private native boolean isWatchOnly();

    public native void setLogLevel(int level);
    public native void setLogCategories(String categories);

    private native long getHistoryJ();

    /* ---------------------------------------------------------------------
     * Convenience wrappers
     * ------------------------------------------------------------------ */
     
    public boolean isReadOnly() {
        return isWatchOnly();
    }     

    private TransactionHistory history = null;

    public TransactionHistory getHistory() {
        if (history == null) {
            history = new TransactionHistory(getHistoryJ(), accountIndex);
        }
        return history;
    }
    
    public boolean isConnected() {
        return ConnectionStatus.ConnectionStatus_Connected == ConnectionStatus.values()[getConnectionStatusJ()];
    }

    public void refreshHistory() {
        getHistory().refreshWithNotes(this);
    }     
     
    public int getStatus() {
        return getStatusJ();
    }

    public ConnectionStatus getConnectionStatus() {
        int s = getConnectionStatusJ();
        return ConnectionStatus.values()[s];
    }

    public String getAddress() {
        return getAddress(accountIndex);
    }

    public String getAddress(int accountIndex) {
        return getAddressJ(accountIndex, 0);
    }

    public String getSubaddress(int addressIndex) {
        return getAddressJ(accountIndex, addressIndex);
    }

    /* ---------------------------------------------------------------------
     * Rescan helpers
     * ------------------------------------------------------------------ */

    /**
     * Start the native async rescan. Returns true if the native async call
     * was successfully invoked (does not indicate completion).
     */
    public boolean rescanBlockchainAsync() {
        synced = false;
        try {
            rescanBlockchainAsyncJ(); // native, void
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "Failed to start native async rescan", t);
            return false;
        }
    }

    /**
     * Start native async rescan, and notify the provided callback when a refreshed()
     * event is received from the wallet listener (or call onError() on timeout / failure).
     */
    public void rescanBlockchainAsync(final RescanCallback callback) {
        if (!isInitialized()) {
            if (callback != null) callback.onError("Wallet not initialized");
            return;
        }

        boolean started = rescanBlockchainAsync();
        if (!started) {
            if (callback != null) callback.onError("Failed to start native async rescan");
            return;
        }

        if (callback == null) return;

        final WalletListener previousListener;
        synchronized (this) {
            previousListener = this.listener;
        }

        final AtomicBoolean callbackFired = new AtomicBoolean(false);

        WalletListener wrapper = new WalletListener() {
            @Override
            public void moneySent(String txId, long amount) {
                if (previousListener != null) previousListener.moneySent(txId, amount);
            }

            @Override
            public void moneyReceived(String txId, long amount) {
                if (previousListener != null) previousListener.moneyReceived(txId, amount);
            }

            @Override
            public void unconfirmedMoneyReceived(String txId, long amount) {
                if (previousListener != null) previousListener.unconfirmedMoneyReceived(txId, amount);
            }

            @Override
            public void refreshed() {
                if (previousListener != null) previousListener.refreshed();
                if (callbackFired.compareAndSet(false, true)) {
                    try {
                        callback.onSuccess();
                    } catch (Throwable t) {
                        Log.w(TAG, "Rescan callback onSuccess threw", t);
                    } finally {
                        try {
                            setListener(previousListener);
                        } catch (Throwable t) {
                            Log.w(TAG, "Failed to restore previous listener", t);
                        }
                    }
                }
            }

            @Override
            public void newBlock(long height) {
                if (previousListener != null) previousListener.newBlock(height);
            }

            @Override
            public void updated() {
                if (previousListener != null) previousListener.updated();
            }
        };

        try {
            setListener(wrapper);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to set wrapper listener", t);
            try { setListener(previousListener); } catch (Throwable ignored) {}
            if (callback != null) callback.onError("Failed to attach listener for rescan monitoring");
            return;
        }

        new Thread(() -> {
            try {
                Thread.sleep(RESCAN_TIMEOUT_MS);
                if (callbackFired.compareAndSet(false, true)) {
                    try { setListener(previousListener); } catch (Throwable ignored) {}
                    if (callback != null) callback.onError("Rescan timed out after " + (RESCAN_TIMEOUT_MS / 1000) + "s");
                }
            } catch (InterruptedException ignored) {}
        }, "rescan-timeout-thread").start();
    }

    /* ---------------------------------------------------------------------
     * Callback interface (unique to Wallet)
     * ------------------------------------------------------------------ */
    public interface RescanCallback {
        void onSuccess();
        void onError(String error);
    }

    /* ---------------------------------------------------------------------
     * API for listener plumbing - uses package-level WalletListener type
     * ------------------------------------------------------------------ */
    public void setListener(WalletListener listener) {
        this.listener = listener;
        setListenerJ(listener);
    }

    /* ---------------------------------------------------------------------
     * Helpers & smaller APIs
     * ------------------------------------------------------------------ */
    private boolean isInitialized() {
        return handle != 0;
    }

    public int getLastStatus() {
        int[] outStatus = new int[1];
        String[] outError = new String[1];
        statusWithErrorString(outStatus, outError);
        lastErrorString = outError[0];
        return outStatus[0];
    }

    public String getErrorString() {
        return lastErrorString;
    }

    /** Proxy wrapper for initJ that hides the "proxy" parameter for most cases. */
    public long init(String daemonAddress,
                     long upperTransactionLimit,
                     String daemonUsername,
                     String daemonPassword,
                     boolean ssl,
                     boolean lightWallet) {
        return initJ(daemonAddress, upperTransactionLimit, daemonUsername, daemonPassword, ssl, lightWallet, "");
    }

    public boolean store() {
        return store(getFilename());
    }

    @Override
    protected void finalize() throws Throwable {
        if (handle != 0 && !isClosed.get()) {
            Log.d(TAG, "Finalizer closing wallet handle: " + handle);
            close();
        }
        super.finalize();
    }

    // Modify the close() method
    public boolean close() {
        if (isClosed.compareAndSet(false, true)) {
            return WalletManager.getInstance().close(this);
        }
        Log.d(TAG, "Wallet already closed, skipping");
        return true; // Already closed
    }    
}

