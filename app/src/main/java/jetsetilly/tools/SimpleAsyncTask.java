package jetsetilly.tools;

import android.os.Process;
import android.os.AsyncTask;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

public class SimpleAsyncTask {
    public SimpleAsyncTask(Runnable background_runnable) {
        Task task = new Task();
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, background_runnable);
    }

    private class Task extends AsyncTask<Runnable, Void, Void> {
        @Override
        @WorkerThread
        protected Void doInBackground(Runnable... runnable) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            runnable[0].run();
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
        }

        @Override
        @UiThread
        protected void onCancelled() {
        }
    }
}
