package jetsetilly.mandelbrot.RenderCanvas;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import jetsetilly.mandelbrot.Tools;

public class Gestures implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener
{
    private static final String DEBUG_TAG = "touch canvas";

    private enum TouchState {IDLE, TOUCH, DOUBLE_TOUCH, SCROLL, SCALE}
    private TouchState touch_state = TouchState.IDLE;

    private final RenderCanvas canvas;

    // gestures will be ignored so long as blocked == true
    private boolean blocked;

    // whether the canvas has been altered somehow (ie. scaled or moved)
    private boolean altered_canvas;


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
                */
                if (event.getActionMasked() == MotionEvent.ACTION_UP ) {
                    if (altered_canvas) {
                        Tools.printDebug(DEBUG_TAG, "onUp (after altered_canvas): " + event.toString());
                        canvas.startRender();
                    }

                    touch_state = TouchState.IDLE;
                }

                boolean scale_ret = scale_detector.onTouchEvent(event);
                return gestures_detector.onTouchEvent(event) || scale_ret;
            }
        });
    }

    public void blockGestures() {
        blocked = true;
    }

    public void unblockGestures() {
        blocked = false;
    }

    /* implementation of onGesturesListener */
    @Override
    public boolean onDown(MotionEvent event) {
        if (blocked) return false;

        Tools.printDebug(DEBUG_TAG, "onDown: " + event.toString());
        canvas.checkActionBar(event.getX(), event.getY(), false);
        touch_state = TouchState.TOUCH;
        altered_canvas = false;
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (blocked) return false;

        if (touch_state != TouchState.TOUCH && touch_state != TouchState.SCROLL)
            return true;

        Tools.printDebug(DEBUG_TAG, "onScroll: " + e1.toString() + e2.toString());
        canvas.scrollBy((int) distanceX, (int) distanceY);
        touch_state = TouchState.SCROLL;
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

        canvas.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.LONG_PRESS);

        int offset_x = (int) (event.getX() - (canvas.getWidth() /2));
        int offset_y = (int) (event.getY() - (canvas.getHeight() / 2));

        touch_state = TouchState.DOUBLE_TOUCH;
        canvas.doubleTouchZoom(offset_x, offset_y);

        // not setting altered_canvas to true because we need to
        // restart the render via canvas.doubleTouchZoom method instead
        // see the anim.withEndAction() in animatedZoom()

        return true;
    }
    /* END OF implementation of onDoubleTapListener interface */


    /* implementation of OnScaleGestureListener interface */
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (blocked) return false;

        Tools.printDebug(DEBUG_TAG, "onScale: " + detector.toString());
        canvas.scaleBy(detector.getCurrentSpan() - detector.getPreviousSpan());

        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (blocked) return false;

        Tools.printDebug(DEBUG_TAG, "onScaleBegin: " + detector.toString());

        touch_state = TouchState.SCALE;
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        if (blocked) return;

        Tools.printDebug(DEBUG_TAG, "onScaleEnd: " + detector.toString());
        touch_state = TouchState.TOUCH;

        canvas.scaleCorrection();
        altered_canvas = true;
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
