package com.my.ddfiledelete;

public class utils {
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
}
