package jetsetilly.mandelbrot.RenderCanvas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.mandelbrot.Mandelbrot.MandelbrotCanvas;
import jetsetilly.mandelbrot.Settings.PaletteSettings;
import jetsetilly.mandelbrot.Settings.GestureSettings;

public class RenderCanvas extends ImageView implements MandelbrotCanvas
{
    private final String DBG_TAG = "render canvas";

    private MainActivity main_activity;
    private Gestures gestures;

    private Mandelbrot mandelbrot;

    // the display_bm is a pointer to whatever bitmap is currently displayed
    // whenever setImageBitmap() is called we should set display_bm to equal
    // whatever the Bitmap is being sent
    // the idiosyncratic ways to do this is setImageBitmap(display_bm = bm)
    // I considered overloading the setImageBitmap() method but that's too clumsy
    // a solution IMO
    private Bitmap display_bm;

    private Bitmap render_bm;
    private Buffer buffer;

    private final PaletteSettings palette_settings = PaletteSettings.getInstance();
    private final GestureSettings gesture_settings = GestureSettings.getInstance();

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

    /* initialisation */
    public RenderCanvas(Context context) {
        super(context);
        init(context);
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
        this.main_activity = (MainActivity) context;
        this.gestures = new Gestures(context, this);
    }

    private void clearImage() {
        Bitmap clear_bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas clear_canvas = new Canvas(clear_bm);
        clear_canvas.drawColor(palette_settings.mostFrequentColor());
        setImageBitmap(display_bm = clear_bm);
    }

    public void startCanvas() {
        // kill any existing image
        clearImage();

        // set background color
        setBackgroundColor(palette_settings.mostFrequentColor());

        mandelbrot = new Mandelbrot(main_activity, this);
        startRender();
    }
    /* end of initialisation */

    /* MandelbrotCanvas implementation */
    public void startDraw(Mandelbrot.RenderMode render_mode) {
        if (render_mode == Mandelbrot.RenderMode.MIN_TO_MAX) {
            buffer = new BufferPixels(this);
        } else {
            buffer = new BufferPixels(this);
        }

        buffer.primeBuffer(display_bm);
    }

    public void drawPoint(float dx, float dy, int iteration)
    {
        buffer.pushDraw(dx, dy, iteration);
    }

    public void update() {
        buffer.flush(display_bm, false);
    }

    public void endDraw() {
        // NOT the same as update() - we're passing true to the buffer.flush() function
        buffer.flush(display_bm, true);
        buffer = null;
    }

    /* use ImageView implementations getWidth() and getHeight() */

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
        if (main_activity.action_bar.inActionBar(y)) {
            main_activity.action_bar.hide(false);
            return false;
        }

        main_activity.action_bar.hide(true);
        return true;
    }
    /* end of property functions */

    /* render control */
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

        render_bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas render_canvas = new Canvas(render_bm);

        // fill colour to first colour in current colours
        render_canvas.drawColor(palette_settings.mostFrequentColor());

        if (display_bm != null) {
            if (gestures.has_scaled) {
                render_canvas.drawBitmap(display_bm, 0, 0, null);
            } else {
                render_canvas.drawBitmap(display_bm, -getScrollX(), -getScrollY(), null);
                scrollTo(0, 0);
            }
        }

        // lose reference to old bitmap(s)
        setImageBitmap(display_bm = render_bm);

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

    public void doubleTouchZoom(int offset_x, int offset_y) {
        animatedZoom(gesture_settings.double_tap_scale, offset_x, offset_y);
    }

    public void animatedZoom(float scale, int offset_x, int offset_y) {
        // animation can take a while -- we don't gestures to be honoured
        // while the animation is taking place. call blockGestures() here
        // and unblockGestures() in the animation's endAction
        gestures.blockGestures();

        stopRender(); // stop render to avoid smearing

        updateOffsets(offset_x, offset_y);
        final Bitmap zoomed_bm = zoomImage(scale, true);

        // we're going to animate the image view not the bitmap itself
        // in other words, the image view is going to change size and show whatever
        // is behind the image view in the layout. in our case the thing that is behind the
        // image view is another image view. we use this background_view to keep the illusion
        // that the bitmap is scaling. to do this we set the background color of background_view
        // to palette_settings.mostFrequentColor()
        //
        // we'll remove this when we introduce tessellated bitmaps
        main_activity.background_view.setBackgroundColor(palette_settings.mostFrequentColor());

        // do animation
        ViewPropertyAnimator anim = animate();

        anim.withLayer();
        anim.setDuration(1000);
        anim.x(-offset_x * scale);
        anim.y(-offset_y * scale);
        anim.scaleX(scale);
        anim.scaleY(scale);

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
                gestures.unblockGestures();
            }
        });

        anim.start();
    }

    public void zoomBy(int pixels) {
        stopRender(); // stop render to avoid smearing
        zoomImage(scaleFromPixels(pixels), false);
    }

    private Bitmap zoomImage(float scale, boolean defer) {
        double new_left, new_right, new_top, new_bottom;
        Canvas zoom_canvas;
        Bitmap offset_bm, zoomed_bm;
        Rect blit_to, blit_from;

        // calculate zoom
        zoom_factor += zoomFactorFromScale(scale);

        // use render bitmap to do the zoom - this allows us to chain calls to the zoom routine
        // without the zoom_factor going crazy or losing definition

        /// do offset
        offset_bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        zoom_canvas = new Canvas(offset_bm);
        zoom_canvas.drawColor(palette_settings.mostFrequentColor());
        zoom_canvas.drawBitmap(render_bm, -rendered_offset_x, -rendered_offset_y, null);
        scrollTo(0, 0);

        // do zoom
        new_left = zoom_factor * getWidth();
        new_right = getWidth() - new_left;
        new_top = zoom_factor * getHeight();
        new_bottom = getHeight() - new_top;
        zoomed_bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        zoom_canvas = new Canvas(zoomed_bm);

        blit_to = new Rect(0, 0, getWidth(), getHeight());
        blit_from = new Rect((int)new_left, (int) new_top, (int) new_right, (int) new_bottom);

        zoom_canvas.drawColor(palette_settings.mostFrequentColor());
        zoom_canvas.drawBitmap(offset_bm, blit_from, blit_to, null);

        // image zoomed so scrolling needs a new scroll_scale
        scroll_scale = (new_right - new_left) / getWidth();

        if (!defer) {
            setImageBitmap(display_bm = zoomed_bm);
        }

        return zoomed_bm;
    }

    private double factorFromPixels(int pixels) {
        return pixels / Math.hypot(getHeight(), getWidth());
    }

    private float scaleFromPixels(int pixels) {
        return (float) getWidth() / (float) ( (getWidth() - 2 * factorFromPixels(pixels) * getWidth()) );
    }

    private float zoomFactorFromScale(float scale) {
        return - (getWidth() - (scale * getWidth())) / (2 * scale * getWidth());
    }
}


