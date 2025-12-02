package com.m2049r.xmrwallet.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Helper {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    public static String getDisplayAmount(long amount) {
        double xmr = amount / 1e12;
        return String.format("%.12f", xmr).replaceAll("0*$", "").replaceAll("\\.$", "");
    }

    public static long getAmountFromString(String amount) {
        try {
            return (long)(Double.parseDouble(amount) * 1e12);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static String formatDateTime(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp * 1000));
    }

    public static boolean isValidAddress(String address) {
        return address != null && address.length() > 90 && (address.startsWith("4") || address.startsWith("8"));
    }

    public static String formatHash(String hash) {
        if (hash == null || hash.length() < 16) return hash;
        return hash.substring(0, 8) + "..." + hash.substring(hash.length() - 8);
    }
}
