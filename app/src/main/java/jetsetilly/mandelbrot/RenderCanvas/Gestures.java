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

    private final int DOUBLE_TOUCH_ZOOM_AMOUNT = 500;

    private RenderCanvas canvas;
    private boolean dirty_canvas;

    /* initialisation */
    public Gestures(Context context, final RenderCanvas canvas) {
        this.canvas = canvas;
        this.dirty_canvas = false;

        final GestureDetectorCompat gestures_detector = new GestureDetectorCompat(context, this);
        final ScaleGestureDetector scale_detector = new ScaleGestureDetector(context, this);

        gestures_detector.setOnDoubleTapListener(this);

        canvas.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                /* gestures_detector doesn't handle or expose ACTION_UP events!!
                this is necessary because we need to detect when a ACTION_MOVE event so
                that we can kick-start canvas rendering.

                note that onSingleTapUp() is not the same thing because it is not
                called after a ACTION_MOVE

                we also allow scale events to use the dirty_canvas flag. we could kick-start
                rendering in the onScaleEnd callback but instead we set the dirty_canvas flag to
                keep things consistent.
                */
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    Log.d(DEBUG_TAG, "onUp: " + event.toString());
                    if (dirty_canvas) {
                        canvas.startRender();
                        dirty_canvas = false;
                    }
                }

                boolean ret_val = scale_detector.onTouchEvent(event);
                return gestures_detector.onTouchEvent(event) || ret_val;
            }
        });
    }

    @Override
    public boolean onDown(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDown: " + event.toString());
        canvas.checkActionBar(event.getX(), event.getY());
        return true;
    }

    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        Log.d(DEBUG_TAG, "onFling: " + event1.toString()+event2.toString());
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        Log.d(DEBUG_TAG, "onLongPress: " + event.toString());
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.d(DEBUG_TAG, "onScroll: " + e1.toString() + e2.toString());
        canvas.scrollBy((int) distanceX, (int) distanceY);
        dirty_canvas = true;
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        Log.d(DEBUG_TAG, "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        Log.d(DEBUG_TAG, "onSingleTapUp: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString());

        canvas.offset_x = (int) (event.getX() - canvas.getCanvasMidX());
        canvas.offset_y = (int) (event.getY() - canvas.getCanvasMidY());

        canvas.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.LONG_PRESS);

        // defer displaying of zoomed image - this means that there
        // will be a zoomed image pointed to by display_bm but
        // which hasn't been "attached" to the ImageView
        // later in the startRender() method, this display_bm
        // will be scrolled and then displayed.
        //
        // TODO: a better way of doing that.
        canvas.zoomBy(DOUBLE_TOUCH_ZOOM_AMOUNT, true);
        canvas.startRender();

        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        Log.d(DEBUG_TAG, "onDoubleTapEvent: " + event.toString());
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        Log.d(DEBUG_TAG, "onSingleTapConfirmed: " + event.toString());
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        Log.d(DEBUG_TAG, "onScale: " + detector.toString());
        Log.d(DEBUG_TAG, "currentSpan: " + detector.getCurrentSpan());

        canvas.zoomBy((int) ((detector.getCurrentSpan()-detector.getPreviousSpan())) / 2);

        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        Log.d(DEBUG_TAG, "onScaleBegin: " + detector.toString());
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        Log.d(DEBUG_TAG, "onScaleEnd: " + detector.toString());
        dirty_canvas = true;
    }
}
