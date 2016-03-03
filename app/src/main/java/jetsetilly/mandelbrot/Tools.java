package jetsetilly.mandelbrot;

import android.content.Context;
import android.util.Log;

public class Tools {
    static public int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    static public void printWTF(String tag, String msg) {
        printDebug(tag, msg, true);
    }

    static public void printStackTrace(String tag) {
        printDebug(tag, Log.getStackTraceString(new Exception()));
    }

    static public void printDebug(String tag, int num) {
        printDebug(tag, String.format("%d", num));
    }

    static public void printDebug(String tag, String msg) {
       printDebug(tag, msg, false);
    }

    static public void printDebug(String tag, String msg, boolean wtf) {
        if (wtf) {
            Log.wtf(tag, String.format("[%s] %s", Thread.currentThread().getId(), msg));
        } else {
            Log.d(tag, String.format("[%s] %s", Thread.currentThread().getId(), msg));
        }
    }
}
