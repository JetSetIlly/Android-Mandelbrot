package jetsetilly.tools;

import android.util.Log;

public class LogTools {
    public static final String NO_LOG_PREFIX = "xxx ";

    static public void printWTF(String tag, String msg) {
        printDebug(tag, msg, true);
    }

    static public void printStackTrace(String tag) {
        printDebug(tag, android.util.Log.getStackTraceString(new Exception()));
    }

    static public void printDebug(String tag, int num) {
        printDebug(tag, String.format("%d", num));
    }

    static public void printDebug(String tag, double num) {
        printDebug(tag, String.format("%f", num));
    }

    static public void printDebug(String tag, String msg) {
       printDebug(tag, msg, false);
    }

    static public void printDebug(String tag, String msg, boolean wtf) {
        if (tag.startsWith(NO_LOG_PREFIX)) return;

        if (wtf) {
            Log.wtf(tag, String.format("[%s] %s", Thread.currentThread().getId(), msg));
        } else {
            Log.d(tag, String.format("[%s] %s", Thread.currentThread().getId(), msg));
        }
    }
}
