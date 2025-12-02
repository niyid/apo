package com.m2049r.xmrwallet.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class TransactionHistory {
    private long handle;

    int accountIndex;

    public void setAccountFor(Wallet wallet) {
        if (accountIndex != wallet.accountIndex) {
            this.accountIndex = wallet.accountIndex;
            refreshWithNotes(wallet);
        }
    }

    public TransactionHistory(long handle, int accountIndex) {
        this.handle = handle;
        this.accountIndex = accountIndex;
    }

    private void loadNotes(Wallet wallet) {
        for (TransactionInfo info : transactions) {
            info.notes = wallet.getUserNote(info.hash);
        }
    }

    public native int getCount(); // over all accounts/subaddresses

    //private native long getTransactionByIndexJ(int i);

    //private native long getTransactionByIdJ(String id);

    public List<TransactionInfo> getAll() {
        return transactions;
    }

    private List<TransactionInfo> transactions = new ArrayList<>();

    public void refreshWithNotes(Wallet wallet) {
        refresh();
        loadNotes(wallet);
    }

    public void refresh() {
        transactions = refreshJ(accountIndex);
    }

    private native List<TransactionInfo> refreshJ(int accountIndex);
}
