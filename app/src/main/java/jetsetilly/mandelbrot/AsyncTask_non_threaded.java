package jetsetilly.mandelbrot;

import java.util.concurrent.Executor;

/* Implementation of AsyncTask interface but without any of the actual threading. Used to
test whether code that uses AsyncTask is affected by the overhead. */

public abstract  class AsyncTask_non_threaded<Params, Progress, Result>{
    protected abstract Result doInBackground(Params... params);
    protected void onPreExecute() {};
    protected void onPostExecute(Result result) {};
    protected void onProgressUpdate(Progress... values) {};
    protected void onCancelled() {};

    public void executeOnExecutor(Executor exec) {
        executeOnExecutor(exec, null);
    }

    public void executeOnExecutor(Executor exec, Params... params) {
        onPreExecute();
        Result result = doInBackground(params);
        onPostExecute(result);
    }

    protected void publishProgress(Progress... values) {
        onProgressUpdate(values);
    }

    protected boolean isCancelled() {
        return false;
    }

    public void cancel(boolean b) {
    }
}
