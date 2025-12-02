package com.m2049r.xmrwallet.model;

public interface WalletListener {
    /**
     * Called when money is received
     * @param txId transaction id
     * @param amount amount received in atomic units
     */
    void moneyReceived(String txId, long amount);

    /**
     * Called when money is sent
     * @param txId transaction id
     * @param amount amount sent in atomic units
     */
    void moneySent(String txId, long amount);

    /**
     * Called when unconfirmed money is received
     * @param txId transaction id
     * @param amount amount received in atomic units
     */
    void unconfirmedMoneyReceived(String txId, long amount);

    /**
     * Called when a new block is added to blockchain
     * @param height new blockchain height
     */
    void newBlock(long height);

    /**
     * Called when wallet is updated
     */
    void updated();

    /**
     * Called when wallet refresh is finished
     */
    void refreshed();
}
