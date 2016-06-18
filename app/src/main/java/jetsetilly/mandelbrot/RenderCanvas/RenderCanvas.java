package jetsetilly.mandelbrot.RenderCanvas;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Trace;
import android.provider.MediaStore;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.view.ViewPropertyAnimator;
import android.view.ViewAnimationUtils;
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
import jetsetilly.mandelbrot.Tools;

public class RenderCanvas extends ImageView implements MandelbrotCanvas
{
    private final String DBG_TAG = "render canvas";

    private MainActivity main_activity;
    private Mandelbrot mandelbrot;

    // that ImageView that sits behind RenderCanvas in the layout. we colour this image view
    // so that zooming the canvas doesn't expose the nothingness behind the canvas.
    private ImageView static_background;

    // that ImageView that sits in front of RenderCanvas in the layout. used to disguise changes
    // to main RenderCanvas and allows us to animate changes
    private ImageView static_foreground;

    // colour which is used to color the background - the actual colour, not the index
    // into the current palette. consequently, in some instances a colour may be used that is not
    // in the current palette definition.
    protected int background_colour;

    // special widget used to listen for gestures -- better than listening for gestures
    // on the RenderCanvas because we want to scale the RenderCanvas and scaling screws up
    // distance measurements
    private GestureOverlay gestures;
    private final GestureSettings gesture_settings = GestureSettings.getInstance();

    // the display_bm is a pointer to whatever bitmap is currently displayed
    // setImageBitmap() is over-ridden and will assign appropriately
    private Bitmap display_bm;

    // buffer implementation
    private Buffer buffer;

    // the iterations array that was last sent to plotIterations()
    // we use this to quickly redraw a render with different colours
    // may prove useful in other scenarios (although we may need to make
    // the mechanism more flexible)
    private int[] cached_iterations;

    // canvas_id of most recent thread that has called MandelbrotCanvas.startDraw()
    private final long NO_CANVAS_ID = -1;
    private long this_canvas_id = NO_CANVAS_ID;

    // the amount of deviation (offset) from the current display_bm
    // used when chaining scroll and zoom events
    // reset when render is restarted
    // use getX() and getY() to retrieve current scroll values
    private int rendered_offset_x;
    private int rendered_offset_y;

    // the amount by which the mandelbrot needs to scale in order to match the display.
    // for our purposes, this isn't the same as the scale amount. doubling in size would mean
    // a scale of 2. this equates to: mandelbrot_zoom_factor = (scale - 1) / (2 * scale)
    //
    // in this class when we use the word "scale" we mean mandelbrot_zoom_factor.
    // the Gestures class uses the word scale more liberally.
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

    // controls the transition between bitmaps when using this class's setBitmap() with
    // the transition flag set
    public enum TransitionType {NONE, CROSS_FADE, CIRCLE}
    public enum TransitionSpeed {VFAST, FAST, NORMAL, SLOW, VSLOW}
    private final TransitionType def_transition_type = TransitionType.CROSS_FADE;
    private final TransitionSpeed def_transition_speed= TransitionSpeed.NORMAL;
    private TransitionType transition_type = def_transition_type;
    private TransitionSpeed transition_speed = def_transition_speed;

    private int canvas_width, canvas_height;

