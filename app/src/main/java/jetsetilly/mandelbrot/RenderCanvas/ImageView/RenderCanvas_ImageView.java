package jetsetilly.mandelbrot.RenderCanvas.ImageView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.util.AttributeSet;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;

import java.util.concurrent.Semaphore;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.mandelbrot.R;
import jetsetilly.mandelbrot.RenderCanvas.Base.RenderCanvas_Base;
import jetsetilly.mandelbrot.RenderCanvas.Transforms;
import jetsetilly.mandelbrot.Settings.GestureSettings;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;
import jetsetilly.tools.LogTools;
import jetsetilly.tools.SimpleAsyncTask;
import jetsetilly.tools.SimpleRunOnUI;

public class RenderCanvas_ImageView extends RenderCanvas_Base {
    private final String DBG_TAG = "render canvas";

    MainActivity main_activity;

    // width/height values set in onSizeChanged() - rather than relying on getWidth()/getHeight()
    // which are only callable from the UIThread
    private int canvas_width;
    private int canvas_height;

    protected int background_color;

    // canvas on which the fractal is drawn -- all transforms (scrolling, scaling) affect
    // this view only
    private ImageView display_canvas;

    // that ImageView that sits in front of RenderCanvas_ImageView in the layout. used to disguise changes
    // to main RenderCanvas_ImageView and allows us to animate changes
    private ImageView foreground;

    // special widget used to listen for gestures -- better than listening for gestures
    // on the RenderCanvas_ImageView because we want to scale the RenderCanvas_ImageView and scaling screws up
    // distance measurements
    private final GestureSettings gesture_settings = GestureSettings.getInstance();

    // the display_bm is a pointer to whatever bitmap is currently displayed in display_canvas
    private Bitmap display_bm;
    // foreground_bm is whatever bitmap is currently displayed in foreground
    private Bitmap foreground_bm;

    // buffer implementation
    private Buffer buffer;
    private Semaphore buffer_latch = new Semaphore(1);

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

    // the amount by which the mandelbrot needs to scale in order to match the display (image_scale)
    private double fractal_scale;

    // the amount of scaling since the last rendered image
    private double cumulative_image_scale = 1.0f;

    // hack solution to the problem of pinch zooming after a image move (which includes animated
    // zoom). i think that the problem has something to do with pivot points but i couldn't
    // figure it out properly. it's such a fringe case however that this hack seems reasonable.
    // plus, once we have properly stitched bitmaps we can remove the artificial ON_UP_DELAY
    // in GestureOverlay
    private boolean scrolled_since_last_normalise;

    // completed render is true if last render was finished to completion. set to true
    // if render was interrupted prematurely (call to cancelDraw())
    private boolean complete_render;

    // controls the transition between bitmaps when using this class's setDisplay()
    // with the transition flag set
    @IntDef({TransitionType.NONE, TransitionType.CROSS_FADE})
    @interface TransitionType {
        int NONE = 0;
        int CROSS_FADE = 1;
    }

    @IntDef({TransitionSpeed.VFAST, TransitionSpeed.FAST, TransitionSpeed.NORMAL, TransitionSpeed.SLOW, TransitionSpeed.VSLOW})
    @interface TransitionSpeed {
        int VFAST = 0;
        int FAST = 1;
        int NORMAL = 2;
        int SLOW = 3;
        int VSLOW = 4;
    }

    private final @TransitionType  int def_transition_type = TransitionType.CROSS_FADE;
    private final @TransitionSpeed int def_transition_speed = TransitionSpeed.NORMAL;
    private @TransitionType int transition_type = def_transition_type;
    private @TransitionSpeed int transition_speed = def_transition_speed;

    // runnable that handles cancelling of transition anim
    // if null then no animation is running. otherwise, animation IS running
    private Runnable set_display_anim_cancel = null;

    /*** initialisation ***/
    public RenderCanvas_ImageView(Context context) {
        super(context);
    }

