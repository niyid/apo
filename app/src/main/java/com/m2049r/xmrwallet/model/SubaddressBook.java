package com.m2049r.xmrwallet.model;

import java.util.List;

public class SubaddressBook {

    private long handle;

    public SubaddressBook(long handle) {
        this.handle = handle;
    }

    public native List<Subaddress> getAll();
    public native void addRow(int accountIndex, String address, String description);
    public native boolean deleteRow(int rowId);
    public native void refresh();
    public native String errorString();
    public native int lookupPaymentID(String paymentId);

    @Override
    protected void finalize() throws Throwable {
        // Handle cleanup handled by wallet
        super.finalize();
    }
}
