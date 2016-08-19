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
import jetsetilly.mandelbrot.R;
import jetsetilly.mandelbrot.RenderCanvas.RenderCanvas;

abstract public class RenderCanvas_Base extends RelativeLayout implements RenderCanvas, MandelbrotCanvas, GestureHandler {
    private MainActivity main_activity;
    protected Mandelbrot mandelbrot;
    protected GestureOverlay gestures;

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
        this.gestures = (GestureOverlay) main_activity.findViewById(R.id.gestureOverlay);
        assert this.gestures != null;
        this.gestures.setup(main_activity, this);
        this.main_activity = main_activity;
    }

    @CallSuper
    public void resetCanvas() {
        // prepare new mandelbrot
        mandelbrot = new Mandelbrot(main_activity, this, (TextView) main_activity.findViewById(R.id.infoPane));
    }
}
