package com.m2049r.xmrwallet.model;

import lombok.Getter;
import lombok.Setter;

public class PendingTransaction {
    
    public long handle;
    
    PendingTransaction(long handle) {
        this.handle = handle;
    }
    
    public enum Status {
        Status_Ok(0),
        Status_Error(1),
        Status_Critical(2);
        
        private final int value;
        
        Status(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static Status fromInteger(int x) {
            switch (x) {
                case 0:
                    return Status_Ok;
                case 1:
                    return Status_Error;
                case 2:
                    return Status_Critical;
                default:
                    return Status_Error;
            }
        }
    }
    
    public enum Priority {
        Priority_Default(0),
        Priority_Low(1),
        Priority_Medium(2),
        Priority_High(3),
        Priority_Last(4);
        
        private final int value;
        
        Priority(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        public static Priority fromInteger(int x) {
            switch (x) {
                case 0:
                    return Priority_Default;
                case 1:
                    return Priority_Low;
                case 2:
                    return Priority_Medium;
                case 3:
                    return Priority_High;
                case 4:
                    return Priority_Last;
                default:
                    return Priority_Default;
            }
        }
    }
    
    public Status getStatus() {
        return Status.values()[getStatusJ()];
    }
    
    public native int getStatusJ();
    
    public native String getErrorString();
    
    // commit transaction or save to file if filename is provided.
    public native boolean commit(String filename, boolean overwrite);
    
    public native long getAmount();
    
    public native long getDust();
    
    public native long getFee();
    
    public String getFirstTxId() {
        String id = getFirstTxIdJ();
        if (id == null)
            throw new IndexOutOfBoundsException();
        return id;
    }
    
    public native String getFirstTxIdJ();
    
    public native long getTxCount();
    
    @Getter
    @Setter
    private long pocketChange;
    
    public long getNetAmount() {
        return getAmount() - pocketChange;
    }
}
