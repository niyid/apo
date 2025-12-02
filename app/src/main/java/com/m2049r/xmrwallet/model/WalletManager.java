package com.m2049r.xmrwallet.model;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.m2049r.xmrwallet.data.Node;

/**
 * WalletManager handles JNI bindings, wallet lifecycle, and configuration
 * such as wallet name/password, language, and daemon details.
 */
public class WalletManager {

    /* ===== SINGLETON ===== */
    private static WalletManager instance;

    /* ===== CONFIGURATION FIELDS ===== */
    private String walletName;
    private String walletPassword;
    private String walletLanguage;
    private String daemonAddress;
    private int daemonPort;
    private String daemonUsername = "";
    private String daemonPassword = "";
    private NetworkType networkType = NetworkType.NetworkType_Stagenet; // default
    private boolean forceSsl;

    /* ===== STATE ===== */
    private String errorString = "";

    /* ===== PRIVATE CONSTRUCTOR ===== */
    private WalletManager() {}

    public static synchronized WalletManager getInstance() {
        if (instance == null) {
            instance = new WalletManager();
        }
        return instance;
    }

    /* ===== CONFIGURATION LOADERS ===== */
    public void applyConfiguration(Properties props) {
        this.walletName = props.getProperty("wallet.name", "bitchat_wallet_stagenet");
        this.walletPassword = props.getProperty("wallet.password", "bitchat_secure_pass");
        this.walletLanguage = props.getProperty("wallet.language", "English");
        this.daemonAddress = props.getProperty("daemon.address", "stagenet.xmr-tw.org");
        this.daemonPort = Integer.parseInt(props.getProperty("daemon.port", "38081"));
        this.daemonUsername = props.getProperty("daemon.username", "");
        this.daemonPassword = props.getProperty("daemon.password", "");
        int netInt = Integer.parseInt(props.getProperty("network.type", "2"));
        this.networkType = fromInt(netInt);
        this.forceSsl = Boolean.parseBoolean(props.getProperty("daemon.ssl", "false"));
    }

    private static NetworkType fromInt(int value) {
        switch (value) {
            case 0: return NetworkType.NetworkType_Mainnet;
            case 1: return NetworkType.NetworkType_Testnet;
            case 2:
            default: return NetworkType.NetworkType_Stagenet;
        }
    }

    private static int toInt(NetworkType type) {
        if (type == null) return 2;
        switch (type) {
            case NetworkType_Mainnet: return 0;
            case NetworkType_Testnet: return 1;
            case NetworkType_Stagenet:
            default: return 2;
        }
    }

    public Node createNodeFromConfig() {
        Node node = new Node();
        try {
            node.setHost(daemonAddress.split(":")[0]);
        } catch (UnknownHostException ex) {
            throw new IllegalArgumentException("cannot resolve host " + daemonAddress);
        }        
        node.setRpcPort(daemonPort);
        node.setUsername(daemonUsername);
        node.setPassword(daemonPassword);
        node.setNetworkType(networkType);
        return node;
    }

    public void setDaemonConfig(String address, int port, String username, String password,
                                NetworkType netType, boolean ssl) {
        if (address != null && !address.isEmpty()) daemonAddress = address;
        daemonPort = port;
        daemonUsername = username != null ? username : "";
        daemonPassword = password != null ? password : "";
        if (netType != null) networkType = netType;
        forceSsl = ssl;
    }

    /* ===== JNI BINDINGS ===== */
    public native long createWalletJ(String path, String password, String language, int nettype);
    public native long openWalletJ(String path, String password, int nettype);
    public native long recoveryWalletJ(String path, String password, String mnemonic,
                                       int nettype, long restoreHeight);

    private native boolean verifyWalletPasswordJ(String keysFileName, String password, boolean noSpendKey);
    private native String getErrorStringJ();
    private native String[] findWalletsJ(String path);
    public native boolean setDaemonAddressJ(String address);

    public native int getDaemonVersion();
    public native long getBlockchainHeight();
    public native long getBlockchainTargetHeight();
    public native long getNetworkDifficulty();
    public native double getMiningHashRate();
    public native long getBlockTarget();
    public native boolean isMining();
    public native boolean startMining(String address, boolean background_mining, boolean ignore_battery);
    public native boolean stopMining();
    public native String resolveOpenAlias(String address, boolean dnssec_valid);
    public native boolean setProxy(String address);

    static public native void initLogger(String argv0, String defaultLogBaseName);
    static public native void setLogLevel(int level);
    static public native void logDebug(String category, String message);
    static public native void logInfo(String category, String message);
    static public native void logWarning(String category, String message);
    static public native void logError(String category, String message);
    static public native String moneroVersion();
    public native boolean closeJ(Wallet wallet);

    /* ===== JAVA WRAPPERS ===== */
    public void setDaemon(Node node) {
        if (node != null) {
            if (networkType != null && networkType != node.getNetworkType())
                throw new IllegalArgumentException("network type does not match");
            this.daemonUsername = node.getUsername();
            this.daemonPassword = node.getPassword();
            this.setDaemonAddress(node.getHost());
            this.daemonPort = node.getRpcPort();
        } else {
            this.daemonAddress = null;
            this.daemonUsername = "";
            this.daemonPassword = "";
        }
    }

    public boolean setDaemonAddress(String address) {
        this.daemonAddress = address;
        return setDaemonAddressJ(address);
    }

    public void setNetworkType(NetworkType networkType) {
        this.networkType = networkType;
    }

    public Wallet createWallet(String path) {
        long walletHandle = createWalletJ(path, walletPassword, walletLanguage, toInt(networkType));
        return walletHandle != 0 ? new Wallet(walletHandle) : null;
    }

    public Wallet openWallet(String path) {
        long walletHandle = openWalletJ(path, walletPassword, toInt(networkType));
        return walletHandle != 0 ? new Wallet(walletHandle) : null;
    }

    public Wallet recoveryWallet(String path, String mnemonic, long restoreHeight) {
        long walletHandle = recoveryWalletJ(path, walletPassword, mnemonic, toInt(networkType), restoreHeight);
        return walletHandle != 0 ? new Wallet(walletHandle) : null;
    }

    public boolean verifyWalletPassword(String keysFileName, boolean noSpendKey) {
        return verifyWalletPasswordJ(keysFileName, walletPassword, noSpendKey);
    }

    public List<String> findWallets(String path) {
        List<String> wallets = new ArrayList<>();
        String[] result = findWalletsJ(path);
        if (result != null) {
            for (String walletName : result) {
                wallets.add(walletName);
            }
        }
        return wallets;
    }

    public String getErrorString() {
        return getErrorStringJ();
    }
    
    public boolean close(Wallet wallet) {
        return closeJ(wallet);
    }    

    /* ===== GETTERS ===== */
    public String getWalletName() { return walletName; }
    public String getWalletPassword() { return walletPassword; }
    public String getWalletLanguage() { return walletLanguage; }
    public String getDaemonAddress() { return daemonAddress; }
    public int getDaemonPort() { return daemonPort; }
    public String getDaemonUsername() { return daemonUsername; }
    public String getDaemonPassword() { return daemonPassword; }
    public NetworkType getNetworkType() { return networkType; }
    public boolean isForceSsl() { return forceSsl; }
}

