package com.my.ddfiledelete;

import android.util.Log;

public class LogUtil {
    public static boolean logable = true;

    public static void i(String tag, String msg) {
        if (logable) {
            Log.i(tag, msg);
        }
    }
    public static void d(String tag, String msg) {
        if (logable) {
            Log.d(tag, msg);
        }
    }
    public static void e(String tag, String msg) {
        if (logable) {
            Log.e(tag, msg);
        }
    }
    public static void w(String tag, String msg) {
        if (logable) {
            Log.w(tag, msg);
        }
    }
}
