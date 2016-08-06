package jetsetilly.tools;

import android.content.Context;
import android.os.Process;
import android.os.AsyncTask;
import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

public class SimpleAsyncTask {
    private Runnable background_runnable;

    public SimpleAsyncTask(Runnable background_runnable) {
        this.background_runnable = background_runnable;
        Task task = new Task();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class Task extends AsyncTask<Void, Void, Void> {
        protected long canvas_id;

        @Override
        @WorkerThread
        @CallSuper
        protected Void doInBackground(Void... v) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            background_runnable.run();
            return null;
        }

        @Override
        @UiThread
        @CallSuper
        protected void onProgressUpdate(Void... v) {
        }

        @Override
        @UiThread
        @CallSuper
        protected void onPreExecute() {
        }

        @Override
        @UiThread
        @CallSuper
        protected void onPostExecute(Void v) {
        }

        @Override
        @UiThread
        @CallSuper
        protected void onCancelled() {
        }
    }
}
