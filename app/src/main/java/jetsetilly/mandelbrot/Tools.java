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
        printDebug(tag, msg, false);
    }

    static public void printStackTrace(String tag) {
        printDebug(tag, Log.getStackTraceString(new Exception()));
    }

    static public void printDebug(String tag, String msg) {
       printDebug(tag, msg, false);
    }

    static public void printDebug(String tag, String msg, boolean wtf) {
        Log.d(tag, String.format("[%s] %s", Thread.currentThread().getId(), msg));
    }
}
