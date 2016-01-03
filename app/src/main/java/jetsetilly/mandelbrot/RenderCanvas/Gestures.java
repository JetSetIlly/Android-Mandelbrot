package jetsetilly.mandelbrot.RenderCanvas;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class Gestures implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener
{
    private static final String DEBUG_TAG = "touch canvas";

    public enum TouchState {IDLE, TOUCH, DOUBLE_TOUCH, SCROLL, SCALE}
    public TouchState touch_state = TouchState.IDLE;

    private RenderCanvas canvas;
    public boolean has_scaled;

    // gestures will be ignored so long as blocked == true
    private boolean blocked;

    /* initialisation */
    public Gestures(Context context, final RenderCanvas canvas) {
        this.canvas = canvas;
        this.blocked = false;

        final GestureDetectorCompat gestures_detector = new GestureDetectorCompat(context, this);
        final ScaleGestureDetector scale_detector = new ScaleGestureDetector(context, this);
        scale_detector.setQuickScaleEnabled(false);

        gestures_detector.setOnDoubleTapListener(this);

        canvas.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                /* onGestureListener doesn't handle or expose ACTION_UP events!!
                this is necessary because we need to detect when a TouchState.SCROLL event ends
                so that we can kick-start canvas rendering.

                note that onSingleTapUp() is not the same thing because it is not
                called after a ACTION_MOVE

                we also allow scale events to use the dirty_canvas flag. we could kick-start
                rendering in the onScaleEnd callback but instead we set the dirty_canvas flag to
                keep things consistent.
                */
                if (event.getActionMasked() == MotionEvent.ACTION_UP ) {
                    if (touch_state == TouchState.SCROLL) {
                        Log.d(DEBUG_TAG, "onUp (after move): " + event.toString());
                        canvas.startRender();
                    }

                    touch_state = TouchState.IDLE;
                }

                boolean ret_val = scale_detector.onTouchEvent(event);
                return gestures_detector.onTouchEvent(event) || ret_val;
            }
        });

        resetGestureState();
    }

    public void blockGestures() {
        blocked = true;
    }

    public void unblockGestures() {
        blocked = false;
    }

    private void resetGestureState() {
        has_scaled = false;
    }

    /* implementation of onGesturesListener */
    @Override
    public boolean onDown(MotionEvent event) {
        if (blocked) return false;

        Log.d(DEBUG_TAG, "onDown: " + event.toString());
        canvas.checkActionBar(event.getX(), event.getY());
        touch_state = TouchState.TOUCH;
        resetGestureState();
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (blocked) return false;

        if (touch_state != TouchState.TOUCH && touch_state != TouchState.SCROLL)
            return true;

        Log.d(DEBUG_TAG, "onScroll: " + e1.toString() + e2.toString());
        canvas.scrollBy((int) distanceX, (int) distanceY);
        touch_state = TouchState.SCROLL;
        return true;
    }
    /* END OF implementation of onGesturesListener */


    /* implementation of onDoubleTapListener interface */
    @Override
    public boolean onDoubleTap(MotionEvent event) {
        if (blocked) return false;

        Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString());

        canvas.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.LONG_PRESS);

        int offset_x = (int) (event.getX() - canvas.getCanvasMidX());
        int offset_y = (int) (event.getY() - canvas.getCanvasMidY());

        touch_state = TouchState.DOUBLE_TOUCH;
        canvas.doubleTouchZoom(offset_x, offset_y);

        // render restarted in the canvas.doubleTouchZoom method

        return true;
    }
    /* END OF implementation of onDoubleTapListener interface */


    /* implementation of OnScaleGestureListener interface */
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (blocked) return false;

        Log.d(DEBUG_TAG, "onScale: " + detector.toString());
        Log.d(DEBUG_TAG, "currentSpan: " + detector.getCurrentSpan());
        Log.d(DEBUG_TAG, "previousSpan: " + detector.getPreviousSpan());

        canvas.zoomBy((int) ((detector.getCurrentSpan()-detector.getPreviousSpan()) / 1.25));

        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (blocked) return false;

        Log.d(DEBUG_TAG, "onScaleBegin: " + detector.toString());

        /* don't allow scaling to happen twice without a call to
        resetGestureState(). This is to prevent awkwardness in RenderCanvas.
        I'm sure it's fixable but this is simple fix without much imapact on usability
         */
        if (has_scaled) {
            return false;
        }

        touch_state = TouchState.SCALE;
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        if (blocked) return;

        Log.d(DEBUG_TAG, "onScaleEnd: " + detector.toString());
        touch_state = TouchState.TOUCH;
        has_scaled = true;

        canvas.startRender();
    }
    /* END OF implementation of OnScaleGesture interface */


    /* following methods are not really used */
    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        /* implementation of onGesturesListener */
        if (blocked) return false;

        Log.d(DEBUG_TAG, "onFling: " + event1.toString() + event2.toString());
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        /* implementation of onGesturesListener */
        if (blocked) return;

        Log.d(DEBUG_TAG, "onLongPress: " + event.toString());
    }

    @Override
    public void onShowPress(MotionEvent event) {
        /* implementation of onGesturesListener */
        if (blocked) return;

        Log.d(DEBUG_TAG, "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        /* implementation of onGesturesListener */
        if (blocked) return false;

        Log.d(DEBUG_TAG, "onSingleTapUp: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        /* implementation of onDoubleTapListener interface */
        if (blocked) return false;

        Log.d(DEBUG_TAG, "onDoubleTapEvent: " + event.toString());
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        /* implementation of onDoubleTapListener interface */
        if (blocked) return false;

        Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());
        return true;
    }
}
