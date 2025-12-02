package com.m2049r.xmrwallet.model;

public class Subaddress {

    private long handle;

    public Subaddress(long handle) {
        this.handle = handle;
    }

    public native String getAddress();
    public native String getLabel();
    public native int getAddressIndex();
    public native boolean isUsed();

    @Override
    protected void finalize() throws Throwable {
        // Handle cleanup handled by wallet
        super.finalize();
    }
}
