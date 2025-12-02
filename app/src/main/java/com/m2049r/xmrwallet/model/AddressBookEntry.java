package com.m2049r.xmrwallet.model;

/**
 * Represents a Monero Address Book Entry backed by native code.
 */
public class AddressBookEntry {

    // Pointer to native C++ object
    private long handle;

    /* === Native-backed getters === */
    public native int getIndex();
    public native String getAddress();
    public native String getDescription();
    public native String getPaymentId();

    // Called by JNI to free the native C++ object
    private native void dispose();

    @Override
    protected void finalize() throws Throwable {
        try {
            dispose();
        } finally {
            super.finalize();
        }
    }

    /* === Constructors for Java-side use === */
    public AddressBookEntry(int index, String address, String description, String paymentId) {
        this.handle = 0; // No native link, pure Java
        this.index = index;
        this.address = address;
        this.description = description;
        this.paymentId = paymentId;
    }

    // Fallback fields for when the object is purely Java-created
    private final int index;
    private final String address;
    private final String description;
    private final String paymentId;

    /* === Safe getters with fallback === */
    public int getIndexSafe() {
        return (handle != 0) ? getIndex() : index;
    }

    public String getAddressSafe() {
        return (handle != 0) ? getAddress() : address;
    }

    public String getDescriptionSafe() {
        return (handle != 0) ? getDescription() : description;
    }

    public String getPaymentIdSafe() {
        return (handle != 0) ? getPaymentId() : paymentId;
    }

    @Override
    public String toString() {
        return "AddressBookEntry{" +
                "index=" + getIndexSafe() +
                ", address='" + getAddressSafe() + '\'' +
                ", description='" + getDescriptionSafe() + '\'' +
                ", paymentId='" + getPaymentIdSafe() + '\'' +
                '}';
    }
}

