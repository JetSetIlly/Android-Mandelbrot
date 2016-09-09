package jetsetilly.mandelbrot.RenderCanvas.Base;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.CallSuper;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.TextView;

import jetsetilly.mandelbrot.Gestures.GestureHandler;
import jetsetilly.mandelbrot.Gestures.GestureOverlay;
import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.mandelbrot.Mandelbrot.MandelbrotCanvas;
import jetsetilly.mandelbrot.Mandelbrot.MandelbrotThread;
import jetsetilly.mandelbrot.Mandelbrot.MandelbrotThread_dalvik;
import jetsetilly.mandelbrot.Mandelbrot.MandelbrotThread_renderscript;
import jetsetilly.mandelbrot.R;
import jetsetilly.mandelbrot.RenderCanvas.RenderCanvas;
import jetsetilly.mandelbrot.Settings.Settings;
import jetsetilly.tools.MyDebug;

abstract public class RenderCanvas_Base extends RelativeLayout implements RenderCanvas, MandelbrotCanvas, GestureHandler {
    protected MainActivity context;
    protected Mandelbrot mandelbrot;
    protected GestureOverlay gestures;
    protected TextView fractal_info;

    protected final Settings settings = Settings.getInstance();
    protected MandelbrotThread render_thr;

    /*** fields to record the state of the image mandelbrot ***/
    // the amount of deviation (offset) from the current display_bm
    // used when chaining scroll and zoom events
    // reset when render is restarted
    // use getX() and getY() to retrieve current scroll values
    protected int rendered_offset_x;
    protected int rendered_offset_y;

    // the amount by which the mandelbrot needs to scale in order to match the display (image_scale)
    protected double fractal_scale;

    // whether or not the previous render completed its work
    protected boolean incomplete_render;
    /*** end ***/

    public RenderCanvas_Base(Context context) {
        super(context);
    }

    public RenderCanvas_Base(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RenderCanvas_Base(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void checkActionBar(float x, float y, boolean allow_show) {
        // returns false if coordinates are in action bar, otherwise true
       if (!MainActivity.action_bar.inActionBar(y)) {
           MainActivity.action_bar.setVisibility(true);
        } else {
           if (allow_show) {
               MainActivity.action_bar.setVisibility(false);
           }
        }
    }

    @CallSuper
    public void initialise(final MainActivity main_activity) {
        // get a reference to the gesture overlay and set it up
        this.context = main_activity;
        this.gestures = (GestureOverlay) main_activity.findViewById(R.id.gestureOverlay);
        assert this.gestures != null;
        this.gestures.setup(main_activity, this);
        this.fractal_info = (TextView) context.findViewById(R.id.infoPane);
    }

    @CallSuper
    public void resetCanvas() {
        // prepare new mandelbrot
        mandelbrot = new Mandelbrot(context,  getWidth(), getHeight());
    }

    protected void startRenderThread() {
        MainActivity.progress.startSession();

        if (settings.render_mode == Mandelbrot.RenderMode.HARDWARE) {
            render_thr = new MandelbrotThread_renderscript(mandelbrot, this);
        } else {
            render_thr = new MandelbrotThread_dalvik(mandelbrot, this);
        }

        render_thr.execute();
    }

    protected void stopRenderThread() {
        if (render_thr == null) {
            return;
        }

        render_thr.cancel(false);
        render_thr = null;
    }

    protected void transformMandelbrot() {
        stopRender();
        mandelbrot.transformMandelbrot(rendered_offset_x, rendered_offset_y, fractal_scale, incomplete_render);
        fractal_scale = 0;
        rendered_offset_x = 0;
        rendered_offset_y = 0;

        // display mandelbrot info
        post(new Runnable() {
                @Override
                public void run() {
                    fractal_info.setText(mandelbrot.toString());
                }
            }
        );
    }

    abstract public void startRender();
    abstract public void stopRender();
}
