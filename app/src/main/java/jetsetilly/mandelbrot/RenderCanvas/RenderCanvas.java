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
import jetsetilly.mandelbrot.R;
import jetsetilly.mandelbrot.Settings.PaletteSettings;
import jetsetilly.mandelbrot.Settings.GestureSettings;

public class RenderCanvas extends ImageView implements MandelbrotCanvas
{
    private final String DBG_TAG = "render canvas";

    private MainActivity main_activity;
    private Gestures gestures;
    private Mandelbrot mandelbrot;

    // that ImageView that sits behind RenderCanvas in the layout. we colour this image view
    // so that zooming the canvas doesn't expose the nothingness behind the canvas.
    private ImageView static_background;

    // the display_bm is a pointer to whatever bitmap is currently displayed
    // whenever setImageBitmap() is called we should set display_bm to equal
    // whatever the Bitmap is being sent
    // the idiosyncratic ways to do this is setImageBitmap(display_bm = bm)
    // I considered overloading the setImageBitmap() method but that's too clumsy
    // a solution IMO
    public Bitmap display_bm;

    // render_bm is sometimes equal to display_bm sometimes not.
    // 1. on startRender() the current  display_bm is used to copy into the new render_bm by the
    // correct offset (resetting the scroll  of the ImageView afterwards)
    // 2. when pinch zooming the zoom amount is applied to the render_bm and not the display_bm. this
    // prevents exponential zooming of the image
    private Bitmap render_bm;
    private Buffer buffer;

    private final PaletteSettings palette_settings = PaletteSettings.getInstance();
    private final GestureSettings gesture_settings = GestureSettings.getInstance();

    private double cumulative_zoom_factor;

    // the amount of deviation (offset) from the current render_bm
    // used when chaining scroll and zoom events
    // reset when new render_bm is created;
    // use getScrollX() and getScrollY() to retrieve current scroll values
    private int rendered_offset_x;
    private int rendered_offset_y;

    // mandelbrot movement depends on current zoom level
    // mandelbrot_scroll_scale is used to add the correct weight to the scroll amount
    // value of mandelbrot_scroll_scale is altered in zoomBy() function
    private double mandelbrot_offset_x;
    private double mandelbrot_offset_y;
    private double mandelbrot_scroll_scale;


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

    public void initPostLayout() {
        this.static_background = (ImageView) main_activity.findViewById(R.id.static_background);
        resetCanvas();
    }
    /* end of initialisation */

    private void clearImage() {
        Bitmap clear_bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        clear_bm.eraseColor(palette_settings.mostFrequentColor());
        setImageBitmap(display_bm = clear_bm);
    }

    public void resetCanvas() {
        // kill any existing image
        clearImage();

        // set base color
        setBackgroundColor(palette_settings.mostFrequentColor());

        mandelbrot = new Mandelbrot(main_activity, this);
        startRender();
    }

    public void checkActionBar(float x, float y, boolean show) {
        // returns false if coordinates are in action bar, otherwise true
        if (show) {
            if (main_activity.action_bar.inActionBar(y)) {
                main_activity.action_bar.hide(false);
            }
        } else {
            main_activity.action_bar.hide(true);
        }
    }

    /* MandelbrotCanvas implementation */
    public void startDraw(Mandelbrot.RenderMode render_mode) {
        /* flush buffer here whether we need to or not. this is a hacky solution to the problem of
         cancelling the Mandelbrot thread and restarting it before ASyncTask.onCancelled()
         has run. the scheduling of onCancelled() is unreliable and the new thread may have started
         in the meantime. calling buffer.flush() from onCancelled() may try to write to a non-existing
         buffer. worse, if it does run it will kill the buffer leaving the new task nothing to
         work with
         */
        if (buffer != null) buffer.flush(true);

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
        buffer.flush(false);
    }

    public void endDraw() {
        if (buffer != null) {
            buffer.flush(true);
            buffer = null;
        }
    }
    /* using ImageView implementations getWidth() and getHeight() */
    /* end of MandelbrotCanvas implementation */


    /* render control */
    public void startRender() {
        stopRender();

        render_bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas render_canvas = new Canvas(render_bm);

        // fill colour to first colour in current colours
        render_canvas.drawColor(palette_settings.mostFrequentColor());

        if (display_bm != null) {
            render_canvas.drawBitmap(display_bm, -getScrollX(), -getScrollY(), null);
            scrollTo(0, 0);
        }

        // lose reference to old bitmap(s)
        setImageBitmap(display_bm = render_bm);

        // reset colours count
        palette_settings.resetCount();

        // start render thread
        mandelbrot.startRender(mandelbrot_offset_x, mandelbrot_offset_y, cumulative_zoom_factor);

        cumulative_zoom_factor = 0;
        rendered_offset_x = 0;
        rendered_offset_y = 0;
        mandelbrot_offset_x = 0;
        mandelbrot_offset_y = 0;
        mandelbrot_scroll_scale = 1;
    }

