package jetsetilly.mandelbrot.RenderCanvas.ImageView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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
import jetsetilly.mandelbrot.Settings.PaletteSettings;
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

    // canvas on which the fractal is drawn -- all transforms (scrolling, scaling) affect
    // this view only
    private ImageView display_canvas;

    // that ImageView that sits behind RenderCanvas_ImageView in the layout. we colour this image view
    // so that zooming the canvas doesn't expose the nothingness behind the canvas.
    protected Background background;

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

    // hack solution to the problem of pinch zooming after a image move (which includes animated
    // zoom). i think that the problem has something to do with pivot points but i couldn't
    // figure it out properly. it's such a fringe case however that this hack seems reasonable.
    // plus, once we have properly stitched bitmaps we can remove the artificial ON_UP_DELAY
    // in GestureOverlay
    private boolean scrolled_since_last_normalise;

    // completed render is true if last render was finished to completion. set to true
    // if render was interrupted prematurely (call to cancelDraw())
    private boolean completed_render;

    // controls the transition between bitmaps when using this class's setDisplay()
    // with the transition flag set
    public enum TransitionType {NONE, CROSS_FADE}
    public enum TransitionSpeed {VFAST, FAST, NORMAL, SLOW, VSLOW}

    private final TransitionType def_transition_type = TransitionType.CROSS_FADE;
    private final TransitionSpeed def_transition_speed = TransitionSpeed.NORMAL;
    private TransitionType transition_type = def_transition_type;
    private TransitionSpeed transition_speed = def_transition_speed;

    // runnable that handles cancelling of transition anim
    // if null then no animation is running. otherwise, animation IS running
    private Runnable set_display_anim_cancel = null;

    // latch preventing a second call to showBitmap() starting before a previous one has completed
    private Semaphore set_display_latch = new Semaphore(1);


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

        post(new Runnable() {
            @Override
            public void run() {
                // create the views used for rendering
                display_canvas = new ImageView(main_activity);
                background = new Background(main_activity);
                foreground = new ImageView(main_activity);

                background.setMinimumWidth(getWidth());
                background.setMinimumHeight(getHeight());

                // add the views in order - from back to front
                addView(background);
                addView(display_canvas);
                addView(foreground);

                // set scale type of fractal canvas to reckon from the centre of the view
                display_canvas.setScaleType(ImageView.ScaleType.CENTER);

                // set to hardware acceleration if available
                // TODO: proper hardware acceleration using SurfaceView
                display_canvas.setLayerType(LAYER_TYPE_HARDWARE, null);
                foreground.setLayerType(LAYER_TYPE_HARDWARE, null);
                background.setLayerType(LAYER_TYPE_HARDWARE, null);

                // create foreground bitmap
                foreground_bm = Bitmap.createBitmap(canvas_width, canvas_height, Bitmap.Config.ARGB_8888);
                foreground.setImageBitmap(foreground_bm);

                // create display canvas
                display_bm = Bitmap.createBitmap(canvas_width, canvas_height, Bitmap.Config.ARGB_8888);
                //display_bm.eraseColor(Color.TRANSPARENT);
                display_canvas.setImageBitmap(display_bm);

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
                background.invalidate();
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
        background.resetBackground();
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
            LogTools.printDebug(DBG_TAG, "starting new MandelbrotCanvas draw session before finishing another");
        }

        this_canvas_id = canvas_id;

        if (MandelbrotSettings.getInstance().render_mode == Mandelbrot.RenderMode.HARDWARE ||
                MandelbrotSettings.getInstance().render_mode == Mandelbrot.RenderMode.SOFTWARE_SIMPLEST) {
            buffer = new BufferSimple(this);
        } else {
            buffer = new BufferTimer(this);
        }

        buffer.startDraw(display_bm);
        completed_render = false;
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
                        background.setBackground();
                        completed_render = !cancelled;
                        this_canvas_id = NO_CANVAS_ID;
                        buffer_latch.release();
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
        return completed_render;
    }
    /*** END OF MandelbrotCanvas implementation ***/


    private void normaliseCanvas(){
        display_canvas.setScaleX(1f);
        display_canvas.setScaleY(1f);
        display_canvas.setX(0);
        display_canvas.setY(0);

        invalidate();
        scrolled_since_last_normalise = false;
    }

    public void setNextTransition(TransitionType type) {
        setNextTransition(type, def_transition_speed);
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
                        // block gestures -- will unblock at end of fixateVisibleImage() routine
                        // note that fixateVisibleImage() routine may return significantly before
                        // the routine finishes due to animated transitions
                        //
                        // also note that gestures may have already been blocked
                        // - eg. in the autoZoom() method
                        gestures.block();

                        // use whatever image is currently visible as the basis for the new render
                        // we do this with a smooth_transition if the image has been zoomed
                        // see comments in fixateVisibleImage() for explanation
                        fixateVisibleImage(fractal_scale != 0);
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
        stopRender(true);
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
        stopRender(false);

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
        gestures.block();

        // stop render to avoid smearing
        stopRender();

        // correct offset values
        if (!zoom_out) {
            offset_x -= (canvas_width / 2);
            offset_y -= (canvas_height / 2);
        }

        // transform offsets by current scroll/image_scale state
        float old_image_scale = (float) Transforms.imageScaleFromFractalScale(fractal_scale);
        offset_x -= getX();
        offset_y -= getY();
        offset_x /= old_image_scale;
        offset_y /= old_image_scale;

        // get new image_scale value - old_image_scale will be 1 if this is the first scale in the sequence
        float image_scale;

        if (zoom_out) {
            // no user setting to control how much to zoom out
            image_scale = old_image_scale * 0.5f;
        } else {
            image_scale = old_image_scale * gesture_settings.double_tap_scale;
        }

        // set zoom_factor and offsets ready for the new render
        fractal_scale = Transforms.fractalScaleFromImageScale(image_scale);
        rendered_offset_x = offset_x;
        rendered_offset_y = offset_y;

        // do animation
        ViewPropertyAnimator anim = this.display_canvas.animate();
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
                // gestures.block() was called at the head of the autoZoom() method
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

        completed_render = false;

        if (scrolled_since_last_normalise) {
            fixateVisibleImage(false);
        }

        // stop render to avoid smearing
        stopRender();

        // calculate fractal_scale
        fractal_scale += amount / Math.hypot(canvas_width, canvas_height);

        LogTools.printDebug(DBG_TAG, "fractal scale: "+fractal_scale);

        // limit fractal_scale between max in/out ranges
        fractal_scale = Math.max(gesture_settings.max_pinch_zoom_out,
                Math.min(gesture_settings.max_pinch_zoom_in, fractal_scale));

        float image_scale = (float) Transforms.imageScaleFromFractalScale(fractal_scale);

        this.display_canvas.setScaleX(image_scale);
        this.display_canvas.setScaleY(image_scale);
    }

    public void endManualZoom(boolean force) {
        // don't rescale image if we've zoomed in. this allows the zoomed image to be scrolled
        // and without losing any of the image after the image has been rescaled
        if (fractal_scale >= 0) {
            return;
        }

        completed_render = false;
        fixateVisibleImage(false);
    }
    /*** END OF GestureHandler implementation ***/

    private void fixateVisibleImage(boolean smooth_transition) {
        // smooth_transition fixates the image but does it twice, once with a bilinear filter
        // applied to the image, the second without. showBitmap() is called the second time with
        // the transition flag set to true.
        // this rigmarole is necessary after the image has been scaled. scaling the canvas is done
        // with filtering applied and the first call to getVisibleImage()/showBitmap()
        // recreates a normalised image similar to the final zoomed image. however, we don't want a
        // bilinear filtered image, we want a pixelated image. the second call to
        // getVisibleImage()/showBitmap() creates this image and causes a transition between
        // the two images (smooth to pixelated)
        // if we didn't create the pixelated image, then multiple zooms without a re-rendering of the
        // image will result in a blurry mess. the pixelated image looks better.
        int speed = 0;

        if (smooth_transition) {
            int from_pixels[] = new int[canvas_width * canvas_height];
            int to_pixels[] = new int[canvas_width * canvas_height];
            getVisibleImage(true).getPixels(from_pixels, 0, canvas_width, 0, 0, canvas_width, canvas_height);
            getVisibleImage(false).getPixels(to_pixels, 0, canvas_width, 0, 0, canvas_width, canvas_height);
            transformMandelbrot();

            setNextTransition(TransitionType.NONE);
            setDisplay(from_pixels);
            setNextTransition(TransitionType.CROSS_FADE, TransitionSpeed.VFAST);
            speed = setDisplay(to_pixels);
        } else {
            int pixels[] = new int[canvas_width * canvas_height];
            getVisibleImage(false).getPixels(pixels, 0, canvas_width, 0, 0, canvas_width, canvas_height);
            transformMandelbrot();
            setNextTransition(TransitionType.NONE);
            speed = setDisplay(pixels);
        }

        /*
        if (speed > 0) {
            final int delay_speed = speed;
            SimpleRunOnUI.run(main_activity, new Runnable() {
                @Override
                public void run() {
                    // clumsily wait for transition anim to finish
                    Handler h = new Handler();
                    h.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            gestures.unblock();
                        }
                    }, delay_speed);
                }
            });
        } else {
            SimpleRunOnUI.run(main_activity, new Runnable() {
                @Override
                public void run() {
                    gestures.unblock();
                }
            });
        }
        */

        SimpleRunOnUI.run(main_activity,
                new Runnable() {
                    @Override
                    public void run() {
                        gestures.unblock();
                    }
                }
        );
    }

    public Bitmap getVisibleImage(final boolean bilinear_filter) {
        // getVisibleImage() returns just the portion of the bitmap that is visible. used so
        // we can reset the scrolling and scaling of the RenderCanvas_ImageView ImageView

        int new_left, new_right, new_top, new_bottom;
        Bitmap offset_bm, scaled_bm;
        Canvas offset_canvas, scale_canvas;
        final Rect blit_to, blit_from;

        // do offset
        offset_bm = Bitmap.createBitmap(canvas_width, canvas_height, Bitmap.Config.ARGB_8888);
        offset_canvas = new Canvas(offset_bm);
        offset_canvas.drawBitmap(display_bm, -rendered_offset_x, -rendered_offset_y, null);

        // do zoom

        // use background image as the base for our scaled image
        scaled_bm = background.cloneBackground();

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
            set_display_latch.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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

                SimpleRunOnUI.run(main_activity, new Runnable() {
                    @Override
                    public void run() {
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

                // set up animation based on type
                final ViewPropertyAnimator transition_anim = foreground.animate();

                // prepare end runnable for animation
                final Runnable transition_end_runnable = new Runnable() {
                    @Override
                    public void run() {
                        set_display_anim_cancel = null;
                        foreground.setVisibility(INVISIBLE);
                        set_display_latch.release();
                    }
                };

                // prepare set_display_anim_cancel - ran if we need to stop animation prematurely
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
                        set_display_latch.release();
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
            case VFAST:
                speed = getResources().getInteger(R.integer.transition_duration_vfast);
                break;
            case FAST:
                speed = getResources().getInteger(R.integer.transition_duration_fast);
                break;
            case SLOW:
                speed = getResources().getInteger(R.integer.transition_duration_slow);
                break;
            case VSLOW:
                speed = getResources().getInteger(R.integer.transition_duration_vslow);
                break;
            default:
            case NORMAL:
                speed = getResources().getInteger(R.integer.transition_duration_slow);
                break;
        }
        return speed;
    }
}

