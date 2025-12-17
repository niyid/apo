package com.m2049r.xmrwallet.model;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.m2049r.xmrwallet.data.Node;

import android.util.Log;
import java.io.File;

/**
 * WalletManager handles JNI bindings, wallet lifecycle, and configuration
 * such as wallet name/password, language, and daemon details.
 * 
 * FIXED: Added default values to prevent null pointer exceptions when
 * configuration files are missing.
 */
public class WalletManager {
    private static final String TAG = "com.techducat.apo.WalletManager";
    /* ===== SINGLETON ===== */
    private static WalletManager instance;

    /* ===== CONFIGURATION FIELDS WITH DEFAULTS ===== */
    // CRITICAL FIX: Initialize with default values to prevent NPE
    private String walletName = "bitchat_wallet_stagenet";
    private String walletPassword = "bitchat_secure_pass";
    private String walletLanguage = "English";
    private String daemonAddress = "stagenet.xmr-tw.org";
    private int daemonPort = 38081;
    private String daemonUsername = "";
    private String daemonPassword = "";
    private NetworkType networkType = NetworkType.NetworkType_Stagenet; // default
    private boolean forceSsl = false;

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
        // Use getProperty with defaults as fallback
        this.walletName = props.getProperty("wallet.name", this.walletName);
        this.walletPassword = props.getProperty("wallet.password", this.walletPassword);
        this.walletLanguage = props.getProperty("wallet.language", this.walletLanguage);
        this.daemonAddress = props.getProperty("daemon.address", this.daemonAddress);
        
        // Parse port with error handling
        String portStr = props.getProperty("daemon.port", String.valueOf(this.daemonPort));
        try {
            this.daemonPort = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            // Keep default port if parsing fails
            System.err.println("Invalid daemon.port value: " + portStr + ", using default: " + this.daemonPort);
        }
        
        this.daemonUsername = props.getProperty("daemon.username", "");
        this.daemonPassword = props.getProperty("daemon.password", "");
        
        // Parse network type with error handling
        String netStr = props.getProperty("network.type", "2");
        try {
            int netInt = Integer.parseInt(netStr);
            this.networkType = fromInt(netInt);
        } catch (NumberFormatException e) {
            // Keep default network type if parsing fails
            System.err.println("Invalid network.type value: " + netStr + ", using default: " + this.networkType);
        }
        
        // Parse SSL with error handling
        String sslStr = props.getProperty("daemon.ssl", "false");
        this.forceSsl = Boolean.parseBoolean(sslStr);
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
        try {
            Log.d(TAG, "createWalletJ - path: " + path);
            Log.d(TAG, "createWalletJ - password: " + (walletPassword != null ? "SET" : "NULL"));
            Log.d(TAG, "createWalletJ - language: " + walletLanguage);
            Log.d(TAG, "createWalletJ - networkType: " + toInt(networkType));
            
            // Verify path is writable
            File walletFile = new File(path);
            File parentDir = walletFile.getParentFile();
            if (parentDir != null && !parentDir.canWrite()) {
                Log.e(TAG, "CRITICAL: Cannot write to parent directory: " + parentDir.getAbsolutePath());
                return null;
            }
            
            long walletHandle = createWalletJ(path, walletPassword, walletLanguage, toInt(networkType));
            
            if (walletHandle == 0) {
                String error = getErrorStringJ();
                Log.e(TAG, "createWalletJ returned 0. Error: " + error);
                return null;
            }
            
            Log.d(TAG, "createWalletJ returned handle: " + walletHandle);
            return new Wallet(walletHandle);
        } catch (Throwable t) {
            Log.e(TAG, "CRITICAL: Exception in createWallet", t);
            return null;
        }
    }

    public Wallet openWallet(String path) {
        try {
            Log.d(TAG, "openWalletJ - path: " + path);
            Log.d(TAG, "openWalletJ - password: " + (walletPassword != null ? "SET" : "NULL"));
            Log.d(TAG, "openWalletJ - networkType: " + toInt(networkType));
            
            // Verify keys file exists
            File keysFile = new File(path + ".keys");
            if (!keysFile.exists()) {
                Log.e(TAG, "Keys file does not exist: " + keysFile.getAbsolutePath());
                return null;
            }
            
            if (!keysFile.canRead()) {
                Log.e(TAG, "Keys file not readable: " + keysFile.getAbsolutePath());
                return null;
            }
            
            long walletHandle = openWalletJ(path, walletPassword, toInt(networkType));
            
            if (walletHandle == 0) {
                String error = getErrorStringJ();
                Log.e(TAG, "openWalletJ returned 0. Error: " + error);
                return null;
            }
            
            Log.d(TAG, "openWalletJ returned handle: " + walletHandle);
            return new Wallet(walletHandle);
        } catch (Throwable t) {
            Log.e(TAG, "CRITICAL: Exception in openWallet", t);
            return null;
        }
    }

    public Wallet recoveryWallet(String path, String mnemonic, long restoreHeight) {
        try {
            Log.d(TAG, "recoveryWalletJ - path: " + path);
            Log.d(TAG, "recoveryWalletJ - restoreHeight: " + restoreHeight);
            Log.d(TAG, "recoveryWalletJ - networkType: " + toInt(networkType));
            
            // Verify path is writable
            File walletFile = new File(path);
            File parentDir = walletFile.getParentFile();
            if (parentDir != null && !parentDir.canWrite()) {
                Log.e(TAG, "CRITICAL: Cannot write to parent directory: " + parentDir.getAbsolutePath());
                return null;
            }
            
            if (mnemonic == null || mnemonic.trim().isEmpty()) {
                Log.e(TAG, "Invalid mnemonic seed");
                return null;
            }
            
            long walletHandle = recoveryWalletJ(path, walletPassword, mnemonic, toInt(networkType), restoreHeight);
            
            if (walletHandle == 0) {
                String error = getErrorStringJ();
                Log.e(TAG, "recoveryWalletJ returned 0. Error: " + error);
                return null;
            }
            
            Log.d(TAG, "recoveryWalletJ returned handle: " + walletHandle);
            return new Wallet(walletHandle);
        } catch (Throwable t) {
            Log.e(TAG, "CRITICAL: Exception in recoveryWallet", t);
            return null;
        }
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
