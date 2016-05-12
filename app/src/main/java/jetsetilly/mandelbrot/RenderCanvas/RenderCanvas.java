package jetsetilly.mandelbrot.RenderCanvas;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.mandelbrot.Mandelbrot.MandelbrotCanvas;
import jetsetilly.mandelbrot.R;
import jetsetilly.mandelbrot.Settings.GestureSettings;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;

public class RenderCanvas extends ImageView implements MandelbrotCanvas
{
    private final String DBG_TAG = "render canvas";

    private MainActivity main_activity;
    private Mandelbrot mandelbrot;
    protected ColourCache colour_cache;

    // that ImageView that sits behind RenderCanvas in the layout. we colour this image view
    // so that zooming the canvas doesn't expose the nothingness behind the canvas.
    private ImageView static_background;

    // special widget used to listen for gestures -- better than listening for gestures
    // on the RenderCanvas because we want to scale the RenderCanvas and scaling screws up
    // distance measurements
    private GestureOverlay gestures;
    private final GestureSettings gesture_settings = GestureSettings.getInstance();

    // the display_bm is a pointer to whatever bitmap is currently displayed
    // whenever setImageBitmap() is called we should set display_bm to equal
    // whatever the Bitmap is being sent
    // the idiosyncratic ways to do this is setImageBitmap(display_bm = bm)
    // I considered overloading the setImageBitmap() method but that's too clumsy
    // a solution IMO
    private Bitmap display_bm;

    // buffer implementation
    private Buffer buffer;

    // the amount of deviation (offset) from the current display_bm
    // used when chaining scroll and zoom events
    // reset when render is restarted
    // use getX() and getY() to retrieve current scroll values
    private int rendered_offset_x;
    private int rendered_offset_y;

    // the amount by which the mandelbrot needs to scale in order to match the display
    // for our purposes, this isn't the same as the scale amount. doubling in size would mean
    // a scale of 2. this equates to: mandelbrot_zoom_factor = (scale - 1) / (2 * scale)
    // in this class we use scale exclusively to mean this and is only used in zoom animations
    // where scale is used in this way. the Gestures class uses the word scale more liberally.
    private double mandelbrot_zoom_factor;

    // hack solution to the problem of pinch zooming after a image move (which includes animated
    // zoom). i think that the problem has something to do with pivot points but i couldn't
    // figure it out properly. it's such a fringe case however that this hack seems reasonable.
    // plus, once we have properly stitched bitmaps we can remove the artificial ON_UP_DELAY
    // in GestureOverlay
    private boolean scrolled_since_last_normalise;

