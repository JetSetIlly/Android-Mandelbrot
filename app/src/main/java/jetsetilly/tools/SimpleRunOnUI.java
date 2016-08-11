package jetsetilly.tools;

import android.os.Looper;
import android.support.v7.app.AppCompatActivity;

import java.util.concurrent.Semaphore;

public class SimpleRunOnUI {
    static private final String DBG_TAG = "SimpleRunOnUI";

    static public void run(AppCompatActivity activity, final Runnable runnable) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runnable.run();
        } else {
            final Semaphore sync = new Semaphore(1);

            Runnable master_runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        runnable.run();
                    } finally {
                        sync.release();
                    }
                }
            };

            try {
                sync.acquire();
                activity.runOnUiThread(master_runnable);
                sync.acquire();
                sync.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
