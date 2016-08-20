package jetsetilly.mandelbrot.Gestures;

import android.content.Context;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
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
import jetsetilly.tools.SimpleRunOnUI;

public class GestureOverlay extends ImageView implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener
{
    private static final String DBG_TAG = LogTools.NO_LOG_PREFIX + "gesture overlay";

    MainActivity context;

    private GestureHandler gesture_handler;
    private ImageView gesture_block_icon;
    private long gesture_block_icon_visibility_time;
    private final long MIN_GESTURE_BLOCK_ICON_SHOW_DURATION = 1000;

    // gestures will be ignored so long as blocked == true
    private boolean blocked;

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
        this.blocked = false;
        this.scaling_canvas = false;

        final GestureDetectorCompat gestures_detector = new GestureDetectorCompat(context, this);
        final ScaleGestureDetector scale_detector = new ScaleGestureDetector(context, this);
        scale_detector.setQuickScaleEnabled(false);
        gestures_detector.setOnDoubleTapListener(this);

        gesture_block_icon = (ImageView) ((AppCompatActivity)context).findViewById(R.id.gesture_block);

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

    public void block() {
        blocked = true;
    }

    public void unblock() {
        blocked = false;
        long delay_time = System.currentTimeMillis() - gesture_block_icon_visibility_time;
        if (delay_time > MIN_GESTURE_BLOCK_ICON_SHOW_DURATION) {
            SimpleRunOnUI.run(context, new Runnable() {
                @Override
                public void run() {
                    gesture_block_icon.setVisibility(INVISIBLE);
                }
            });
        } else {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    ViewPropertyAnimator anim = gesture_block_icon.animate();
                    anim.alpha(0.0f);
                    anim.withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            gesture_block_icon.setVisibility(INVISIBLE);
                            gesture_block_icon.setAlpha(1.0f);
                        }
                    });
                    anim.start();
                }
            }, MIN_GESTURE_BLOCK_ICON_SHOW_DURATION - delay_time);
        }
    }

    private boolean testBlock() {
        if (blocked) {
            gesture_block_icon_visibility_time = System.currentTimeMillis();
            gesture_block_icon.setVisibility(VISIBLE);
            return true;
        }
        return false;
    }

    /* implementation of onGesturesListener */
    @Override
    public boolean onDown(MotionEvent event) {
        LogTools.printDebug(DBG_TAG, "onDown: " + event.toString());
        gesture_handler.checkActionBar(event.getX(), event.getY(), false);
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (testBlock()) return false;
        if (scaling_canvas) return true;

        LogTools.printDebug(DBG_TAG, "onScroll: " + e1.toString() + e2.toString());
        gesture_handler.scroll((int) distanceX, (int) distanceY);
        altered_canvas = true;
        return true;
    }

    @Override
    public void onLongPress(MotionEvent event) {
        if (testBlock()) return;

        LogTools.printDebug(DBG_TAG, "onLongPress: " + event.toString());

        // no offset when we're zooming out

        gesture_handler.autoZoom(0, 0, true);
    }
    /* END OF implementation of onGesturesListener */


    /* implementation of onDoubleTapListener interface */
    @Override
    public boolean onDoubleTap(MotionEvent event) {
        if (testBlock()) return false;

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
        if (testBlock()) return false;

        LogTools.printDebug(DBG_TAG, "onScaleBegin: " + detector.toString());
        scaling_canvas = true;
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (testBlock()) return false;

        LogTools.printDebug(DBG_TAG, "onScale: " + detector.toString());
        gesture_handler.manualZoom(detector.getCurrentSpan() - detector.getPreviousSpan());
        altered_canvas = true;

        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        if (testBlock()) return;

        LogTools.printDebug(DBG_TAG, "onScaleEnd: " + detector.toString());
        gesture_handler.endManualZoom(false);
        scaling_canvas = false;
    }
    /* END OF implementation of OnScaleGesture interface */


    /* UNUSED METHODS */
    @Override
    public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
        /* implementation of onGesturesListener */

        //if (testBlock()) return false;
        LogTools.printDebug(DBG_TAG, "onFling: " + event1.toString() + event2.toString());
        return true;
    }

    @Override
    public void onShowPress(MotionEvent event) {
        /* implementation of onGesturesListener */

        //if (testBlock()) return;
        LogTools.printDebug(DBG_TAG, "onShowPress: " + event.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent event) {
        /* implementation of onGesturesListener */

        //if (testBlock()) return false;
        LogTools.printDebug(DBG_TAG, "onSingleTapUp: " + event.toString());
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent event) {
        /* implementation of onDoubleTapListener interface */

        //if (testBlock()) return false;
        LogTools.printDebug(DBG_TAG, "onDoubleTapEvent: " + event.toString());
        return true;
    }

    /* END OF UNUSED METHODS */
}
