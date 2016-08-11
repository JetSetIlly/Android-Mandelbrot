package jetsetilly.mandelbrot.Gestures;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Process;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import java.util.concurrent.Semaphore;

import jetsetilly.tools.LogTools;

public class GestureOverlay extends ImageView implements
        GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener,
        ScaleGestureDetector.OnScaleGestureListener
{
    private static final String DEBUG_TAG = LogTools.NO_LOG_PREFIX + "gesture overlay";
    private static final long ADDITIONAL_ON_UP_DELAY = 0;

    private GestureHandler gesture_handler;

    // gestures will be ignored so long as blocked == true
    private boolean blocked;
    private Semaphore up_delay_sem;
    private Runnable up_delay_runnable;
    private AsyncTask up_delay_thr;

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
    public void setup(Context context, final GestureHandler canvas) {
        this.gesture_handler = canvas;
        this.blocked = false;
        this.up_delay_sem = new Semaphore(1);
        this.up_delay_runnable = null;
        this.up_delay_thr = null;
        this.scaling = false;

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
                after a scroll event
                */
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    if (altered_canvas) {
                        LogTools.printDebug(DEBUG_TAG, "onUp (after altered_canvas): " + event.toString());

                        if (up_delay_thr == null || up_delay_thr.getStatus() != AsyncTask.Status.RUNNING) {
                            // separate thread to test for delay
                            // allows animated zoom to finish before running canvas.finishGesture()
                            up_delay_thr = new AsyncTask() {
                                @Override
                                protected Object doInBackground(Object[] params) {
                                    Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);
                                    try {
                                        up_delay_sem.acquire();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    up_delay_sem.release();

                                    // sleep for ADDITIONAL_ON_UP_DELAY milliseconds
                                    synchronized (this) {
                                        try {
                                            if (ADDITIONAL_ON_UP_DELAY > 0)
                                                wait(ADDITIONAL_ON_UP_DELAY);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    return null;
                                }

                                @Override
                                protected void onPostExecute(Object o) {
                                    super.onPostExecute(o);
                                    if (up_delay_runnable != null) {
                                        up_delay_runnable.run();
                                        up_delay_runnable = null;
                                    }

                                    canvas.finishGesture();
                                }

                                @Override
                                protected void onCancelled() {
                                    super.onCancelled();
                                    LogTools.printDebug(DEBUG_TAG, "up_delay_thr.onCancelled()");
                                }
                            };

                            up_delay_thr.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        }
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
        try {
            up_delay_sem.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void unblock(Runnable runnable) {
        blocked = false;
        up_delay_runnable = runnable;
        up_delay_sem.release();
    }

    /* implementation of onGesturesListener */
    @Override
    public boolean onDown(MotionEvent event) {
        if (blocked) return false;

        if (up_delay_thr != null) {
            up_delay_thr.cancel(true);
        }

        LogTools.printDebug(DEBUG_TAG, "onDown: " + event.toString());
        gesture_handler.checkActionBar(event.getX(), event.getY(), false);
        altered_canvas = false;
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (blocked) return false;
        if (scaling) return true;

        LogTools.printDebug(DEBUG_TAG, "onScroll: " + e1.toString() + e2.toString());
        gesture_handler.scroll((int) distanceX, (int) distanceY);
        altered_canvas = true;
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
        altered_canvas = true;
    }

    /* END OF implementation of onGesturesListener */


    /* implementation of onDoubleTapListener interface */
    @Override
    public boolean onDoubleTap(MotionEvent event) {
        if (blocked) return false;

        LogTools.printDebug(DEBUG_TAG, "onDoubleTap: " + event.toString());

        gesture_handler.animatedZoom((int) event.getX(), (int) event.getY(), false);
        altered_canvas = true;

        return true;
    }
    /* END OF implementation of onDoubleTapListener interface */


    /* implementation of OnScaleGestureListener interface */
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (blocked) return false;

        LogTools.printDebug(DEBUG_TAG, "onScaleBegin: " + detector.toString());
        scaling = true;
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
        altered_canvas = true;
        scaling = false;
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
