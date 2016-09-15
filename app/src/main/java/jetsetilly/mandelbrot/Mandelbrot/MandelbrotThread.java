package jetsetilly.mandelbrot.Mandelbrot;

import android.os.AsyncTask;
import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.Settings.MandelbrotCoordinates;
import jetsetilly.mandelbrot.Settings.Settings;

abstract public class MandelbrotThread extends AsyncTask<Void, Void, Void> {
    /* from the android documentation:

    AsyncTasks should ideally be used for short operations (a few seconds at the most.) If you
    need to keep threads running for long periods of time, it is highly recommended you use_next the
    various APIs provided by the java.util.concurrent package such as Executor,
    ThreadPoolExecutor and FutureTask.
     */

    protected long canvas_id;

    protected final MandelbrotCoordinates mandelbrot_coordinates = MandelbrotCoordinates.getInstance();
    protected final Settings settings = Settings.getInstance();
    protected final Mandelbrot m;
    protected final MandelbrotCanvas c;

    public MandelbrotThread(Mandelbrot mandelbrot, MandelbrotCanvas canvas) {
        this.m = mandelbrot;
        this.c = canvas;
    }

    @Override
    @WorkerThread
    @CallSuper
    protected Void doInBackground(Void... v) {
        c.startPlot(canvas_id);
        return null;
    }

    @Override
    @UiThread
    @CallSuper
    protected void onProgressUpdate(Void... v) {
        MainActivity.progress.kick(m.rescaling_render);
        c.updatePlot(canvas_id);
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
        c.endPlot(canvas_id, false);
        MainActivity.progress.unregister();
    }

    @Override
    @UiThread
    @CallSuper
    protected void onCancelled() {
        c.endPlot(canvas_id, true);
        MainActivity.progress.unregister();
    }
}
