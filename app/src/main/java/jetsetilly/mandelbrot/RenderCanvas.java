package jetsetilly.mandelbrot;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

public class RenderCanvas extends ImageView implements MandelbrotCanvas, View.OnTouchListener {
    private final String DBG_TAG = "render canvas";

    private final double ZOOM_SATURATION = 0.65; // 0 = gray scale, 1 = identity

    private MainActivity context;
    private RenderCanvasTouch touch;

    private Mandelbrot mandelbrot;
    private Bitmap display_bm, render_bm;
    private Canvas canvas;
    private Paint pnt;
    private PaletteDefinitions palette_settings = PaletteDefinitions.getInstance();

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
        this.touch = new RenderCanvasTouch(this);

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
        // iteration has already been limited to the palette size in MandelbrotQueue

        pnt.setColor(palette_settings.palette[iteration]);
        canvas.drawPoint(dx, dy, pnt);

        palette_settings.updateCount(iteration);
    }

    public void doDraw(float[] points, int points_len, int iteration)
    {
        // iteration has already been limited to the palette size in MandelbrotQueue

        pnt.setColor(palette_settings.palette[iteration]);
        canvas.drawPoints(points, 0, points_len, pnt);

        palette_settings.updateCount(iteration);
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

    /* property functions */
    public double getCanvasMidX() {
        return getWidth() / 2;
    }

    public double getCanvasMidY() {
        return getHeight() / 2;
    }

    public Bitmap getDisplayedBitmap() {
        return display_bm;
    }

    public boolean checkActionBar(float x, float y) {
        // returns false if coordinates are in action bar, otherwise true
        if (context.inActionBar(y)) {
            context.hideActionBar(false);
            return false;
        }

        context.hideActionBar(true);
        return true;
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
        stopRender();

        render_bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);
        canvas = new Canvas(render_bm);

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
        display_bm = render_bm;
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
        Rect blit = new Rect(0, 0, getWidth(), getHeight());
        Bitmap tmp_bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);

        // using temporary canvas so we don't clobber the real canvas
        Canvas tmp_canvas = new Canvas(tmp_bm);

        // we're resetting because some palettes don't like it if we don't. no, this doesn't make sense to me either
        // TODO: understand why we need to do this
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

    public void zoomBy(int amount) {
        zoomBy(amount, false);
    }

    public void zoomBy(int amount, boolean deferred_display) {
        double new_left, new_right, new_top, new_bottom;
        double zoom_factor;
        Bitmap tmp_bm;
        Rect blit_to, blit_from;

        stopRender(); // stop render to avoid smearing

        // calculate zoom
        zoom_amount += amount;
        zoom_factor = zoom_amount / Math.hypot(getHeight(), getWidth());
        Log.d(DBG_TAG, "zf: " + zoom_factor);

        /// do offset
        tmp_bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);
        canvas = new Canvas(tmp_bm);

        // use render bitmap to do the zoom - this allows us to chain calls to the zoom routine
        // without the zoom_factor going crazy or losing definition
        canvas.drawBitmap(render_bm, (int) (-offset_x * zoom_factor * 2), (int) (-offset_y * zoom_factor * 2), null);

        // do zoom
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

    // implements View.OnTouchListener
    public boolean onTouch(View view, MotionEvent event) {
        return touch.onTouch(view, event);
    }
}
