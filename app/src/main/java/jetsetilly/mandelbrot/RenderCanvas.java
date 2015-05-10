package jetsetilly.mandelbrot;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RenderCanvas extends ImageView implements View.OnTouchListener, MandelbrotCanvas  {
    private final String DBG_TAG = "render canvas";
    private final long DOUBLE_TOUCH_TIME = 500000000;
    private final int DOUBLE_TOUCH_ZOOM_AMOUNT = 500;

    private final double ZOOM_SATURATION = 0.65; // 0 = gray scale, 1 = identity

    private MainActivity context;

    private Mandelbrot mandelbrot;
    private Paint pnt;
    private Bitmap display_bm;
    private Bitmap render_bm;       // display and render bitmaps are the same until we start the zoom process
    private Canvas canvas;
    private PaletteDefinitions palette_settings = PaletteDefinitions.getInstance();

    private long touch_time = 0;
    private int touch_id = -1;
    private enum TouchState {IDLE, TOUCH, MOVE, DOUBLE_TOUCH}
    private TouchState touch_state = TouchState.IDLE;
    private float touch_x, touch_y;

    private int second_touch_id = -1;
    private TouchState second_touch_state = TouchState.IDLE;
    private float second_touch_x, second_touch_y;

    public int zoom_amount; // cumulative on touch events. resets to zero on down event
    public int offset_x; // offset_x and offset_y could be attained by calling getScrollX but
    public int offset_y; // I think keeping our own books makes the intention clearer

    /* filter to apply to zoomed images */
    ColorMatrixColorFilter zoom_color_filter;
    ColorMatrix zoom_color_matrix;

    /* initialisation */
    public RenderCanvas(Context context) {
        super(context);
    }

    public RenderCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RenderCanvas(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        this.context = (MainActivity) context;

        pnt = new Paint();

        zoom_color_matrix = new ColorMatrix();
        zoom_color_matrix.setSaturation((float) ZOOM_SATURATION);
        zoom_color_filter = new ColorMatrixColorFilter(zoom_color_matrix);

        this.setOnTouchListener(this);
    }

    public void kickStartCanvas() {
        // set background color
        setBackgroundColor(palette_settings.mostFrequentColor());

        mandelbrot = new Mandelbrot(this);
        startRender();
    }
    /* end of initialisation */

    /* MandelbrotCanvas implementation */
    public void doDraw(float dx, float dy, int iteration)
    {
        int palette_entry = iteration % palette_settings.palette.length;

        pnt.setColor(palette_settings.palette[palette_entry]);
        canvas.drawPoint(dx, dy, pnt);

        palette_settings.updateCount(palette_entry);
    }

    public void doDraw(float[] points, int points_len, int iteration)
    {
        int palette_entry = iteration % palette_settings.palette.length;

        pnt.setColor(palette_settings.palette[palette_entry]);
        canvas.drawPoints(points, 0, points_len, pnt);

        palette_settings.updateCount(palette_entry);
    }

    public void update() {
        invalidate();
    }

    public int getCanvasWidth() {
        return getWidth();
    }

    public int getCanvasHeight() {
        return getHeight();
    }

    public int getPaletteSize() { return palette_settings.numColors(); }
    /* end of MandelbrotCanvas implementation */

    public boolean saveImage() {
        long curr_time = System.currentTimeMillis();

        // TODO: saving image to "Pictures" but it's not showing up in gallery - investigate why

        String title = String.format("%s_%s.png",
                context.getString(R.string.app_name),
                new SimpleDateFormat("ssmmhhddmmyyyy", Locale.ENGLISH).format(curr_time));

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, title);
        values.put(MediaStore.Images.Media.DESCRIPTION, context.getString(R.string.app_name));
        values.put(MediaStore.Images.Media.DATE_ADDED, curr_time);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

        ContentResolver cr = context.getContentResolver();
        Uri url = null;

        try {
            url = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            OutputStream o = cr.openOutputStream(url);
            display_bm.compress(Bitmap.CompressFormat.PNG, 100, o);

         } catch (Exception e) {
            if (url != null) {
                cr.delete(url, null, null);
            }

            return false;
        }

        return true;
    }

    /* property functions */
    public double getCanvasMidX() {
        return getWidth() / 2;
    }

    public double getCanvasMidY() {
        return getHeight() / 2;
    }

    boolean touchSensitivity(float point_a, float point_b) {
        if (point_a - point_b >= -40 && point_a - point_b <= 40) {
            return true;
        }

        return false;
    }
    /* end of property functions */

    /* render control */
    public void completeRender() {
        // change background colour - also done in stopRender() because this function
        // won't be called if the render is interrupted
        setBackgroundColor(palette_settings.mostFrequentColor());
    }

    public void stopRender() {
        // change background colour - we do it here so that we perform the change
        // it in all instances. when the render is interrupted by a touch event
        // and when startRender is called (startRender() calls stopRender() to make sure
        // there is nothing currently going on).
        setBackgroundColor(palette_settings.mostFrequentColor());

        mandelbrot.stopRender();
    }

    public void startRender() {
        Bitmap new_bm;

        stopRender();

        new_bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);
        canvas = new Canvas(new_bm);

        // fill colour to first colour in current palette
        canvas.drawColor(palette_settings.mostFrequentColor());

        if (display_bm != null) {
            if (zoom_amount != 0) {
                fadeDisplayBitmap();
            }

            // move bitmap
            canvas.drawBitmap(display_bm, -offset_x, -offset_y, null);
            scrollTo(0, 0); // reset scroll of image view
        }

        // lose reference to old bitmap(s)
        display_bm = render_bm = new_bm;
        setImageBitmap(display_bm);

        // reset palette count
        palette_settings.resetCount();

        // start render thread
        mandelbrot.startRender(offset_x, offset_y, zoom_amount);

        // offset and zoom amount is now meaningless
        // although these values are reset when we resume touch events later
        // we're taking a belt and braces approach to avoid accidents
        offset_x = 0;
        offset_y = 0;
        zoom_amount = 0;
    }
    /* end of render control */

    private void fadeDisplayBitmap() {
        Canvas tmp_canvas;  // using temporary canvas so we don't clobber the real canvas

        Rect blit = new Rect(0, 0, getWidth(), getHeight());
        Bitmap tmp_bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);
        tmp_canvas = new Canvas(tmp_bm);

        // we're resetting because some palettes don't like it if we don't. no, this doesn't make sense to me either
        pnt.reset();

        pnt.setColorFilter(zoom_color_filter);
        tmp_canvas.drawBitmap(display_bm, blit, blit, pnt);
        pnt.setColorFilter(null);

        display_bm = tmp_bm;
        setImageBitmap(display_bm);
    }

    @Override
    public void scrollBy(int x, int y) {
        stopRender(); // stop render to avoid smearing

        super.scrollBy(x, y);
        this.offset_x += x;
        this.offset_y += y;
    }

    private void zoomBy(int amount) {
        zoomBy(amount, false);
    }

    private void zoomBy(int amount, boolean deferred_display) {
        double new_left, new_right, new_top, new_bottom;
        double zoom_factor;
        Bitmap tmp_bm;
        Rect blit_to, blit_from;

        stopRender(); // stop render to avoid smearing

        /* calculate zoom */
        zoom_amount += amount;
        zoom_factor = zoom_amount / Math.hypot(getHeight(), getWidth());
        Log.d(DBG_TAG, "zf: " + zoom_factor);

        /* do offset */
        tmp_bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);
        canvas = new Canvas(tmp_bm);
        canvas.drawBitmap(render_bm, (int) (-offset_x * zoom_factor * 2), (int) (-offset_y * zoom_factor * 2), null);

        /* do zoom */
        new_left = zoom_factor * getWidth();
        new_right = getWidth() - new_left;
        new_top = zoom_factor * getHeight();
        new_bottom = getHeight() - new_top;

        display_bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);
        canvas = new Canvas(display_bm);

        blit_to = new Rect(0, 0, getWidth(), getHeight());
        blit_from = new Rect((int)new_left, (int)new_top, (int)new_right, (int)new_bottom);

        canvas.drawBitmap(tmp_bm, blit_from, blit_to, null);

        if (!deferred_display) {
            setImageBitmap(display_bm);
        }
    }

    /* event listeners */
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
                    offset_x = (int) (new_x - getCanvasMidX());
                    offset_y = (int) (new_y - getCanvasMidY());

                    performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.LONG_PRESS);

                    // defer displaying of zoomed image - this means that there
                    // will be a zoomed image pointed to by display_bm but
                    // which hasn't been "attached" to the ImageView
                    // later in the startRender() method, this display_bm
                    // will be scrolled and then displayed.
                    zoomBy(DOUBLE_TOUCH_ZOOM_AMOUNT, true);
                } else {
                    if (context.inActionBar(new_y)) {
                        context.hideActionBar(false);
                    } else {
                        touch_state = TouchState.TOUCH;
                        zoom_amount = 0;
                        offset_x = 0;
                        offset_y = 0;
                        context.hideActionBar(true);
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
                            zoom_amount = (int) (touch_y - new_y) * 2;
                        } else {
                            zoom_amount = (int) (second_touch_y - new_y) * 2;
                        }

                        if (touch_y > second_touch_y)
                            zoom_amount = -zoom_amount;

                        zoomBy(zoom_amount);
                    }

                } else if (second_touch_state != TouchState.MOVE) {
                    if (new_x != touch_x && new_y != touch_y) {
                        touch_state = TouchState.MOVE;
                        scrollBy((int) (touch_x - new_x), (int) (touch_y - new_y));
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
                    startRender();
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

    /* end of event listeners */
}
