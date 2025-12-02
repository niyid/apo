package com.m2049r.xmrwallet.model;

public class AddressBookRow {

    private long handle;

    public AddressBookRow(long handle) {
        this.handle = handle;
    }

    public native String getAddress();
    public native String getPaymentId();
    public native String getDescription();
    public native int getRowId();

    @Override
    protected void finalize() throws Throwable {
        // Handle cleanup handled by address book
        super.finalize();
    }
}
