package jetsetilly.tools;

import android.os.AsyncTask;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import java.util.Set;

public class SimpleAsyncTask {
    private final static String DBG_TAG = "SAT";
    private final static String TAG_RUNNING = "RUN: ";
    private final static String TAG_COMPLETED = "DONE: ";

    private String tag;
    private Runnable background_runnable;
    private Runnable completion_runnable;
    private Runnable cancelled_runnable;

    public SimpleAsyncTask(String tag, Runnable background_runnable) {
        init(tag, background_runnable, null, null);
    }

    public SimpleAsyncTask(String tag, Runnable background_runnable, Runnable completion_runnable) {
       init(tag, background_runnable, completion_runnable, null);
    }

    public SimpleAsyncTask(String tag, Runnable background_runnable, Runnable completion_runnable, Runnable cancelled_runnable) {
        init(tag, background_runnable, completion_runnable, cancelled_runnable);
    }

    public SimpleAsyncTask(String tag, Runnable background_runnable, Runnable completion_runnable, boolean run_complete_when_cancelled) {
        /* completion runnable is run when task is cancelled */
        init(tag, background_runnable, completion_runnable, run_complete_when_cancelled?completion_runnable:null);
    }

    private void init(String tag, Runnable background_runnable, Runnable completion_runnable, Runnable cancelled_runnable) {
        this.tag = tag;
        this.background_runnable = background_runnable;
        this.completion_runnable = completion_runnable;
        this.cancelled_runnable = cancelled_runnable;

        new Task().execute();
    }

    private class Task extends AsyncTask<Void, Void, Void> {
        @Override
        @WorkerThread
        protected Void doInBackground(Void... v) {
            // priority will be THREAD_PRIORITY_BACKGROUND unless overridden
            background_runnable.run();
            return null;
        }

        @Override
        @UiThread
        protected void onProgressUpdate(Void... v) {
        }

        @Override
        @UiThread
        protected void onPreExecute() {
            Thread.currentThread().setName(TAG_RUNNING + tag);
        }

        @Override
        @UiThread
        protected void onPostExecute(Void v) {
            Thread.currentThread().setName(TAG_COMPLETED + tag);
            if (completion_runnable != null) completion_runnable.run();
        }

        @Override
        @UiThread
        protected void onCancelled() {
            Thread.currentThread().setName(TAG_COMPLETED + tag);
            if (cancelled_runnable != null) cancelled_runnable.run();
        }
    }

    public static void logPrintActivity() {
        Set<Thread> thread_set = Thread.getAllStackTraces().keySet();
        Thread[] thread_list = thread_set.toArray(new Thread[thread_set.size()]);

        for (Thread thr : thread_list) {
            if (thr.isAlive()) {
                String s = thr.toString();
                if (s.contains(TAG_RUNNING)) {
                    LogTools.printDebug(DBG_TAG, thr.getState() + " :: " + thr.toString());
                }
            }
        }

        for (Thread thr : thread_list) {
            if (thr.isAlive()) {
                String s = thr.toString();
                if (s.contains(TAG_COMPLETED)) {
                    LogTools.printDebug(DBG_TAG, thr.getState() + " :: " + thr.toString());
                }
            }
        }
    }
}