    public RenderCanvas_ImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RenderCanvas_ImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void initialise(final MainActivity main_activity) {
        this.main_activity = main_activity;

        // create the views used for rendering
        display_canvas = new ImageView(main_activity);
        foreground = new ImageView(main_activity);

        // add the views in order - from back to front
        addView(display_canvas);
        addView(foreground);

        post(new Runnable() {
            @Override
            public void run() {
                // set scale type of fractal canvas to reckon from the centre of the view
                display_canvas.setScaleType(ImageView.ScaleType.CENTER);
                Matrix m = display_canvas.getMatrix();

                display_canvas.setScaleType(ImageView.ScaleType.MATRIX);
                Matrix display_canvas_matrix = new Matrix();
                display_canvas.setImageMatrix(display_canvas_matrix);

                // set to hardware acceleration if available
                // TODO: proper hardware acceleration using SurfaceView
                display_canvas.setLayerType(LAYER_TYPE_HARDWARE, null);
                foreground.setLayerType(LAYER_TYPE_HARDWARE, null);

                // create display canvas
                display_bm = Bitmap.createBitmap(canvas_width, canvas_height, Bitmap.Config.ARGB_8888);
                //display_bm.eraseColor(Color.TRANSPARENT);
                display_canvas.setImageBitmap(display_bm);

                // create foreground bitmap
                foreground_bm = Bitmap.createBitmap(canvas_width, canvas_height, Bitmap.Config.ARGB_8888);
                foreground.setImageBitmap(foreground_bm);

                invalidate();

                // reset canvas will start the new render
                resetCanvas();
            }
        });
        super.initialise(main_activity);
    }
    /*** END OF initialisation ***/

    @Override // View
    // thread safe
    public void invalidate() {
        SimpleRunOnUI.run(main_activity, new Runnable() {
            @Override
            public void run() {
                // intentionally not calling super.invalidate()
                display_canvas.invalidate();
                foreground.invalidate();
            }
        });
    }

    @Override // View
    public void onSizeChanged(int w, int h, int old_w, int old_h) {
        super.onSizeChanged(w, h, old_w, old_h);
        canvas_width = w;
        canvas_height = h;
    }

    public void resetCanvas() {
        // new render cache
        stopRender();

        super.resetCanvas();
        setBackgroundColor(background_color);
        startRender();
    }


