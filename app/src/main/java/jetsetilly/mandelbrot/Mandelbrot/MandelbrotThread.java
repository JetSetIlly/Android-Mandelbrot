package jetsetilly.mandelbrot.Mandelbrot;

import android.os.AsyncTask;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;

abstract public class MandelbrotThread  extends AsyncTask<Void, Integer, Void> {
    /* from the android documentation:

    AsyncTasks should ideally be used for short operations (a few seconds at the most.) If you
    need to keep threads running for long periods of time, it is highly recommended you use the
    various APIs provided by the java.util.concurrent package such as Executor,
    ThreadPoolExecutor and FutureTask.
     */

    protected final MandelbrotSettings mandelbrot_settings = MandelbrotSettings.getInstance();
    protected final Mandelbrot m;

    public MandelbrotThread(Mandelbrot context) {
        this.m = context;
    }

    @Override
    protected void onProgressUpdate(Integer... pass) {
        MainActivity.progress.kick(pass[0], mandelbrot_settings.num_passes, m.rescaling_render);
        m.canvas.update();
    }

    @Override
    protected void onPreExecute() {
        MainActivity.progress.register();
        m.canvas.startDraw(mandelbrot_settings.render_mode);
    }

    @Override
    protected void onPostExecute(Void v) {
        MainActivity.progress.unregister();
        m.canvas.endDraw();
    }

    @Override
    protected void onCancelled() {
        MainActivity.progress.unregister();
        m.canvas.cancelDraw();
    }
}
