package jetsetilly.mandelbrot.RenderCanvas.Base;

import android.content.Context;
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
import jetsetilly.mandelbrot.Mandelbrot.MandelbrotTransform;
import jetsetilly.mandelbrot.R;
import jetsetilly.mandelbrot.RenderCanvas.RenderCanvas;
import jetsetilly.mandelbrot.Settings.Settings;

abstract public class RenderCanvas_Base extends RelativeLayout implements RenderCanvas, MandelbrotCanvas, GestureHandler {
    protected MainActivity context;
    protected Mandelbrot mandelbrot;
    protected GestureOverlay gestures;
    protected TextView fractal_info;

    protected final Settings settings = Settings.getInstance();
    private MandelbrotThread render_thr;

    protected MandelbrotTransform mandelbrot_transform = new MandelbrotTransform();

    // whether or not the previous render completed its work
    protected boolean incomplete_render;

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
        if (render_thr != null) {
            render_thr.cancel(false);
        }
    }

    protected void renderThreadEnded() {
        render_thr = null;
    }

    protected void transformMandelbrot() {
        mandelbrot.transformMandelbrot(mandelbrot_transform, incomplete_render);
        mandelbrot_transform.reset();

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