    /*** MandelbrotCanvas implementation ***/
    @WorkerThread
    public void startDraw(long canvas_id) {
        try {
            buffer_latch.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (this_canvas_id != canvas_id && this_canvas_id != NO_CANVAS_ID) {
            // this shouldn't happen because of the buffer latch
            LogTools.printWTF(DBG_TAG, "starting new MandelbrotCanvas draw session before finishing another");
        }

        this_canvas_id = canvas_id;

        complete_render = false;

        if (MandelbrotSettings.getInstance().render_mode == Mandelbrot.RenderMode.HARDWARE ||
                MandelbrotSettings.getInstance().render_mode == Mandelbrot.RenderMode.SOFTWARE_SIMPLEST) {
            buffer = new BufferSimple(this);
        } else {
            buffer = new BufferTimer(this);
        }

        buffer.startDraw(display_bm);
    }

    @WorkerThread
    public void plotIterations(long canvas_id, int iterations[], boolean complete_plot) {
        if (this_canvas_id != canvas_id || buffer == null) return;

        buffer.plotIterations(iterations);

        // if the set of iterations is complete
        // (ie. every iteration has resulted in a new pixel) then
        // store iterations for future calls to reRender()
        if (complete_plot) {
            cached_iterations = iterations;
        } else {
            cached_iterations = null;
        }
    }

    @WorkerThread
    public void plotIteration(long canvas_id, int cx, int cy, int iteration) {
        if (this_canvas_id != canvas_id || buffer == null) return;

        cached_iterations = null;
        buffer.plotIteration(cx, cy, iteration);
    }

    @UiThread
    public void update(long canvas_id) {
        if (this_canvas_id != canvas_id || buffer == null) return;
        buffer.update();
    }

    @UiThread
    public void endDraw(long canvas_id, final boolean cancelled) {
        if (this_canvas_id != canvas_id || buffer == null) {
            return;
        }

        new SimpleAsyncTask(new Runnable() {
                    @Override
                    public void run() {
                        buffer.endDraw(cancelled);
                        buffer = null;
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        setBackgroundColor(background_color);
                        complete_render = !cancelled;
                        this_canvas_id = NO_CANVAS_ID;
                        buffer_latch.release();

                        unblockGestures();
                    }
                }, true);
    }

    // any thread
    public int getCanvasWidth() {
        return canvas_width;
    }

    // any thread
    public int getCanvasHeight() {
        return canvas_height;
    }

    // any thread
    public boolean isCompleteRender() {
        return complete_render;
    }
    /*** END OF MandelbrotCanvas implementation ***/

    /*** wrappers for gestures block()/unblock() ***/
    // allows us to add some additional logic
    private void blockGestures() {
        gestures.block();
    }

    private void unblockGestures() {
        if (isCompleteRender()) {
            cumulative_image_scale = 1.0f;
        }

        LogTools.printDebug(DBG_TAG, "cumulative image scale: " + cumulative_image_scale);

        if (cumulative_image_scale < 27.0) {
            gestures.unblock();
        }
    }
    /*** END OF wrappers for gestures block/unblock() ***/

    private void normaliseCanvas(){
        display_canvas.setScaleX(1f);
        display_canvas.setScaleY(1f);
        display_canvas.setX(0);
        display_canvas.setY(0);

        invalidate();
        scrolled_since_last_normalise = false;
    }

    public void setNextTransition(@TransitionType int type) {
        setNextTransition(type, def_transition_speed);
    }

    public void setNextTransition(@TransitionType int type, @TransitionSpeed int speed) {
        transition_type = type;
        transition_speed = speed;
    }

    public void reRender() {
        if (cached_iterations == null) {
            startRender();
            return;
        }

        stopRender();

        final long canvas_id = System.currentTimeMillis();
        new SimpleAsyncTask(
                new Runnable() {
                    @Override
                    public void run() {
                        startDraw(canvas_id);
                        plotIterations(canvas_id, cached_iterations, true);
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        endDraw(canvas_id, false);
                    }
                }
        );
    }

    public void startRender() {
        stopRender();

        new SimpleAsyncTask(
                new Runnable() {
                    @Override
                    public void run() {
                        // use whatever image is currently visible as the basis for the new render
                        fixateVisibleImage();
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        // start render thread
                        mandelbrot.startRender();
                    }
                }
        );
    }

    public void stopRender() {
        stopRender(false);
    }

    public void stopRender(boolean stop_animations) {
        if (stop_animations && set_display_anim_cancel != null) {
            set_display_anim_cancel.run();
        }

        if (mandelbrot != null) {
            mandelbrot.stopRender();
        }
    }

    private void transformMandelbrot() {
        mandelbrot.transformMandelbrot(rendered_offset_x, rendered_offset_y, fractal_scale);
        fractal_scale = 0;
        rendered_offset_x = 0;
        rendered_offset_y = 0;
    }

    /*** GestureHandler implementation ***/
    @Override // View
    public void scroll(int x, int y) {
        stopRender();

        float image_scale = (float) Transforms.imageScaleFromFractalScale(fractal_scale);
        x /= image_scale;
        y /= image_scale;
        rendered_offset_x += x;
        rendered_offset_y += y;

        // offset entire image view rather than using the scrolling ability
        display_canvas.setX(display_canvas.getX() - (x * image_scale));
        display_canvas.setY(display_canvas.getY() - (y * image_scale));

        scrolled_since_last_normalise = true;
    }

    public void finishManualGesture() {
        startRender();
    }

    public void autoZoom(int offset_x, int offset_y, boolean zoom_out) {
        // animation can take a while -- we don't want gestures to be honoured
        // while the animation is taking place. call block() here
        // and unblock() in the animation's endAction
        // this also has the effect of delaying the call to startRender() until
        // after the animation has finished
        blockGestures();

        // stop render to avoid smearing
        stopRender();

        int half_canvas_width = canvas_width / 2;
        int half_canvas_height = canvas_height / 2;

        // correct offset values
        if (!zoom_out) {
            offset_x -= half_canvas_width;
            offset_y -= half_canvas_height;
        }

        // transform offsets by current scroll/image_scale state
        float old_image_scale = (float) Transforms.imageScaleFromFractalScale(fractal_scale);

        if (getX() == 0 && getY() == 0 && old_image_scale == 1.0f) {
            // restrict offset_x and offset_y so that the zoomed image doesn't show
            // the background image
            if (offset_x > half_canvas_width - (int) (half_canvas_width / gesture_settings.double_tap_scale)) {
                offset_x = half_canvas_width - (int) (half_canvas_width / gesture_settings.double_tap_scale);
            } else if (offset_x < - half_canvas_width + (int) (half_canvas_width / gesture_settings.double_tap_scale)) {
                offset_x = - half_canvas_width + (int) (half_canvas_width / gesture_settings.double_tap_scale);
            }

            if (offset_y > half_canvas_height - (int) (half_canvas_height / gesture_settings.double_tap_scale)) {
                offset_y = half_canvas_height - (int) (half_canvas_height / gesture_settings.double_tap_scale);
            } else if (offset_y < - half_canvas_height + (int) (half_canvas_height / gesture_settings.double_tap_scale)) {
                offset_y = - half_canvas_height + (int) (half_canvas_height / gesture_settings.double_tap_scale);
            }
        } else {
            // this code path shouldn't ever be used
            LogTools.printWTF(DBG_TAG, "auto-zoom after scroll/manual zoom (?)");
            offset_x -= getX();
            offset_y -= getY();
            offset_x /= old_image_scale;
            offset_y /= old_image_scale;
        }

        // get new image_scale value - old_image_scale will be 1 if this is the first scale in the sequence
        float image_scale;

        if (zoom_out) {
            // no user setting to control how much to zoom out
            image_scale = old_image_scale * 0.5f;
        } else {
            image_scale = old_image_scale * gesture_settings.double_tap_scale;
        }

        // update cumulative image scale
        cumulative_image_scale *= image_scale;

        // set zoom_factor and offsets ready for the new render
        fractal_scale = Transforms.fractalScaleFromImageScale(image_scale);
        rendered_offset_x = offset_x;
        rendered_offset_y = offset_y;

        // do animation
        final ViewPropertyAnimator anim = this.display_canvas.animate();
        anim.setDuration(getResources().getInteger(R.integer.animated_zoom_duration_fast));
        anim.x(-offset_x * image_scale);
        anim.y(-offset_y * image_scale);
        anim.scaleX(image_scale);
        anim.scaleY(image_scale);

        anim.withStartAction(new Runnable() {
            @Override
            public void run() {
                normaliseCanvas();
            }
        });

        anim.withEndAction(new Runnable() {
            @Override
            public void run() {
                // blockGestures() was called at the head of the autoZoom() method
                // the unblock() method will be called at conclusion of fixateVisibleImage()
                // which is called by the following call to startRender()
                startRender();
            }
        });

        anim.start();
        scrolled_since_last_normalise = true;
    }

    public void manualZoom(float amount) {
        // WARNING: This doesn't work correctly in certain combination of zoom/move chains
        // unless the canvas is reset (as it is in endManualZoom() and startRender() methods)
        if (amount == 0)
            return;

        if (scrolled_since_last_normalise) {
            fixateVisibleImage();
        }

        // stop render to avoid smearing
        stopRender();

        // calculate fractal_scale
        fractal_scale += amount / Math.hypot(canvas_width, canvas_height);

        // limit fractal_scale between max in/out ranges
        fractal_scale = Math.max(gesture_settings.max_pinch_zoom_out,
                Math.min(gesture_settings.max_pinch_zoom_in, fractal_scale));

        float image_scale = (float) Transforms.imageScaleFromFractalScale(fractal_scale);

        // update cumulative image scale
        cumulative_image_scale *= image_scale;

        this.display_canvas.setScaleX(image_scale);
        this.display_canvas.setScaleY(image_scale);
    }

    public void endManualZoom(boolean force) {
        // don't rescale image if we've zoomed in. this allows the zoomed image to be scrolled
        // and without losing any of the image after the image has been rescaled
        if (fractal_scale >= 0) {
            return;
        }

        fixateVisibleImage();
    }
    /*** END OF GestureHandler implementation ***/

    private void fixateVisibleImage() {
        blockGestures();

        int pixels[] = new int[canvas_width * canvas_height];
        getVisibleImage(false).getPixels(pixels, 0, canvas_width, 0, 0, canvas_width, canvas_height);
        transformMandelbrot();
        setNextTransition(TransitionType.NONE);
        setDisplay(pixels);

        unblockGestures();
    }

    public Bitmap getVisibleImage(boolean bilinear_filter) {
        int new_left, new_right, new_top, new_bottom;
        Bitmap offset_bm, scaled_bm;
        Canvas offset_canvas, scale_canvas;
        final Rect blit_to, blit_from;

        // do offset
        offset_bm = Bitmap.createBitmap(canvas_width, canvas_height, Bitmap.Config.ARGB_8888);
        offset_canvas = new Canvas(offset_bm);
        offset_canvas.drawBitmap(display_bm, -rendered_offset_x, -rendered_offset_y, null);

        // do zoom

        // set background colour, otherwise faded reveals in setDisplay() won't work
        scaled_bm = Bitmap.createBitmap(canvas_width, canvas_height, Bitmap.Config.ARGB_8888);
        scaled_bm.eraseColor(background_color);

        new_left = (int) (fractal_scale * canvas_width);
        new_right = canvas_width - new_left;
        new_top = (int) (fractal_scale * canvas_height);
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
    }

    protected int setDisplay(final int pixels[]) {
        try {
            if (transition_type == TransitionType.CROSS_FADE) {
                // get speed of animation (we'll actually set the speed later)
                final int speed = getTransitionSpeed();

                // prepare foreground. this is the image we transition from
                SimpleRunOnUI.run(main_activity, new Runnable() {
                    @Override
                    public void run() {
                        int pixels[] = new int[canvas_width * canvas_height];
                        display_bm.getPixels(pixels, 0, canvas_width, 0, 0, canvas_width, canvas_height);
                        foreground_bm.setPixels(pixels, 0, canvas_width, 0, 0, canvas_width, canvas_height);
                        foreground.setVisibility(VISIBLE);
                        foreground.setAlpha(1.0f);
                    }
                });

                // prepare final image. the image we transition to
                SimpleRunOnUI.run(main_activity, new Runnable() {
                    @Override
                    public void run() {
                        normaliseCanvas();
                        display_bm.setPixels(pixels, 0, canvas_width, 0, 0, canvas_width, canvas_height);
                    }
                });

                // do animation

                final ViewPropertyAnimator transition_anim = foreground.animate();

                final Runnable transition_end_runnable = new Runnable() {
                    @Override
                    public void run() {
                        set_display_anim_cancel = null;
                        foreground.setVisibility(INVISIBLE);
                    }
                };

                set_display_anim_cancel = new Runnable() {
                    @Override
                    public void run() {
                        transition_anim.cancel();
                        transition_end_runnable.run();
                    }
                };

                SimpleRunOnUI.run(main_activity, new Runnable() {
                    @Override
                    public void run() {
                        transition_anim.withEndAction(transition_end_runnable);
                        transition_anim.setDuration(speed);
                        transition_anim.alpha(0.0f);
                        transition_anim.start();
                    }
                });

                return speed;
            } else {
                SimpleRunOnUI.run(main_activity, new Runnable() {
                    @Override
                    public void run() {
                        normaliseCanvas();
                        display_bm.setPixels(pixels, 0, canvas_width, 0, 0, canvas_width, canvas_height);
                    }
                });

                return 0;
            }
        } finally {
            // reset transition type/speed - until next call to changeNextTransition()
            transition_type = def_transition_type;
            transition_speed = def_transition_speed;
        }
    }

    private int getTransitionSpeed() {
        // get speed of animation (we'll actually set the speed later)
        final int speed;
        switch (transition_speed) {
            case TransitionSpeed.VFAST:
                speed = getResources().getInteger(R.integer.transition_duration_vfast);
                break;
            case TransitionSpeed.FAST:
                speed = getResources().getInteger(R.integer.transition_duration_fast);
                break;
            case TransitionSpeed.SLOW:
                speed = getResources().getInteger(R.integer.transition_duration_slow);
                break;
            case TransitionSpeed.VSLOW:
                speed = getResources().getInteger(R.integer.transition_duration_vslow);
                break;
            default:
            case TransitionSpeed.NORMAL:
                speed = getResources().getInteger(R.integer.transition_duration_slow);
                break;
        }
        return speed;
    }
}