    public void stopRender() {
        // change base colour - we do it here so that we perform the change
        // it in all instances. when the render is interrupted by a touch event
        // and when startRender is called (startRender() calls stopRender() to make sure
        // there is nothing currently going on).
        setBackgroundColor(palette_settings.mostFrequentColor());

        mandelbrot.stopRender();
    }
    /* end of render control */


    /* canvas transformation */
    private void updateOffsets(int offset_x, int offset_y) {
        rendered_offset_x += offset_x;
        rendered_offset_y += offset_y;
        mandelbrot_offset_x += offset_x * mandelbrot_scroll_scale;
        mandelbrot_offset_y += offset_y * mandelbrot_scroll_scale;
    }

    @Override
    public void scrollBy(int x, int y) {
        stopRender(); // stop render to avoid smearing
        super.scrollBy(x, y);
        updateOffsets(x, y);
    }

    public void doubleTouchZoom(int offset_x, int offset_y) {
        animatedZoom(gesture_settings.double_tap_scale, offset_x, offset_y);
    }

    private void animatedZoom(float scale, int offset_x, int offset_y) {
        // animation can take a while -- we don't gestures to be honoured
        // while the animation is taking place. call blockGestures() here
        // and unblockGestures() in the animation's endAction
        gestures.blockGestures();

        // stop render to avoid smearing
        stopRender();

        // update offsets ready for the new render
        updateOffsets(offset_x, offset_y);

        // update zoom factor
        cumulative_zoom_factor += zoomFactorFromScale(scale);

        // generate final zoomed image
        final Bitmap zoomed_bm = getZoomedImage();

        // we're going to animate the image view not the bitmap itself
        // in other words, the image view is going to change size and show whatever
        // is behind the image view in the layout. in our case the thing that is behind the
        // image view is another image view. we use this static background to keep the illusion
        // that the bitmap is scaling. to do this we set the background color of static_background
        // to palette_settings.mostFrequentColor()
        //
        // we'll remove this when we introduce tessellated/giant bitmaps
        static_background.setBackgroundColor(palette_settings.mostFrequentColor());

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

        /*
        // OLD FASHIONED ANIMATION (pre 3.0)
        // set up zoom animation
        Animation scale_anim = new ScaleAnimation(1.0f, scale, 1.0f, scale, (float)getCanvasMidX(), (float)getCanvasMidY());
        Animation translate_anim = new TranslateAnimation(
                0,
                -offset_x*scale,
                0,
                -offset_y*scale
        );
        AnimationSet zoom_anim = new AnimationSet(true);
        zoom_anim.addAnimation(scale_anim);
        zoom_anim.addAnimation(translate_anim);
        zoom_anim.setDuration(1000);
        zoom_anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setScaleX(1f);
                setScaleY(1f);
                setX(0f);
                setY(0f);
                setImageBitmap(display_bm = zoomed_bm);
                startRender();
                gestures.unblockGestures();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        startAnimation(zoom_anim);
        // END OF OLD FASHIONED ANIMATION
        */
    }

    public void zoomBy(int pixels) {
        // stop render to avoid smearing
        stopRender();

        cumulative_zoom_factor += zoomFactorFromScale(scaleFromPixels(pixels));
        Bitmap zoomed_bm = getZoomedImage();
        scrollTo(0, 0);

        setImageBitmap(display_bm = zoomed_bm);
    }

    private Bitmap getZoomedImage() {
        /* TODO: make this more efficient */

        double new_left, new_right, new_top, new_bottom;
        Canvas offset_canvas, zoom_canvas;
        Bitmap offset_bm, zoomed_bm;
        Rect blit_to, blit_from;

        // use render bitmap to do the zoom - this allows us to chain calls to the zoom routine
        // without the cumulative_zoom_factor going crazy or losing definition

        /// do offset
        offset_bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        offset_canvas = new Canvas(offset_bm);
        offset_canvas.drawColor(palette_settings.mostFrequentColor());
        offset_canvas.drawBitmap(render_bm, -rendered_offset_x, -rendered_offset_y, null);

        // do zoom
        new_left = cumulative_zoom_factor * getWidth();
        new_right = getWidth() - new_left;
        new_top = cumulative_zoom_factor * getHeight();
        new_bottom = getHeight() - new_top;
        zoomed_bm = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        zoom_canvas = new Canvas(zoomed_bm);

        blit_to = new Rect(0, 0, getWidth(), getHeight());
        blit_from = new Rect((int)new_left, (int) new_top, (int) new_right, (int) new_bottom);

        zoom_canvas.drawColor(palette_settings.mostFrequentColor());
        zoom_canvas.drawBitmap(offset_bm, blit_from, blit_to, null);

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
    /* end of canvas transformation */
}


