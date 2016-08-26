package jetsetilly.mandelbrot.Mandelbrot;

import android.os.AsyncTask;
import android.os.Process;
import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;

abstract public class MandelbrotThread extends AsyncTask<Void, Void, Void> {
    /* from the android documentation:

    AsyncTasks should ideally be used for short operations (a few seconds at the most.) If you
    need to keep threads running for long periods of time, it is highly recommended you use the
    various APIs provided by the java.util.concurrent package such as Executor,
    ThreadPoolExecutor and FutureTask.
     */

    protected long canvas_id;

    protected final MandelbrotSettings mandelbrot_settings = MandelbrotSettings.getInstance();
    protected final Mandelbrot m;

    public MandelbrotThread(Mandelbrot context) {
        this.m = context;
    }

    @Override
    @WorkerThread
    @CallSuper
    protected Void doInBackground(Void... v) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);
        m.canvas.startDraw(canvas_id);
        return null;
    }

    @Override
    @UiThread
    @CallSuper
    protected void onProgressUpdate(Void... v) {
        MainActivity.progress.kick(m.rescaling_render);
        m.canvas.update(canvas_id);
    }

    @Override
    @UiThread
    @CallSuper
    protected void onPreExecute() {
        canvas_id = System.currentTimeMillis();
        MainActivity.progress.startSession();
    }

    @Override
    @UiThread
    @CallSuper
    protected void onPostExecute(Void v) {
        m.canvas.endDraw(canvas_id, false);
        MainActivity.progress.unregister();
    }

    @Override
    @UiThread
    @CallSuper
    protected void onCancelled() {
        m.canvas.endDraw(canvas_id, true);
        MainActivity.progress.unregister();
    }
}