    // runnable that handles cancelling of transition anim
    // if null then no animation is running. otherwise, animation IS running
    private Runnable transition_anim_cancel = null;

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
        this.static_foreground = (ImageView) main_activity.findViewById(R.id.static_foreground);
        this.gestures = (GestureOverlay) main_activity.findViewById(R.id.gesture_overlay);
        this.gestures.setup(main_activity, this);
        resetCanvas();
    }
    /* end of initialisation */

    @Override // View
    public void onSizeChanged(int w, int h, int old_w, int old_h) {
        super.onSizeChanged(w, h, old_w, old_h);
        canvas_width = w;
        canvas_height = h;
    }

    @Override // View
    public void setImageBitmap(Bitmap bm) {
        setImageBitmap(bm, false);
    }

    public void setImageBitmap(Bitmap bm, boolean transition) {
        if (transition || transition_type == TransitionType.NONE) {
            // prepare foreground. this is the image we transition from
            static_foreground.setImageBitmap(display_bm);
            static_foreground.setVisibility(VISIBLE);
            static_foreground.setAlpha(1.0f);

            // prepare final image. the image we transition to
            super.setImageBitmap(display_bm = bm);

            // get speed of animation (we'll actually set the speed later)
            int speed;
            switch (transition_speed) {
                case VFAST:
                    speed = R.integer.transition_duration_vfast;
                    break;
                case FAST:
                    speed = R.integer.transition_duration_fast;
                    break;
                case SLOW:
                    speed = R.integer.transition_duration_slow;
                    break;
                case VSLOW:
                    speed = R.integer.transition_duration_vslow;
                    break;
                default:
                case NORMAL:
                    speed = R.integer.transition_duration_slow;
                    break;
            }
            speed = getResources().getInteger(speed);

            // same end runnable for all transition types
            final Runnable transition_end_runnable = new Runnable() {
                @Override
                public void run() {
                    static_foreground.setVisibility(INVISIBLE);
                    transition_anim_cancel = null;
                }
            };

            // set up animation based on type
            if (transition_type == TransitionType.CROSS_FADE) {
                final ViewPropertyAnimator transition_anim = static_foreground.animate();

                // prepare transition_anim_cancel. see comments in declaration for explanation
                transition_anim_cancel= new Runnable() {
                    @Override
                    public void run() {
                        transition_anim.cancel();
                        transition_end_runnable.run();
                    }
                };

                transition_anim.withEndAction(transition_end_runnable);
                transition_anim.setDuration(speed);
                transition_anim.alpha(0.0f);
                transition_anim.start();

            } else if (transition_type == TransitionType.CIRCLE) {
                final Animator transition_anim = ViewAnimationUtils.createCircularReveal(static_foreground,
                        getCanvasWidth()/2, getCanvasHeight()/2, getCanvasWidth(), 0);

                // prepare transition_anim_cancel. see comments in declaration for explanation
                transition_anim_cancel = new Runnable() {
                    @Override
                    public void run() {
                        transition_anim.cancel();
                        transition_end_runnable.run();
                    }
                };

                transition_anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        transition_end_runnable.run();
                    }
                });

                transition_anim.setDuration(speed);
                transition_anim.start();
            }

            // reset transition type/speed - until next call to changeNextTransition()
            transition_type = def_transition_type;
            transition_speed = def_transition_speed;
        }
        else {
            super.setImageBitmap(display_bm = bm);
        }
    }

    @Override // View
    public void setBackgroundColor(int color) {
        super.setBackgroundColor(color);

        // color static_background at the same time -- static_background only comes into play
        // when zooming. set it here for convenience and neatness
        static_background.setBackgroundColor(color);
    }

    private void clearImage() {
        Bitmap clear_bm = Bitmap.createBitmap(getCanvasWidth(), getCanvasHeight(), Bitmap.Config.ARGB_8888);
        clear_bm.eraseColor(background_colour);
        setNextTransition(TransitionType.CROSS_FADE, TransitionSpeed.FAST);
        setImageBitmap(clear_bm, true);
    }

    public void resetCanvas() {
        // new render cache
        stopRender();

        // kill any existing image
        clearImage();

        // set base color
        setBackgroundColor(background_colour);

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
    // any thread
    public void startDraw(long canvas_id) {
        if (this_canvas_id != canvas_id && this_canvas_id != NO_CANVAS_ID) {
            Tools.printDebug(DBG_TAG, "starting new MandelbrotCanvas draw session before finishing another");
        }
        this_canvas_id = canvas_id;

        if (MandelbrotSettings.getInstance().render_mode == Mandelbrot.RenderMode.HARDWARE) {
            buffer = new BufferSimple(this);
        } else {
            buffer = new BufferTimer(this);
        }

        buffer.primeBuffer(display_bm);
        completed_render = false;
    }

    // any thread
    public void plotIterations(long canvas_id, int iterations[], boolean complete_plot) {
        if (this_canvas_id != canvas_id || buffer == null) return;

        // plot iterations and if the set of iterations is complete
        // (ie. every iteration has resulted in a new pixel) then
        // store iterations for future calls to reRender()
        buffer.plotIterations(iterations);

        if (complete_plot) {
            cached_iterations = iterations;
        } else {
            cached_iterations = null;
        }
    }

    // any thread
    public void plotIteration(long canvas_id, int cx, int cy, int iteration) {
        if (this_canvas_id != canvas_id || buffer == null) return;

        cached_iterations = null;
        buffer.plotIteration(cx, cy, iteration);
    }

    @UiThread
    public void update(long canvas_id) {
        if (this_canvas_id != canvas_id || buffer == null) return;

        buffer.flush();
    }

    @UiThread
    public void endDraw(long canvas_id) {
        if (this_canvas_id != canvas_id || buffer == null) return;

        buffer.endBuffer(false);
        buffer = null;
        setBackgroundColor(background_colour);
        completed_render = true;
        this_canvas_id = NO_CANVAS_ID;
    }

    @UiThread
    public void cancelDraw(long canvas_id) {
        if (this_canvas_id != canvas_id || buffer == null) return;

        buffer.endBuffer(true);
        buffer = null;
        setBackgroundColor(background_colour);
        completed_render = false;
        this_canvas_id = NO_CANVAS_ID;
    }

    public int getCanvasWidth() {
        return canvas_width;
    }

    public int getCanvasHeight() {
        return canvas_height;
    }

    public boolean isCompleteRender() {
        return completed_render;
    }
    /* end of MandelbrotCanvas implementation */


    /* render control */
    private void normaliseCanvas(){
        setScaleX(1f);
        setScaleY(1f);
        setX(0);
        setY(0);

        scrolled_since_last_normalise = false;
    }

    public void setNextTransition(TransitionType type, TransitionSpeed speed) {
        transition_type = type;
        transition_speed = speed;
    }

    public void reRender() {
        if (cached_iterations == null) {
            startRender();
            return;
        }

        stopRender();

        long canvas_id = System.currentTimeMillis();
        startDraw(canvas_id);
        plotIterations(canvas_id, cached_iterations, true);
        endDraw(canvas_id);
    }

    public void startRender() {
        Trace.beginSection("starting render");
        try {
            stopRender();

            // use whatever image is currently visible as the basis for the new render
            // we do this with a smooth_transition if the image has been zoomed
            // see comments in fixateVisibleImage() for explanation
            if (mandelbrot_zoom_factor == 0) {
                fixateVisibleImage(false);
            } else {
                setNextTransition(TransitionType.CROSS_FADE, TransitionSpeed.FAST);
                fixateVisibleImage(true);
            }

            // start render thread
            mandelbrot.startRender(rendered_offset_x, rendered_offset_y, mandelbrot_zoom_factor);

            // reset transformation variables
            rendered_offset_x = 0;
            rendered_offset_y = 0;
            mandelbrot_zoom_factor = 0;
        } finally {
            Trace.endSection();
        }
    }

    public void stopRender() {
        if (mandelbrot != null)
            mandelbrot.stopRender();

        // cancel transition animation and run end conditions
        if (transition_anim_cancel != null) {
            transition_anim_cancel.run();
        }
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
        // animation can take a while -- we don't want gestures to be honoured
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
                // the UI thread was blocked just prior to animatedZoom() being called
                // unblocking it here will allow the gesture sequence to complete once
                // the animation has completed
                gestures.unblock(null);
            }
        });
        anim.start();
        scrolled_since_last_normalise = true;

        /*
        setX(-offset_x * scale);
        setY(-offset_y * scale);
        setScaleX(scale);
        setScaleY(scale);
        scrolled_since_last_normalise = true;
        startRender();
        */
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

        fixateVisibleImage(false);

        // we've reset the image transformation (normaliseCanvas() call in fixateVisibleImage())
        // so we need to reset the mandelbrot transformation
        mandelbrot.transformMandelbrot(rendered_offset_x, rendered_offset_y, mandelbrot_zoom_factor);
        mandelbrot_zoom_factor = 0;
        rendered_offset_x = 0;
        rendered_offset_y = 0;

        // and we also need to mark the render as incomplete in order to force a complete re-render
        completed_render = false;
    }

    private void fixateVisibleImage(boolean smooth_transition) {
        // smooth_transition fixates the image but does it twice, once with a bilinear filter
        // applied to the image, the second without. setImageBitmap() is called the second time with
        // the transition flag set to true.
        // this rigmarole is necessary after the image has been scaled. scaling the canvas is done
        // with bilinear filtering applied and the first call to getVisibleImage()/setImageBitmap()
        // recreates a normalised image similar to the final zoomed image. however, we don't want a
        // bilinear filtered image, we want a pixelated image. the second call to
        // getVisibleImage()/setImageBitmap() creates this image and causes a transition between
        // the two images (smooth to pixelated)
        // if we didn't create the pixelated image, then multiple zooms without a re-rendering of the
        // image will result in a blurry mess. the pixelated image looks better.
        Trace.beginSection("fixateVisibleImage()");
        try {
            if (smooth_transition) {
                Bitmap from_bm = getVisibleImage(true);
                Bitmap to_bm = getVisibleImage(false);
                normaliseCanvas();
                setImageBitmap(from_bm);
                setImageBitmap(to_bm, true);

            } else {
                Bitmap bm = getVisibleImage(false);
                setImageBitmap(bm);
                normaliseCanvas();
            }
        } finally {
            Trace.endSection();
        }
    }

    // getVisibleImage() returns just the portion of the bitmap that is visible. used so
    // we can reset the scrolling and scaling of the RenderCanvas ImageView
    Bitmap getVisibleImage (boolean bilinear_filter) {
        int new_left, new_right, new_top, new_bottom;
        Bitmap offset_bm, scaled_bm;
        Canvas offset_canvas, scale_canvas;
        final Rect blit_to, blit_from;

        Trace.beginSection("getVisibleImage(" + bilinear_filter + ")");
        try {
            // use display_bm as the source bitmap -- this allows us to chain zooming and scrolling
            // in any order. the image may lose definition after several cycles of this but
            // it shouldn't be too noticeable

            // do offset
            offset_bm = Bitmap.createBitmap(canvas_width, canvas_height, Bitmap.Config.ARGB_8888);
            offset_canvas = new Canvas(offset_bm);
            offset_canvas.drawBitmap(display_bm, -rendered_offset_x, -rendered_offset_y, null);

            // do zoom
            scaled_bm = Bitmap.createBitmap(canvas_width, canvas_height, Bitmap.Config.ARGB_8888);

            // in case the source image has been offset (we won't check for it, it's not worth it) we
            // fill the final bitmap with a colour wash of mostFrequentColor(). if we don't then animating
            // reveals with setImageBitmap() may not work as expected
            scaled_bm.eraseColor(background_colour);

            new_left = (int) (mandelbrot_zoom_factor * canvas_width);
            new_right = canvas_width - new_left;
            new_top = (int) (mandelbrot_zoom_factor * canvas_height);
            new_bottom = canvas_height - new_top;
            blit_to = new Rect(0, 0, canvas_width, canvas_height);
            blit_from = new Rect(new_left, new_top, new_right, new_bottom);

            Paint scale_pnt = null;
            if (bilinear_filter) {
                scale_pnt = new Paint();
                scale_pnt.setFilterBitmap(true);
            }

            scale_canvas = new Canvas(scaled_bm);
            scale_canvas.drawBitmap(offset_bm, blit_from, blit_to, scale_pnt);

            return scaled_bm;
        } finally {
            Trace.endSection();
        }
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
            getVisibleImage(false).compress(Bitmap.CompressFormat.JPEG, 100, output_stream);
        } catch (Exception e) {
            if (url != null) {
                cr.delete(url, null, null);
            }

            return false;
        }

        return true;
    }
}

