package jetsetilly.mandelbrot.RenderCanvas.Base;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import jetsetilly.mandelbrot.Gestures.GestureHandler;
import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.Mandelbrot.MandelbrotCanvas;
import jetsetilly.mandelbrot.RenderCanvas.RenderCanvas;

abstract public class RenderCanvas_Base extends RelativeLayout implements RenderCanvas, MandelbrotCanvas, GestureHandler {
    public RenderCanvas_Base(Context context) {
        super(context);
    }

    public RenderCanvas_Base(Context context, AttributeSet attrs) {
        super(context, attrs);
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
}
