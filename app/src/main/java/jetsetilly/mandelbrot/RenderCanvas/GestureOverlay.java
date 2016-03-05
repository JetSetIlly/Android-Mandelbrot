package jetsetilly.mandelbrot.RenderCanvas;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import jetsetilly.mandelbrot.Tools;

public class GestureOverlay extends ImageView implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener
{
    private static final String DEBUG_TAG = "touch canvas";

    private RenderCanvas canvas;

    // gestures will be ignored so long as blocked == true
    private boolean blocked;

    // used by onScroll() to exit early if it is set to true
    // scaling == true between calls to onScaleBegin() and onScaleEnd()
    private boolean scaling;

    // whether the canvas has been altered somehow (ie. scaled or moved)
    private boolean altered_canvas;

    public GestureOverlay(Context context) {
        super(context);
    }

    public GestureOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* initialisation */
    public void setup(Context context, final RenderCanvas canvas) {
        this.canvas = canvas;
        this.blocked = false;
        this.scaling = false;

        final GestureDetectorCompat gestures_detector = new GestureDetectorCompat(context, this);
        final ScaleGestureDetector scale_detector = new ScaleGestureDetector(context, this);
        scale_detector.setQuickScaleEnabled(false);
        gestures_detector.setOnDoubleTapListener(this);

        this.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                /* onGestureListener doesn't handle or expose ACTION_UP events!!
                this is necessary because we need to detect when a TouchState.SCROLL event ends
                so that we can kick-start canvas rendering.

                note that onSingleTapUp() is not the same thing because it is not
                after a scroll event
                */
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    if (altered_canvas) {
                        Tools.printDebug(DEBUG_TAG, "onUp (after altered_canvas): " + event.toString());
                        canvas.startRender();
                    }
                }

                boolean scale_ret = scale_detector.onTouchEvent(event);
                return gestures_detector.onTouchEvent(event) || scale_ret;
            }
        });
    }

    public void block() {
        blocked = true;
    }

    public void unblock() {
        blocked = false;
    }

    /* implementation of onGesturesListener */
    @Override
    public boolean onDown(MotionEvent event) {
        if (blocked) return false;

        Tools.printDebug(DEBUG_TAG, "onDown: " + event.toString());
        canvas.checkActionBar(event.getX(), event.getY(), false);
        altered_canvas = false;
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (blocked) return false;
        if (scaling) return true;

        Tools.printDebug(DEBUG_TAG, "onScroll: " + e1.toString() + e2.toString());
        canvas.scrollBy((int) distanceX, (int) distanceY);
        altered_canvas = true;
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        /* implementation of onDoubleTapListener interface */
        if (blocked) return false;

        Tools.printDebug(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());
        canvas.checkActionBar(event.getX(), event.getY(), true);

        return true;
    }
    /* END OF implementation of onGesturesListener */


    /* implementation of onDoubleTapListener interface */
    @Override
    public boolean onDoubleTap(MotionEvent event) {
        if (blocked) return false;

        Tools.printDebug(DEBUG_TAG, "onDoubleTap: " + event.toString());

        int offset_x = (int) (event.getX() - (canvas.getCanvasWidth() /2));
        int offset_y = (int) (event.getY() - (canvas.getCanvasHeight() / 2));

        canvas.animatedZoom(offset_x, offset_y);

        // not setting altered_canvas to true because we need to
        // restart the render via canvas.animatedZoom method instead
        // see the anim.withEndAction() in animatedZoom()

        return true;
    }
    /* END OF implementation of onDoubleTapListener interface */


    /* implementation of OnScaleGestureListener interface */
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (blocked) return false;

        Tools.printDebug(DEBUG_TAG, "onScaleBegin: " + detector.toString());
        scaling = true;
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (blocked) return false;

        Tools.printDebug(DEBUG_TAG, "onScale: " + detector.toString());
        canvas.zoomBy(detector.getCurrentSpan() - detector.getPreviousSpan());

        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        if (blocked) return;

        Tools.printDebug(DEBUG_TAG, "onScaleEnd: " + detector.toString());
        canvas.zoomCorrection();
        altered_canvas = true;
        scaling = false;
    }
    /* END OF implementation of OnScaleGesture interface */


    /* following methods are not used */
    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        /* implementation of onGesturesListener */
        if (blocked) return false;

        Tools.printDebug(DEBUG_TAG, "onFling: " + event1.toString() + event2.toString());
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        /* implementation of onGesturesListener */
        if (blocked) return;

        Tools.printDebug(DEBUG_TAG, "onLongPress: " + event.toString());
    }

    @Override
    public void onShowPress(MotionEvent event) {
        /* implementation of onGesturesListener */
        if (blocked) return;

        Tools.printDebug(DEBUG_TAG, "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        /* implementation of onGesturesListener */
        if (blocked) return false;

        Tools.printDebug(DEBUG_TAG, "onSingleTapUp: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        /* implementation of onDoubleTapListener interface */
        if (blocked) return false;

        Tools.printDebug(DEBUG_TAG, "onDoubleTapEvent: " + event.toString());
        return true;
    }

}
