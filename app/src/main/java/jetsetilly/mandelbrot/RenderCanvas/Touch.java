package jetsetilly.mandelbrot.RenderCanvas;

import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

public class Touch {
    private final String DBG_TAG = "render canvas touch";

    private final long DOUBLE_TOUCH_TIME = 500000000;
    private final int DOUBLE_TOUCH_ZOOM_AMOUNT = 500;
    private final float TOUCH_SENSITIVITY = 50;

    private RenderCanvas canvas;

    private enum TouchState {IDLE, TOUCH, MOVE, DOUBLE_TOUCH}

    private long touch_time = 0;

    private int touch_id = -1;
    private TouchState touch_state = TouchState.IDLE;
    private float touch_x, touch_y;

    private int second_touch_id = -1;
    private TouchState second_touch_state = TouchState.IDLE;
    private float second_touch_x, second_touch_y;

    public Touch(RenderCanvas canvas)
    {
        this.canvas = canvas;
    }

    private boolean touchSensitivity(float point_a, float point_b) {
        return (point_a - point_b >= -TOUCH_SENSITIVITY) && (point_a - point_b <= TOUCH_SENSITIVITY);
    }

    public boolean onTouch(View view, MotionEvent event) {
        float new_x, new_y;

        switch (event.getActionMasked())
        {
            case MotionEvent.ACTION_DOWN:
                long new_time = System.nanoTime();

                touch_id = event.getPointerId(0);
                new_x = event.getX();
                new_y = event.getY();

                Log.d(DBG_TAG, "x: " + new_x + " y: " + new_y);

                if (new_time - touch_time < DOUBLE_TOUCH_TIME && touchSensitivity(new_x, touch_x) && touchSensitivity(new_y, touch_y)) {
                    touch_state = TouchState.DOUBLE_TOUCH;
                    int offset_x = (int) (new_x - canvas.getCanvasMidX());
                    int offset_y = (int) (new_y - canvas.getCanvasMidY());
                    canvas.scrollBy(offset_x, offset_y);

                    canvas.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.LONG_PRESS);

                    // defer displaying of zoomed image - this means that there
                    // will be a zoomed image pointed to by display_bm but
                    // which hasn't been "attached" to the ImageView
                    // later in the startRender() method, this display_bm
                    // will be scrolled and then displayed.
                    canvas.zoomBy(DOUBLE_TOUCH_ZOOM_AMOUNT);
                } else {
                    if (canvas.checkActionBar(new_x, new_y)) {
                        touch_state = TouchState.TOUCH;
                        //canvas.zoom_amount = 0;
                    }
                }

                touch_time = new_time;
                touch_x = new_x;
                touch_y = new_y;

                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                // only handle second touch if first touch hasn't
                // start to do anything
                if (touch_state == TouchState.MOVE)
                    break;

                // no need to record touches after the second
                if (second_touch_id == -1)  {
                    int second_touch_idx = event.getActionIndex();
                    second_touch_id = event.getPointerId(second_touch_idx);
                    second_touch_state = TouchState.TOUCH;
                    second_touch_x = event.getX(second_touch_idx);
                    second_touch_y = event.getY(second_touch_idx);

                    Log.d(DBG_TAG, "initial second touch id: " + second_touch_id);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // we will still receive move events in the event of initial touch events
                // being cancelled so we break early in this case
                if (touch_id == -1 && second_touch_id == -1)
                    break;

                int this_touch_idx = event.getActionIndex();
                int this_touch_id = event.getPointerId(this_touch_idx);

                new_x = event.getX(this_touch_idx);
                new_y = event.getY(this_touch_idx);

                if (second_touch_id != -1 && touch_state != TouchState.MOVE ) {

                    if (new_x != second_touch_x && new_y != second_touch_y) {
                        int zoom_amount;

                        second_touch_state = TouchState.MOVE;

                        if (this_touch_id == touch_id) {
                            zoom_amount = (int) (touch_y - new_y);
                        } else {
                            zoom_amount = (int) (second_touch_y - new_y);
                        }

                        // limit zoom amount
                        zoom_amount = (int) (zoom_amount * 1.5);

                        if (touch_y > second_touch_y)
                            zoom_amount = -zoom_amount;

                        canvas.zoomBy(zoom_amount);
                    }

                } else if (second_touch_state != TouchState.MOVE) {
                    if (new_x != touch_x && new_y != touch_y) {
                        touch_state = TouchState.MOVE;
                        canvas.scrollBy((int) (touch_x - new_x), (int) (touch_y - new_y));
                    }
                }

                // update (second) touch coordinates
                if (this_touch_id == touch_id) {
                    touch_x = new_x;
                    touch_y = new_y;
                } else {
                    second_touch_x = new_x;
                    second_touch_y = new_y;
                }

                break;

            case MotionEvent.ACTION_POINTER_UP:
                if (second_touch_id == -1)
                    break;

                /*** deliberate fall through of case statement ***/

            case MotionEvent.ACTION_UP:
                if (touch_state == TouchState.MOVE || touch_state == TouchState.DOUBLE_TOUCH || second_touch_state == TouchState.MOVE) {
                    canvas.startRender();
                }

                // cancel both touch events to prevent weird movement/zooming
                touch_id = -1;
                touch_state = TouchState.IDLE;
                second_touch_id = -1;
                second_touch_state = TouchState.IDLE;

                break;
        }

        return true;
    }
}
