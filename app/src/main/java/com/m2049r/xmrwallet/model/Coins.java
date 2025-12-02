package com.m2049r.xmrwallet.model;

import java.util.List;

public class Coins {

    private long handle;

    public Coins(long handle) {
        this.handle = handle;
    }

    public native int count();
    public native List<CoinsInfo> getAll();
    public native CoinsInfo coin(int index);
    public native void refresh();
    public native void setFrozen(int index);
    public native void thaw(int index);
    public native boolean isTransferUnlocked(int index);

    @Override
    protected void finalize() throws Throwable {
        // Handle cleanup handled by wallet
        super.finalize();
    }
}
