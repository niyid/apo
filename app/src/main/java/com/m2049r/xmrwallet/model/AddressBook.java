package com.m2049r.xmrwallet.model;

import java.util.List;

public class AddressBook {

    private long handle;

    public AddressBook(long handle) {
        this.handle = handle;
    }

    public native List<AddressBookRow> getAll();
    public native boolean addRow(String address, String paymentId, String description);
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
