package jetsetilly.mandelbrot.RenderCanvas;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
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
import jetsetilly.mandelbrot.Settings.PaletteSettings;
import jetsetilly.mandelbrot.Settings.GestureSettings;

public class RenderCanvas extends ImageView implements MandelbrotCanvas
{
    private final String DBG_TAG = "render canvas";

    private MainActivity main_activity;
    private Mandelbrot mandelbrot;

    // that ImageView that sits behind RenderCanvas in the layout. we colour this image view
    // so that zooming the canvas doesn't expose the nothingness behind the canvas.
    private ImageView static_background;

    // special widget used to listen for gestures -- better than listening for gestures
    // on the RenderCanvas because we want to scale the RenderCanvas and scaling screws up
    // distance measurements
    private GestureOverlay gestures;

    // the display_bm is a pointer to whatever bitmap is currently displayed
    // whenever setImageBitmap() is called we should set display_bm to equal
    // whatever the Bitmap is being sent
    // the idiosyncratic ways to do this is setImageBitmap(display_bm = bm)
    // I considered overloading the setImageBitmap() method but that's too clumsy
    // a solution IMO
    private Bitmap display_bm;

    // render_bm is sometimes equal to display_bm sometimes not.
    // 1. on startRender() the current  display_bm is used to copy into the new render_bm by the
    // correct offset (resetting the scroll  of the ImageView afterwards)
    // 2. when pinch zooming the zoom amount is applied to the render_bm and not the display_bm. this
    // prevents exponential zooming of the image
    private Bitmap render_bm;
    private Buffer buffer;

    private final PaletteSettings palette_settings = PaletteSettings.getInstance();
    private final GestureSettings gesture_settings = GestureSettings.getInstance();

    // the amount of deviation (offset) from the current render_bm
    // used when chaining scroll and zoom events
    // reset when render is restarted
    // use getScrollX() and getScrollY() to retrieve current scroll values
    private int rendered_offset_x;
    private int rendered_offset_y;

