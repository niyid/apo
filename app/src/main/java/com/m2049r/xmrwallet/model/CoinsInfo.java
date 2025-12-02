package com.m2049r.xmrwallet.model;

public class CoinsInfo {

    private long handle;

    public CoinsInfo(long handle) {
        this.handle = handle;
    }

    public native long getBlockHeight();
    public native String getHash();
    public native long getInternalOutputIndex();
    public native long getGlobalOutputIndex();
    public native boolean getSpent();
    public native boolean getFrozen();
    public native String getSpentHeight();
    public native long getAmount();
    public native boolean getRct();
    public native String getKeyImage();
    public native long getUnlockTime();
    public native boolean getUnlocked();
    public native String getPubKey();
    public native int getSubaddrAccount();
    public native int getSubaddrIndex();

    @Override
    protected void finalize() throws Throwable {
        // Handle cleanup handled by coins
        super.finalize();
    }
}
