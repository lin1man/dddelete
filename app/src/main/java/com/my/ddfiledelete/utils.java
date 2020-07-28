package com.my.ddfiledelete;

public class utils {
    public static String strSettingsName = "settings";
    public static String strResetConfigName = "reset";
    public static String strLogSwitchConfigName = "logswitch";

    public static void sleep(int mm) {
        try {
            Thread.sleep(mm);
        } catch (Throwable t) {
        }
    }

    public static String getStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement stackTraceElement: stackTrace) {
            sb.append(stackTraceElement.toString() + "\n");
        }
        return sb.toString();
    }

    public static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
