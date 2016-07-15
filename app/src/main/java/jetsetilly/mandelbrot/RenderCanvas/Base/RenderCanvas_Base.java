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

    protected float scaleFromZoomFactor(double zoom_factor) {
        return (float) (1 / (1 - (2 * zoom_factor)));
    }

    protected double zoomFactorFromScale(float scale) {
        return (scale - 1) / (2 * scale);
    }

    public void checkActionBar(float x, float y, boolean show) {
        // returns false if coordinates are in action bar, otherwise true
        if (show) {
            if (MainActivity.action_bar.inActionBar(y)) {
                MainActivity.action_bar.setVisibility(false);
            }
        } else {
            MainActivity.action_bar.setVisibility(true);
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