    // completed render is true if last render was finished to completion. set to true
    // if render was interrupted prematurely (call to cancelDraw())
    private boolean completed_render;


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
        setScaleType(ImageView.ScaleType.CENTER);
    }

    public void initPostLayout() {
        this.static_background = (ImageView) main_activity.findViewById(R.id.static_background);
        this.gestures = (GestureOverlay) main_activity.findViewById(R.id.gesture_overlay);
        this.gestures.setup(main_activity, this);
        resetCanvas();
    }
    /* end of initialisation */

    @Override // View
    public void setBackgroundColor(int color) {
        super.setBackgroundColor(color);

        // color static_background at the same time -- static_background only comes into play
        // when zooming. set it here for convenience and neatness
        static_background.setBackgroundColor(color);
    }

    private void clearImage() {
        Bitmap clear_bm = Bitmap.createBitmap(getCanvasWidth(), getCanvasHeight(), Bitmap.Config.ARGB_8888);
        clear_bm.eraseColor(colour_cache.mostFrequentColor());
        setImageBitmap(display_bm = clear_bm);
    }

    public void resetCanvas() {
        // new render cache
        stopRender();
        colour_cache = new ColourCache();

        // kill any existing image
        clearImage();

        // set base color
        setBackgroundColor(colour_cache.mostFrequentColor());

        mandelbrot = new Mandelbrot(main_activity, this, (TextView) main_activity.findViewById(R.id.info_pane));

        startRender();
    }

    public void checkActionBar(float x, float y, boolean show) {
        // returns false if coordinates are in action bar, otherwise true
        if (show) {
            if (MainActivity.action_bar.inActionBar(y)) {
                MainActivity.action_bar.setVisibility(false);
            }
        } else {
            MainActivity.action_bar.setVisibility(true);
        }
    }


    /* MandelbrotCanvas implementation */
    public void startDraw(Mandelbrot.RenderMode render_mode) {
        // UI thread

        /* flush buffer here whether we need to or not. this is a hacky solution to the problem of
         cancelling the Mandelbrot thread and restarting it before ASyncTask.onCancelled()
         has run. the scheduling of onCancelled() is unreliable and the new thread may have started
         in the meantime. calling buffer.flush() from onCancelled() may try to write to a non-existing
         buffer. worse, if it does run it will kill the buffer leaving the new task nothing to
         work with
         */
        if (buffer != null)
            buffer.flush(true);

        if (MandelbrotSettings.getInstance().render_mode == Mandelbrot.RenderMode.HARDWARE) {
            buffer = new BufferSimple(this);
        } else {
            buffer = new BufferTimer(this);
        }

        buffer.primeBuffer(display_bm);
        completed_render = false;
    }

    public void drawPoints(int iterations[]) {
        // Mandelbrot Thread
        buffer.scheduleDraw(iterations);
    }

    public void drawPoint(int cx, int cy, int iteration) {
        // Mandelbrot Thread
        buffer.scheduleDraw(cx, cy, iteration);
    }

    public void update() {
        // UI thread
        buffer.flush(false);
    }

    public void endDraw() {
        // UI thread
        if (buffer != null) {
            buffer.flush(true);
            buffer = null;
        }
        setBackgroundColor(colour_cache.mostFrequentColor());
        completed_render = true;
    }

    public void cancelDraw() {
        // UI thread
        completed_render = false;
    }

    public int getCanvasWidth() {
        return getWidth();
    }

    public int getCanvasHeight() {
        return getHeight();
    }

    public boolean isCompleteRender() {
        return completed_render;
    }
    /* end of MandelbrotCanvas implementation */


    /* render control */
    private void normaliseCanvas() {
        setScaleX(1f);
        setScaleY(1f);
        setX(0);
        setY(0);
        scrolled_since_last_normalise = false;
    }

    public void startRender() {
        stopRender();

        // use whatever image is currently visible as the basis for the new render
        fixateVisibleImage(); // method sets display_bm

        colour_cache.reset();

        // start render thread
        mandelbrot.startRender(rendered_offset_x, rendered_offset_y, mandelbrot_zoom_factor);

        // reset transformation variables
        rendered_offset_x = 0;
        rendered_offset_y = 0;
        mandelbrot_zoom_factor = 0;
    }

    public void stopRender() {
        if (mandelbrot != null)
            mandelbrot.stopRender();
    }
    /* end of render control */


    /* canvas transformations */
    @Override // View
    public void scrollBy(int x, int y) {
        // no need to stop rendering except that there is an effect where the dominant
        // background colour will change while the existing render is ongoing
        stopRender();

        float scale = scaleFromZoomFactor(mandelbrot_zoom_factor);
        x /= scale;
        y /= scale;
        rendered_offset_x += x;
        rendered_offset_y += y;

        // offset entire image view rather than using the scrolling ability
        setX(getX() - (x * scale));
        setY(getY() - (y * scale));

        scrolled_since_last_normalise = true;
    }

    public void animatedZoom(int offset_x, int offset_y) {
        // animation can take a while -- we don't gestures to be honoured
        // while the animation is taking place. call block() here
        // and unblock() in the animation's endAction
        // this also has the effect of delaying the call to startRender() until
        // after the animation has finished
        gestures.block();

        // stop render to avoid smearing
        stopRender();

        // transform offsets by current scroll/scale state
        float old_scale = scaleFromZoomFactor(mandelbrot_zoom_factor);
        offset_x -= getX();
        offset_y -= getY();
        offset_x /= old_scale;
        offset_y /= old_scale;

        // get new scale value - old_scale will be 1 if this is the first scale in the sequence
        float scale = old_scale * gesture_settings.double_tap_scale;

        // set zoom_factor and offsets ready for the new render
        mandelbrot_zoom_factor = zoomFactorFromScale(scale);
        rendered_offset_x = offset_x;
        rendered_offset_y = offset_y;

        // do animation
        ViewPropertyAnimator anim = animate();
        anim.withLayer();
        anim.setDuration(getResources().getInteger(R.integer.animated_zoom_duration_fast));
        anim.x(-offset_x * scale);
        anim.y(-offset_y * scale);
        anim.scaleX(scale);
        anim.scaleY(scale);

        anim.withStartAction(new Runnable() {
            @Override
            public void run() {
                normaliseCanvas();
            }
        });

        anim.withEndAction(new Runnable() {
            @Override
            public void run() {
                gestures.unblock(null);
            }
        });
        anim.start();

        scrolled_since_last_normalise = true;

    }

    public void pinchZoom(float amount) {
        // WARNING: This doesn't work correctly in certain combination of zoom/move chains
        // unless the canvas is reset (as it is in zoomCorrection() and startRender() methods)

        if (amount == 0)
            return;

        if (scrolled_since_last_normalise)
            zoomCorrection(true);

        // stop render to avoid smearing
        stopRender();

        // calculate mandelbrot_zoom_factor
        mandelbrot_zoom_factor += amount / Math.hypot(getCanvasWidth(), getCanvasHeight());

        // limit mandelbrot_zoom_factor between max in/out ranges
        mandelbrot_zoom_factor = Math.max(gesture_settings.max_pinch_zoom_out,
                Math.min(gesture_settings.max_pinch_zoom_in, mandelbrot_zoom_factor));

        float scale = scaleFromZoomFactor(mandelbrot_zoom_factor);
        setScaleX(scale);
        setScaleY(scale);
    }

    public void zoomCorrection(boolean force) {
        // force == true -> called by pinchZoom() when scrolled_since_last_normalise is true
        // force == false -> called by GestureOverlay().onScaleEnd()

        // don't rescale image if we've zoomed in. this allows the zoomed image to be scrolled
        // and without losing any of the image after the image has been rescaled
        if (!force && mandelbrot_zoom_factor >= 0) {
            return;
        }

        fixateVisibleImage();

        // we've reset the image transformation (normaliseCanvas() call in fixateVisibleImage())
        // so we need to reset the mandelbrot transformation
        mandelbrot.transformMandelbrot(rendered_offset_x, rendered_offset_y, mandelbrot_zoom_factor);
        mandelbrot_zoom_factor = 0;
        rendered_offset_x = 0;
        rendered_offset_y = 0;

        // and we also need to mark the render as incomplete in order to force a complete re-render
        completed_render = false;
    }

    private Bitmap fixateVisibleImage() {
        setImageBitmap(display_bm = getVisibleImage());
        normaliseCanvas();
        return display_bm;
    }

    // getVisibleImage() returns just the portion of the bitmap that is visible. used so
    // we can reset the scrolling and scaling of the RenderCanvas ImageView
    private Bitmap getVisibleImage() {
        double new_left, new_right, new_top, new_bottom;
        Canvas offset_canvas, scale_canvas;
        Bitmap offset_bm, scaled_bm;
        Rect blit_to, blit_from;

        // do offset
        offset_bm = Bitmap.createBitmap(getCanvasWidth(), getCanvasHeight(), Bitmap.Config.ARGB_8888);
        //offset_bm.eraseColor(colour_cache.mostFrequentColor());

        // use display_bm as the source bitmap -- this allows us to chain zooming and scrolling
        // in any order. the image may lose definition after several cycles of this but
        // it shouldn't be too noticeable
        offset_canvas = new Canvas(offset_bm);
        offset_canvas.drawBitmap(display_bm, -rendered_offset_x, -rendered_offset_y, null);

        // do zoom
        new_left = mandelbrot_zoom_factor * getCanvasWidth();
        new_right = getCanvasWidth() - new_left;
        new_top = mandelbrot_zoom_factor * getCanvasHeight();
        new_bottom = getCanvasHeight() - new_top;

        scaled_bm = Bitmap.createBitmap(getCanvasWidth(), getCanvasHeight(), Bitmap.Config.ARGB_8888);
        //scaled_bm.eraseColor(colour_cache.mostFrequentColor());

        blit_to = new Rect(0, 0, getCanvasWidth(), getCanvasHeight());
        blit_from = new Rect((int) new_left, (int) new_top, (int) new_right, (int) new_bottom);

        Paint scale_paint = new Paint();
        scale_paint.setAntiAlias(true);
        scale_paint.setDither(true);

        scale_canvas = new Canvas(scaled_bm);
        scale_canvas.drawBitmap(offset_bm, blit_from, blit_to, scale_paint);

        return scaled_bm;
    }
    /* end of canvas transformations */

    private float scaleFromZoomFactor(double zoom_factor) {
        return (float) (1 / (1 - (2 * zoom_factor)));
    }

    private double zoomFactorFromScale(float scale) {
        return (scale - 1) / (2 * scale);
    }

    public boolean saveImage() {
        long curr_time = System.currentTimeMillis();

        String title = String.format("%s_%s.jpeg", getContext().getString(R.string.app_name), new SimpleDateFormat("yyyymmdd_hhmmss", Locale.ENGLISH).format(curr_time));

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, title);
        values.put(MediaStore.Images.Media.DESCRIPTION, getContext().getString(R.string.app_name));
        values.put(MediaStore.Images.Media.DATE_ADDED, curr_time);
        values.put(MediaStore.Images.Media.DATE_TAKEN, curr_time);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        ContentResolver cr = getContext().getContentResolver();
        Uri url = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        // TODO: album in pictures folder

        try {
            url = cr.insert(url, values);
            assert url != null;
            OutputStream output_stream = cr.openOutputStream(url);
            getVisibleImage().compress(Bitmap.CompressFormat.JPEG, 100, output_stream);
        } catch (Exception e) {
            if (url != null) {
                cr.delete(url, null, null);
            }

            return false;
        }

        return true;
    }
}

