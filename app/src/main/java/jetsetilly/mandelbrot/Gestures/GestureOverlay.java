package jetsetilly.mandelbrot.Gestures;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import jetsetilly.tools.LogTools;

public class GestureOverlay extends ImageView implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener
{
    private static final String DEBUG_TAG = LogTools.NO_LOG_PREFIX + "gesture overlay";

    private GestureHandler gesture_handler;

    // gestures will be ignored so long as blocked == true
    private boolean blocked;

    // whether the canvas has been scrolled somehow
    private boolean scrolling_canvas;

    // used by onScroll() to exit early if it is set to true
    // scaling_canvas == true between calls to onScaleBegin() and onScaleEnd()
    private boolean scaling_canvas;

    public GestureOverlay(Context context) {
        super(context);
    }

    public GestureOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* initialisation */
    public void setup(Context context, final GestureHandler gesture_handler) {
        this.gesture_handler = gesture_handler;
        this.blocked = false;
        this.scaling_canvas = false;

        final GestureDetectorCompat gestures_detector = new GestureDetectorCompat(context, this);
        final ScaleGestureDetector scale_detector = new ScaleGestureDetector(context, this);
        scale_detector.setQuickScaleEnabled(false);
        gestures_detector.setOnDoubleTapListener(this);

        this.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                /*
                onGestureListener doesn't handle or expose ACTION_UP events!!
                this is necessary because we need to detect when a TouchState.SCROLL event ends
                so that we can kick-start canvas rendering.

                note that onSingleTapUp() is not the same thing because it is not
                called after a scroll event
                */
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    if (scrolling_canvas) {
                        scrolling_canvas = false;
                        LogTools.printDebug(DEBUG_TAG, "onUp (after scrolled canvas): " + event.toString());
                        gesture_handler.finishScroll();
                    }
                }

                boolean scale_ret = scale_detector.onTouchEvent(event);
                return gestures_detector.onTouchEvent(event) || scale_ret;
            }
        });
    }
    /* END OF initialisation */

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

        LogTools.printDebug(DEBUG_TAG, "onDown: " + event.toString());
        gesture_handler.checkActionBar(event.getX(), event.getY(), false);
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (blocked) return false;
        if (scaling_canvas) return true;

        LogTools.printDebug(DEBUG_TAG, "onScroll: " + e1.toString() + e2.toString());
        gesture_handler.scroll((int) distanceX, (int) distanceY);
        scrolling_canvas = true;
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        /* implementation of onDoubleTapListener interface */
        if (blocked) return false;

        LogTools.printDebug(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());
        gesture_handler.checkActionBar(event.getX(), event.getY(), true);

        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        /* implementation of onGesturesListener */
        if (blocked) return;

        LogTools.printDebug(DEBUG_TAG, "onLongPress: " + event.toString());

        // no offset when we're zooming out

        gesture_handler.animatedZoom(0, 0, true);
    }

    /* END OF implementation of onGesturesListener */


    /* implementation of onDoubleTapListener interface */
    @Override
    public boolean onDoubleTap(MotionEvent event) {
        if (blocked) return false;

        LogTools.printDebug(DEBUG_TAG, "onDoubleTap: " + event.toString());

        gesture_handler.animatedZoom((int) event.getX(), (int) event.getY(), false);

        return true;
    }
    /* END OF implementation of onDoubleTapListener interface */


    /* implementation of OnScaleGestureListener interface */
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (blocked) return false;

        LogTools.printDebug(DEBUG_TAG, "onScaleBegin: " + detector.toString());
        scaling_canvas = true;
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (blocked) return false;

        LogTools.printDebug(DEBUG_TAG, "onScale: " + detector.toString());
        gesture_handler.pinchZoom(detector.getCurrentSpan() - detector.getPreviousSpan());

        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        if (blocked) return;

        LogTools.printDebug(DEBUG_TAG, "onScaleEnd: " + detector.toString());
        gesture_handler.zoomCorrection(false);
        scaling_canvas = false;
    }
    /* END OF implementation of OnScaleGesture interface */


    /* following methods are not used */
    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        /* implementation of onGesturesListener */
        if (blocked) return false;

        LogTools.printDebug(DEBUG_TAG, "onFling: " + event1.toString() + event2.toString());
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        /* implementation of onGesturesListener */
        if (blocked) return;

        LogTools.printDebug(DEBUG_TAG, "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        /* implementation of onGesturesListener */
        if (blocked) return false;

        LogTools.printDebug(DEBUG_TAG, "onSingleTapUp: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        /* implementation of onDoubleTapListener interface */
        if (blocked) return false;

        LogTools.printDebug(DEBUG_TAG, "onDoubleTapEvent: " + event.toString());
        return true;
    }

}