    // the amount by which the mandelbrot needs to scale in order to match the display
    // for our purposes, this isn't the same as the scale amount. doubling in size would mean
    // a scale of 2. this equates to: zoom_rate = (scale - 1) / (2 * scale)
    // in this class we use scale exclusively to mean this and is only used in zoom animations
    // where scale is used in this way. the Gestures class uses the word scale more liberally.
    private double zoom_rate;


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
    }

    public void initPostLayout() {
        this.static_background = (ImageView) main_activity.findViewById(R.id.static_background);
        this.gestures = (GestureOverlay) main_activity.findViewById(R.id.gesture_overlay);
        this.gestures.setup(main_activity, this);
        resetCanvas();
    }
    /* end of initialisation */

    @Override
    public void setBackgroundColor(int color) {
        super.setBackgroundColor(color);

        // color static_background at the same time -- static_background only comes into play
        // when zooming but there's no performance loss so we set it here for convenience and neatness
        static_background.setBackgroundColor(color);
    }

    private void clearImage() {
        Bitmap clear_bm = Bitmap.createBitmap(getCanvasWidth(), getCanvasHeight(), Bitmap.Config.ARGB_8888);
        clear_bm.eraseColor(palette_settings.mostFrequentColor());
        setImageBitmap(display_bm = clear_bm);
    }

    public void resetCanvas() {
        // kill any existing image
        clearImage();

        // set base color
        setBackgroundColor(palette_settings.mostFrequentColor());

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
        setBackgroundColor(palette_settings.mostFrequentColor());
    }

    public void cancelDraw() {
        setBackgroundColor(palette_settings.mostFrequentColor());
    }

    public int getCanvasWidth() {
        return getWidth();
    }

    public int getCanvasHeight() {
        return getHeight();
    }
    /* end of MandelbrotCanvas implementation */


    /* render control */
    public void startRender() {
        stopRender();

        render_bm = Bitmap.createBitmap(getCanvasWidth(), getCanvasHeight(), Bitmap.Config.ARGB_8888);
        Canvas render_canvas = new Canvas(render_bm);

        // fill colour to first colour in current colours
        render_canvas.drawColor(palette_settings.mostFrequentColor());

        if (display_bm != null) {
            render_canvas.drawBitmap(display_bm, -getScrollX(), -getScrollY(), null);
            scrollTo(0, 0);
        }

        // lose reference to old bitmap
        setImageBitmap(display_bm = render_bm);

        // reset colours count
        palette_settings.resetCount();

        // start render thread
        mandelbrot.startRender(rendered_offset_x, rendered_offset_y, zoom_rate);

        // reset transformation variables
        rendered_offset_x = 0;
        rendered_offset_y = 0;
        zoom_rate = 0;
    }

    public void stopRender() {
        mandelbrot.stopRender();
    }
    /* end of render control */


    /* canvas transformations */
    private void updateOffsets(int offset_x, int offset_y) {
        rendered_offset_x += offset_x;
        rendered_offset_y += offset_y;
    }

    @Override
    public void scrollBy(int x, int y) {
        stopRender(); // stop render to avoid smearing
        updateOffsets(x, y);
        super.scrollBy(x, y);
    }

    public void animatedZoom(int offset_x, int offset_y) {
        // animation can take a while -- we don't gestures to be honoured
        // while the animation is taking place. call block() here
        // and unblock() in the animation's endAction
        gestures.block();

        float scale = gesture_settings.double_tap_scale;

        // stop render to avoid smearing
        stopRender();

        // update offsets ready for the new render
        updateOffsets(offset_x, offset_y);

        // update zoom rate
        zoom_rate = zoomRateFromScale(scale);

        // generate final zoomed image
        final Bitmap zoomed_bm = getScaledImage();

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
                gestures.unblock();
            }
        });
        anim.start();
    }

    public void zoomBy(float amount) {
        if (amount == 0)
            return;

        // stop render to avoid smearing
        stopRender();

        // calculate zoom_rate
        zoom_rate += amount / Math.hypot(getCanvasWidth(), getCanvasHeight());

        // limit zoom_rate between max in/out ranges
        zoom_rate = Math.max(gesture_settings.max_pinch_zoom_out,
                Math.min(gesture_settings.max_pinch_zoom_in, zoom_rate));

        float scale = scaleFromZoomRate(zoom_rate);
        setScaleX(scale);
        setScaleY(scale);
    }

    public void zoomCorrection() {
        /* this is called by Gestures.onScaleEnd() and is used to correct chaining of pinch-scaling and scrolling
        events, particularly scrolling after scaling.

        it's very similar to the startRender() method but principally doesn't clobber the render_bm
        and calls mandelbrot.preRender() instead of mandelbrot.startRender()

        see mandelbrot.preRender() for more discussion. we can remove this function once we figure out
        how to alter the render_offset_* variables after a zoom event
         */
        stopRender();

        // lose reference to old bitmap
        setImageBitmap(display_bm = getScaledImage());

        // transform mandelbrot coordinates - alternative to mandelbrot.startRender()
        mandelbrot.preRender(rendered_offset_x, rendered_offset_y, zoom_rate);

        // reset transformation variables
        rendered_offset_x = 0;
        rendered_offset_y = 0;
        zoom_rate = 0;

        setScaleX(1f);
        setScaleY(1f);
        scrollTo(0, 0);
    }

    private Bitmap getScaledImage() {
        double new_left, new_right, new_top, new_bottom;
        Canvas offset_canvas, zoom_canvas;
        Bitmap offset_bm, zoomed_bm;
        Rect blit_to, blit_from;

        // do offset
        offset_bm = Bitmap.createBitmap(getCanvasWidth(), getCanvasHeight(), Bitmap.Config.ARGB_8888);
        offset_canvas = new Canvas(offset_bm);
        offset_canvas.drawColor(palette_settings.mostFrequentColor());

        // use display_bm as the source bitmap -- this allows us to chain zooming and scrolling
        // in any order. the image may lose definition after several cycles of this but
        // it shouldn't be too noticeable
        offset_canvas.drawBitmap(display_bm, -rendered_offset_x, -rendered_offset_y, null);

        // do zoom
        new_left = zoom_rate * getCanvasWidth();
        new_right = getCanvasWidth() - new_left;
        new_top = zoom_rate * getCanvasHeight();
        new_bottom = getCanvasHeight() - new_top;

        zoomed_bm = Bitmap.createBitmap(getCanvasWidth(), getCanvasHeight(), Bitmap.Config.ARGB_8888);
        zoom_canvas = new Canvas(zoomed_bm);

        blit_to = new Rect(0, 0, getCanvasWidth(), getCanvasHeight());
        blit_from = new Rect((int) new_left, (int) new_top, (int) new_right, (int) new_bottom);

        zoom_canvas.drawColor(palette_settings.mostFrequentColor());
        zoom_canvas.drawBitmap(offset_bm, blit_from, blit_to, null);

        return zoomed_bm;
    }
    /* end of canvas transformations */

    private float scaleFromZoomRate(double zoom_rate) {
        return (float) (1 / (1 - (2 * zoom_rate)));
    }

    private double zoomRateFromScale(float scale) {
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
            this.display_bm.compress(Bitmap.CompressFormat.JPEG, 100, output_stream);
        } catch (Exception e) {
            if (url != null) {
                cr.delete(url, null, null);
            }

            return false;
        }

        return true;
    }
}

