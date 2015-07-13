package jetsetilly.mandelbrot.RenderCanvas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.mandelbrot.Mandelbrot.MandelbrotCanvas;
import jetsetilly.mandelbrot.Palette.Settings;

public class RenderCanvas extends ImageView implements MandelbrotCanvas
{
    private final String DBG_TAG = "render canvas";

    private MainActivity context;
    private Gestures gestures;

    private Mandelbrot mandelbrot;
    private Bitmap display_bm, render_bm;
    private android.graphics.Canvas canvas;
    private Paint pnt;
    private Settings palette_settings = Settings.getInstance();

    private double zoom_factor;

    // the amount of deviation (offset) from the current render_bm
    // used when chaining scroll and zoom events
    // reset when new render_bm is created;
    // use getScrollX() and getScrollY() to retrieve current scroll values
    private int rendered_offset_x;
    private int rendered_offset_y;

    // mandelbrot movement depends on current zoom level
    // scroll_scale is used to add the correct weight to the scroll amount
    // value of scroll_scale is altered in zoomBy() function
    private double mandelbrot_offset_x;
    private double mandelbrot_offset_y;
    private double scroll_scale;

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
        this.gestures = new Gestures(context, this);
        pnt = new Paint();
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
        // iteration has already been limited to the colours size in MandelbrotQueue

        pnt.setColor(palette_settings.selected_palette.colours[iteration]);
        canvas.drawPoint(dx, dy, pnt);

        palette_settings.updateCount(iteration);
    }

    public void doDraw(float[] points, int points_len, int iteration)
    {
        // iteration has already been limited to the colours size in MandelbrotQueue

        pnt.setColor(palette_settings.selected_palette.colours[iteration]);
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

    public double getCanvasHypotenuse() { return Math.hypot(getHeight(), getWidth()); }

    public double getCanvasRatio() { return (double) getWidth() / (double) getHeight(); }

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
        if (context.action_bar.inActionBar(y)) {
            context.action_bar.hide(false);
            return false;
        }

        context.action_bar.hide(true);
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
        canvas = new android.graphics.Canvas(render_bm);

        // fill colour to first colour in current colours
        canvas.drawColor(palette_settings.mostFrequentColor());

        if (display_bm != null) {
            if (gestures.last_touch_state == Gestures.TouchState.SCALE) {
                canvas.drawBitmap(display_bm, 0, 0, null);
            } else {
                canvas.drawBitmap(display_bm, -getScrollX(), -getScrollY(), null);
                scrollTo(0, 0);
            }
        }

        // lose reference to old bitmap(s)
        display_bm = render_bm;
        setImageBitmap(display_bm);

        // reset colours count
        palette_settings.resetCount();

        // start render thread
        mandelbrot.startRender(mandelbrot_offset_x, mandelbrot_offset_y, zoom_factor);

        zoom_factor = 0;
        rendered_offset_x = 0;
        rendered_offset_y = 0;
        mandelbrot_offset_x = 0;
        mandelbrot_offset_y = 0;
        scroll_scale = 1;
    }
    /* end of render control */

    @Override
    public void scrollBy(int x, int y) {
        stopRender(); // stop render to avoid smearing
        super.scrollBy(x, y);
        updateOffsets(x, y);
    }

    private void updateOffsets(int offset_x, int offset_y) {
        rendered_offset_x += offset_x;
        rendered_offset_y += offset_y;
        mandelbrot_offset_x += offset_x * scroll_scale;
        mandelbrot_offset_y += offset_y * scroll_scale;
    }

    public void animatedZoom(int amount, int offset_x, int offset_y) {
        stopRender(); // stop render to avoid smearing

        updateOffsets(offset_x, offset_y);
        final Bitmap zoomed_bm = zoomImage(amount, true);

        ViewPropertyAnimator anim = animate();

        float scale = 1 / (float) (amount / getCanvasHypotenuse());

        anim.x(-offset_x * scale);
        anim.y(-offset_y * scale);
        anim.scaleX(scale);
        anim.scaleY(scale);
        anim.setDuration(1000);

        anim.withEndAction(new Runnable() {
            @Override
            public void run() {
                setScaleX(1f);
                setScaleY(1f);
                setX(0f);
                setY(0f);
                setImageBitmap(display_bm = zoomed_bm);
                postInvalidate();
                startRender();
            }
        });

        anim.start();
    }

    public void zoomBy(int amount) {
        stopRender(); // stop render to avoid smearing
        zoomImage(amount, false);
    }

    private Bitmap zoomImage(int amount, boolean defer) {
        double new_left, new_right, new_top, new_bottom;
        Bitmap offset_bm, zoomed_bm;
        Rect blit_to, blit_from;

        // calculate zoom
        zoom_factor += amount / getCanvasHypotenuse();

        // use render bitmap to do the zoom - this allows us to chain calls to the zoom routine
        // without the zoom_factor going crazy or losing definition

        /// do offset
        offset_bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);
        canvas = new android.graphics.Canvas(offset_bm);
        canvas.drawColor(palette_settings.mostFrequentColor());
        canvas.drawBitmap(render_bm, -rendered_offset_x, -rendered_offset_y, null);
        scrollTo(0, 0);

        // do zoom
        new_left = zoom_factor * getWidth();
        new_right = getWidth() - new_left;
        new_top = zoom_factor * getHeight();
        new_bottom = getHeight() - new_top;

        zoomed_bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.RGB_565);
        canvas = new android.graphics.Canvas(zoomed_bm);

        blit_to = new Rect(0, 0, getWidth(), getHeight());
        blit_from = new Rect((int)new_left, (int) new_top, (int) new_right, (int) new_bottom);

        canvas.drawColor(palette_settings.mostFrequentColor());
        canvas.drawBitmap(offset_bm, blit_from, blit_to, null);

        // image zoomed so scrolling needs a new scroll_scale
        scroll_scale = (new_right - new_left) / getWidth();

        if (!defer) {
            setImageBitmap(display_bm = zoomed_bm);
        }

        return zoomed_bm;
    }
}
