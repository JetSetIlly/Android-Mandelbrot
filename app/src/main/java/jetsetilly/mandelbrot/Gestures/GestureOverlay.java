package jetsetilly.mandelbrot.Gestures;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.R;
import jetsetilly.tools.LogTools;

public class GestureOverlay extends ImageView implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener
{
    private static final String DBG_TAG = "gesture overlay";

    MainActivity context;

    private GestureHandler gesture_handler;

    // gestures will be ignored so long as pause_zoom == true
    private boolean pause_zoom;
    private ImageView pause_icon;
    private long pause_icon_time;
    private final long MIN_PAUSE_ICON_DURATION = 1000;

    // whether the canvas has been altered somehow
    private boolean altered_canvas;

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
    public void setup(MainActivity context, final GestureHandler gesture_handler) {
        this.context = context;
        this.gesture_handler = gesture_handler;
        this.pause_zoom = false;
        this.scaling_canvas = false;

        final GestureDetectorCompat gestures_detector = new GestureDetectorCompat(context, this);
        final ScaleGestureDetector scale_detector = new ScaleGestureDetector(context, this);
        scale_detector.setQuickScaleEnabled(false);
        gestures_detector.setOnDoubleTapListener(this);

        pause_icon = (ImageView) context.findViewById(R.id.pause_icon);

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
                    if (altered_canvas) {
                        altered_canvas = false;
                        LogTools.printDebug(DBG_TAG, "onUp (after altered canvas): " + event.toString());
                        gesture_handler.finishManualGesture();
                    }
                }

                boolean scale_ret = scale_detector.onTouchEvent(event);
                return gestures_detector.onTouchEvent(event) || scale_ret;
            }
        });
    }
    /* END OF initialisation */

    public void pauseZoom(boolean show_pause_icon) {
        pause_zoom = true;
        if (show_pause_icon) {
            pause_icon_time = System.currentTimeMillis();
            pause_icon.setVisibility(VISIBLE);
        }
    }

    public void unpauseZoom() {
        pause_zoom = false;
        long delay_time = System.currentTimeMillis() - pause_icon_time;
        postDelayed(new Runnable() {
            @Override
            public void run() {
                ViewPropertyAnimator anim = pause_icon.animate();
                anim.alpha(0.0f);
                anim.withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        pause_icon.setVisibility(INVISIBLE);
                        pause_icon.setAlpha(1.0f);
                    }
                });
                anim.start();
            }
        }, MIN_PAUSE_ICON_DURATION - delay_time);
    }

    /* implementation of onGestureListener */
    @Override
    public boolean onDown(MotionEvent event) {
        LogTools.printDebug(DBG_TAG, "onDown: " + event.toString());
        gesture_handler.checkActionBar(event.getX(), event.getY(), false);
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (scaling_canvas) return true;

        LogTools.printDebug(DBG_TAG, "onScroll: " + e1.toString() + e2.toString());
        gesture_handler.scroll((int) distanceX, (int) distanceY);
        altered_canvas = true;
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        LogTools.printDebug(DBG_TAG, "onLongPress: " + event.toString());
        gesture_handler.autoZoom((int) event.getX(), (int) event.getY(), true);
    }
    /* END OF implementation of onGestureListener */


    /* implementation of onDoubleTapListener interface */
    @Override
    public boolean onDoubleTap(MotionEvent event) {
        if (pause_zoom) {
            // call pauseZoom() but force display of icon in case it's not already visible
            pauseZoom(true);
            return true;
        }

        LogTools.printDebug(DBG_TAG, "onDoubleTap: " + event.toString());
        gesture_handler.autoZoom((int) event.getX(), (int) event.getY(), false);
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent event) {
        LogTools.printDebug(DBG_TAG, "onSingleTapConfirmed: " + event.toString());
        gesture_handler.checkActionBar(event.getX(), event.getY(), true);
        return true;
    }
    /* END OF implementation of onDoubleTapListener interface */


    /* implementation of OnScaleGestureListener interface */
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (pause_zoom) return true;

        LogTools.printDebug(DBG_TAG, "onScaleBegin: " + detector.toString());
        scaling_canvas = true;
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        LogTools.printDebug(DBG_TAG, "onScale: " + detector.toString());
        gesture_handler.manualZoom(detector.getCurrentSpan() - detector.getPreviousSpan());
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        LogTools.printDebug(DBG_TAG, "onScaleEnd: " + detector.toString());
        gesture_handler.endManualZoom();
        scaling_canvas = false;
        altered_canvas = true;
    }
    /* END OF implementation of OnScaleGesture interface */


    /* UNUSED METHODS */
    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        /* implementation of onGestureListener */
        LogTools.printDebug(DBG_TAG, "onFling: " + event1.toString() + event2.toString());
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        /* implementation of onGestureListener */
        LogTools.printDebug(DBG_TAG, "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        /* implementation of onGestureListener */
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        /* implementation of onDoubleTapListener interface */
        return true;
    }

    /* END OF UNUSED METHODS */
}
