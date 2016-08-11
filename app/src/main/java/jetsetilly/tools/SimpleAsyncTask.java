package jetsetilly.tools;

import android.os.Process;
import android.os.AsyncTask;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

public class SimpleAsyncTask {
    private Runnable background_runnable;
    private Runnable completion_runnable;
    private Runnable cancelled_runnable;

    public SimpleAsyncTask(Runnable background_runnable) {
        init(background_runnable, null, null);
    }

    public SimpleAsyncTask(Runnable background_runnable, Runnable completion_runnable) {
       init(background_runnable, completion_runnable, null);
    }

    public SimpleAsyncTask(Runnable background_runnable, Runnable completion_runnable, Runnable cancelled_runnable) {
        init(background_runnable, completion_runnable, cancelled_runnable);
    }

    public SimpleAsyncTask(Runnable background_runnable, Runnable completion_runnable, boolean run_complete_when_cancelled) {
        /* completion runnable is run when task is cancelled */
        init(background_runnable, completion_runnable, run_complete_when_cancelled?completion_runnable:null);
    }

    private void init(Runnable background_runnable, Runnable completion_runnable, Runnable cancelled_runnable) {
        this.background_runnable = background_runnable;
        this.completion_runnable = completion_runnable;
        this.cancelled_runnable = cancelled_runnable;

        Task task = new Task();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class Task extends AsyncTask<Void, Void, Void> {
        @Override
        @WorkerThread
        protected Void doInBackground(Void... v) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
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
        }

        @Override
        @UiThread
        protected void onPostExecute(Void v) {
            if (completion_runnable != null) completion_runnable.run();
        }

        @Override
        @UiThread
        protected void onCancelled() {
            if (cancelled_runnable != null) cancelled_runnable.run();
        }
    }
}
