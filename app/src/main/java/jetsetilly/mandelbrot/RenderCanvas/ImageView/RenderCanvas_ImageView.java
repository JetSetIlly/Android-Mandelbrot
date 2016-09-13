package jetsetilly.mandelbrot.RenderCanvas.ImageView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.util.AttributeSet;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.Arrays;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.mandelbrot.R;
import jetsetilly.mandelbrot.RenderCanvas.Base.RenderCanvas_Base;
import jetsetilly.mandelbrot.RenderCanvas.Transforms;
import jetsetilly.tools.LogTools;
import jetsetilly.tools.SimpleAsyncTask;
import jetsetilly.tools.SimpleLatch;

public class RenderCanvas_ImageView extends RenderCanvas_Base {
    private final String DBG_TAG = "render canvas";

    // width/height values set in onSizeChanged() - rather than relying on getWidth()/getHeight()
    // which are only callable from the UIThread
    private int canvas_width, half_canvas_width;
    private int canvas_height, half_canvas_height;

    // bitmap config to use depending on SystemSettings.deep_colour
    Bitmap.Config bitmap_config;

    // dominant colour to use as background colour - visible on scroll and zoom out events
    protected int background_colour;

    // layout to contain the display and foreground ImageViews, defined below
    // all transforms are performed on this layout
    private RelativeLayout canvas;

    // canvas on which the fractal is drawn -- all transforms (scrolling, scaling) affect
    // this view only
    private ImageView display;

    // that ImageView that sits in front of RenderCanvas_ImageView in the layout. used to disguise changes
    // to main RenderCanvas_ImageView and allows us to animate changes
    private ImageView foreground;

    // the display_bm is a pointer to whatever bitmap is currently displayed in display
    private Bitmap display_bm;

    // foreground_bm is whatever bitmap is currently displayed in foreground
    private Bitmap foreground_bm;

    // reference to the task that prepares the main render thread
    // used to cancel render_task before it has started properly
    private SimpleAsyncTask startup_render_task;

    // synchronise ending of fixateVisibleImage() - startRender() will pause until latch is free
    private SimpleLatch fixate_synchronise = new SimpleLatch();

    // prevents setDisplay() animation from running if latch has been acquired
    // and pauses startRender() until an active setDisplay() animation has ended
    // more primitive solution is to run setDisplay().animate.withEndAction() before
    // proceeding with startRender()
    private SimpleLatch set_display_anim_latch = new SimpleLatch();

    // buffer implementation
    private Buffer buffer;
    private SimpleLatch buffer_latch = new SimpleLatch();
    private final long NO_CANVAS_ID = -1;
    private long this_canvas_id = NO_CANVAS_ID;

    // the amount of scaling since the last rendered image
    private double cumulative_image_scale = 1.0f;

    // maximum value of cumulative_image_scale allowed before zoom is paused
    private static float MAX_IMAGE_SCALE = 27.0f;

    // controls the transition type between bitmaps for setDisplay()
    @IntDef({TransitionType.NONE, TransitionType.CROSS_FADE})
    @interface TransitionType {
        int NONE = 0;
        int CROSS_FADE = 1;
    }

    // controls the transition speed between bitmaps for setDisplay()
    // TransitionType.NONE implies immediate transition - speed is meaningless
    @IntDef({TransitionSpeed.FAST, TransitionSpeed.NORMAL, TransitionSpeed.SLOW})
    @interface TransitionSpeed {
        byte FAST = 1;
        byte NORMAL = 2;
        byte SLOW = 3;
    }

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

    public void initialise(final MainActivity context) {
        super.initialise(context);

        removeAllViews();

        if (settings.deep_colour) {
            LogTools.printDebug(DBG_TAG, "ARGB_8888");
            bitmap_config = Bitmap.Config.ARGB_8888;
        } else {
            LogTools.printDebug(DBG_TAG, "RGB_565");
            bitmap_config = Bitmap.Config.RGB_565;
        }

        canvas = new RelativeLayout(context);

        display = new ImageView(context);
        display.setScaleType(ImageView.ScaleType.CENTER);
        display.setLayerType(LAYER_TYPE_HARDWARE, null);

        foreground = new ImageView(context);
        foreground.setLayerType(LAYER_TYPE_HARDWARE, null);
        foreground.setVisibility(INVISIBLE);

        addView(canvas);
        canvas.addView(display);
        canvas.addView(foreground);

        post(new Runnable() {
            @Override
            public void run() {
                display_bm = Bitmap.createBitmap(canvas_width, canvas_height, bitmap_config);
                foreground_bm = Bitmap.createBitmap(canvas_width, canvas_height, bitmap_config);

                display.setImageBitmap(display_bm);
                foreground.setImageBitmap(foreground_bm);
                invalidate();
                resetCanvas();
            }
        });
    }
    /*** END OF initialisation ***/

    @Override // View
    public void onSizeChanged(int w, int h, int old_w, int old_h) {
        super.onSizeChanged(w, h, old_w, old_h);
        canvas_width = w;
        canvas_height = h;
        half_canvas_width = w / 2;
        half_canvas_height = h / 2;
    }

    @Override // View
    public void invalidate() {
        post(new Runnable() {
            @Override
            public void run() {
                display.invalidate();
                foreground.invalidate();
            }
        });
        // not calling super method
    }

    public void resetCanvas() {
        // new render cache
        stopRender();

        super.resetCanvas();
        setBackgroundColor(background_colour);
        startRender();
    }

    /*** MandelbrotCanvas implementation ***/
    @WorkerThread
    public void startDraw(long canvas_id) {
        buffer_latch.acquire();

        if (this_canvas_id != canvas_id && this_canvas_id != NO_CANVAS_ID) {
            // this shouldn't happen because of the buffer latch
            LogTools.printWTF(DBG_TAG, "starting new MandelbrotCanvas draw session before finishing another");
        }

        this_canvas_id = canvas_id;
        incomplete_render = true;

        if (settings.render_mode == Mandelbrot.RenderMode.HARDWARE) {
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
    }

    @WorkerThread
    public void plotIteration(long canvas_id, int cx, int cy, int iteration) {
        if (this_canvas_id != canvas_id || buffer == null) return;
        buffer.plotIteration(cx, cy, iteration);
    }

    @UiThread
    public void update(long canvas_id) {
        if (this_canvas_id != canvas_id || buffer == null) return;
        buffer.update();
    }

    @UiThread
    public void endDraw(long canvas_id, boolean cancelled) {
        if (this_canvas_id != canvas_id || buffer == null) {
            return;
        }

        buffer.endDraw(cancelled);
        setBackgroundColor(background_colour);
        incomplete_render = cancelled;
        this_canvas_id = NO_CANVAS_ID;

        if (!incomplete_render) {
            cumulative_image_scale = 1.0f;
            gestures.unpauseZoom();
        }

        renderThreadEnded();
        buffer = null;
        buffer_latch.release();
    }
    /*** END OF MandelbrotCanvas implementation ***/

    private void normaliseCanvas(){
        canvas.setScaleX(1f);
        canvas.setScaleY(1f);
        canvas.setX(0);
        canvas.setY(0);
        canvas.setPivotX(canvas_width/2);
        canvas.setPivotY(canvas_height/2);
    }

    public void startRender() {
        gestures.unpauseScroll();
        gestures.pauseScroll();

        stopRender();

        startup_render_task = new SimpleAsyncTask("RenderCanvas_ImageView.startRender",
                new Runnable() {
                    @Override
                    public void run() {
                        // wait for previous render to finish
                        buffer_latch.monitor();

                        // wait for any setDisplay() animation to finish before proceeding
                        // and prevent animation from running because we're waiting
                        set_display_anim_latch.monitor();

                        // fixate visible image to conclude and wait for everything to complete
                        fixate_synchronise.acquire();
                        fixateVisibleImage(false);
                        fixate_synchronise.monitor();

                        if (settings.render_mode != Mandelbrot.RenderMode.HARDWARE || cumulative_image_scale < MAX_IMAGE_SCALE) {
                            // unpause zoom gesture if we're below that maximum image scale or
                            // if we're using software rendering
                            gestures.unpauseZoom();
                        }
                        gestures.unpauseScroll();
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        startup_render_task = null;
                        startRenderThread();
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        startup_render_task = null;
                    }
                }
        );
    }

    public void stopRender() {
        if (startup_render_task != null) startup_render_task.cancel();
        stopRenderThread();
    }

    /*** GestureHandler implementation ***/
    public void finishManualGesture() {
        startRender();
    }

    @Override // View
    public void scroll(float x, float y) {
        stopRender();

        float image_scale = (float) Transforms.imageScaleFromFractalScale(mandelbrot_transform.scale);

        canvas.setX(canvas.getX() - (x / image_scale));
        canvas.setY(canvas.getY() - (y / image_scale));

        canvas.setPivotX(canvas.getPivotX() + (x / image_scale));
        canvas.setPivotY(canvas.getPivotY() + (y / image_scale));

        mandelbrot_transform.x += x / image_scale;
        mandelbrot_transform.y += y / image_scale;
    }

    public void autoZoom(float offset_x, float offset_y, boolean zoom_out) {
        float old_image_scale = (float) Transforms.imageScaleFromFractalScale(mandelbrot_transform.scale);

        // some combinations of scroll and zooming don't work
        if (!(canvas.getX() == 0 && canvas.getY() == 0 && old_image_scale == 1.0f)) {
            gestures.pauseZoom(true);
            return;
        }

        stopRender();

        // pause gestures - startRender() will unpause as appropriate
        gestures.pauseZoom(false);
        gestures.pauseScroll();

        // correct offset values
        offset_x -= half_canvas_width;
        offset_y -= half_canvas_height;

        // restrict offset_x and offset_y so that the zoomed image doesn't show
        // the background image
        if (offset_x > half_canvas_width - (int) (half_canvas_width / settings.double_tap_scale)) {
            offset_x = half_canvas_width - (int) (half_canvas_width / settings.double_tap_scale);
        } else if (offset_x < - half_canvas_width + (int) (half_canvas_width / settings.double_tap_scale)) {
            offset_x = - half_canvas_width + (int) (half_canvas_width / settings.double_tap_scale);
        }

        if (offset_y > half_canvas_height - (int) (half_canvas_height / settings.double_tap_scale)) {
            offset_y = half_canvas_height - (int) (half_canvas_height / settings.double_tap_scale);
        } else if (offset_y < - half_canvas_height + (int) (half_canvas_height / settings.double_tap_scale)) {
            offset_y = - half_canvas_height + (int) (half_canvas_height / settings.double_tap_scale);
        }

        // get new image_scale value - old_image_scale will be 1 if this is the first scale in the sequence
        float image_scale;

        if (zoom_out) {
            // no user setting to control how much to zoom out
            image_scale = old_image_scale * 0.5f;
        } else {
            image_scale = old_image_scale * settings.double_tap_scale;
        }

        // update cumulative image scale
        incomplete_render = true;
        cumulative_image_scale *= image_scale;

        // set zoom_factor and offsets ready for the new render
        mandelbrot_transform.scale = Transforms.fractalScaleFromImageScale(image_scale);
        mandelbrot_transform.x = offset_x;
        mandelbrot_transform.y = offset_y;

        // do animation
        ViewPropertyAnimator anim = canvas.animate();
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
                startRender();
            }
        });

        anim.start();
    }

    public void manualZoom(float amount) {
        if (amount == 0) return;

        stopRender();

        // calculate scale
        mandelbrot_transform.scale += amount / Math.hypot(canvas_width, canvas_height);

        // limit scale between max in/out ranges
        mandelbrot_transform.scale = Math.max(settings.max_pinch_zoom_out,
                Math.min(settings.max_pinch_zoom_in, mandelbrot_transform.scale));

        float image_scale = (float) Transforms.imageScaleFromFractalScale(mandelbrot_transform.scale);

        // update cumulative image scale
        incomplete_render = true;
        cumulative_image_scale *= image_scale;

        canvas.setScaleX(image_scale);
        canvas.setScaleY(image_scale);
    }

    public void endManualZoom() {
        // do nothing
    }
    /*** END OF GestureHandler implementation ***/

    private void fixateVisibleImage(boolean instant) {
        int pixels[] = new int[canvas_width * canvas_height];
        if (mandelbrot_transform.scale == 0) {
            fast_getVisibleImage(pixels);
            setDisplay(pixels, TransitionType.NONE);
        } else {
            if (instant) {
                getVisibleImage(false).getPixels(pixels, 0, canvas_width, 0, 0, canvas_width, canvas_height);
                setDisplay(pixels, TransitionType.NONE);
            } else {
                // in the case of a scaled image we cross fade between a bilinear filtered image
                // and a non-bilinear filtered image. this is because the animated zoomed image (autoZoom)
                // is filtered - if we don't do this cross fade then the transition between the animated
                // image and non-filtered image is jarring.
                int pixels_smooth[] = new int[canvas_width * canvas_height];
                getVisibleImage(true).getPixels(pixels_smooth, 0, canvas_width, 0, 0, canvas_width, canvas_height);
                getVisibleImage(false).getPixels(pixels, 0, canvas_width, 0, 0, canvas_width, canvas_height);
                setDisplay(pixels_smooth, TransitionType.NONE);
                setDisplay(pixels, TransitionType.CROSS_FADE, TransitionSpeed.FAST);
            }
        }
        transformMandelbrot();

        // release fixate_synchronise on UI thread to make sure it happens after
        // all the other UI thread events posted in setDisplay
        // note that we don't wait for set_display_anim_latch to be released because
        // we want gesturing to be unpaused as soon as possible
        post(new Runnable() {
            @Override
            public void run() {
                fixate_synchronise.release();
            }
        });
    }

    private void fast_getVisibleImage(int pixels[]) {
        int offset, x, y, width, height;

        Arrays.fill(pixels, background_colour);

        if (mandelbrot_transform.x >= 0) {
            offset = 0;
            x = (int) mandelbrot_transform.x;
            width = canvas_width - (int) mandelbrot_transform.x;
        } else {
            int abs_x = Math.abs((int) mandelbrot_transform.x);
            x = 0;
            offset = abs_x;
            width = canvas_width - abs_x;
        }

        if (mandelbrot_transform.y >= 0) {
            y = (int) mandelbrot_transform.y;
            height = canvas_height - (int) mandelbrot_transform.y;
        } else {
            int abs_y = Math.abs((int) mandelbrot_transform.y);
            y = 0;
            offset += abs_y * canvas_width;
            height = canvas_height - abs_y;
        }

        display_bm.getPixels(pixels, offset, canvas_width, x, y, width, height);
    }

    public Bitmap getVisibleImage(boolean bilinear_filter) {
        // set background colour, otherwise faded reveals in setDisplay() won't work
        Bitmap bm = Bitmap.createBitmap(canvas_width, canvas_height, bitmap_config);
        bm.eraseColor(background_colour);

        double new_left = (mandelbrot_transform.scale * canvas_width);
        double new_right = canvas_width - new_left;
        double new_top = (mandelbrot_transform.scale * canvas_height);
        double new_bottom = canvas_height - new_top;
        new_left += mandelbrot_transform.x;
        new_right += mandelbrot_transform.x;
        new_top += mandelbrot_transform.y;
        new_bottom += mandelbrot_transform.y;
        Rect blit_to = new Rect(0, 0, canvas_width, canvas_height);
        Rect blit_from = new Rect((int) new_left, (int) new_top, (int) new_right, (int) new_bottom);

        Paint paint = new Paint();
        paint.setDither(false);
        paint.setAntiAlias(false);
        if (bilinear_filter) {
            paint.setFilterBitmap(true);
        } else {
            paint.setFilterBitmap(false);
        }

        Canvas canvas = new Canvas(bm);
        canvas.drawBitmap(display_bm, blit_from, blit_to, paint);

        return bm;
    }

    protected boolean setDisplay(int pixels[], @TransitionType int transition_type) {
        return setDisplay(pixels, transition_type, TransitionSpeed.NORMAL, true);
    }

    protected boolean setDisplay(int pixels[], @TransitionType int transition_type, @TransitionSpeed int transition_speed) {
        return setDisplay(pixels, transition_type, transition_speed, true);
    }

    protected boolean setDisplay(int pixels[], @TransitionType int transition_type, @TransitionSpeed int transition_speed, final boolean ui_draw) {
        if (transition_type == TransitionType.CROSS_FADE) {
            // get speed of animation (we'll actually set the speed later)
            final int speed;
            switch (transition_speed) {
                case TransitionSpeed.FAST:
                    speed = getResources().getInteger(R.integer.transition_duration_fast);
                    break;
                case TransitionSpeed.SLOW:
                    speed = getResources().getInteger(R.integer.transition_duration_slow);
                    break;
                default:
                case TransitionSpeed.NORMAL:
                    speed = getResources().getInteger(R.integer.transition_duration_normal);
                    break;
            }

            // prepare foreground. this is the image we transition from
            int foreground_pixels[] = new int[canvas_width * canvas_height];
            display_bm.getPixels(foreground_pixels, 0, canvas_width, 0, 0, canvas_width, canvas_height);
            foreground_bm.setPixels(foreground_pixels, 0, canvas_width, 0, 0, canvas_width, canvas_height);

            post(new Runnable() {
                @Override
                public void run() {
                    foreground.setAlpha(1.0f);
                    foreground.setVisibility(VISIBLE);

                    // we want all this to happen in the same frame
                    foreground.invalidate();
                    if (ui_draw) {
                        normaliseCanvas();
                    }
                    // END OF same frame
                }
            });

            // prepare final image (the image we transition to) this will be
            // obscured by foreground until the end of the animation
            if (ui_draw) {
                set_display_anim_latch.acquire();
            } else {
                if (!set_display_anim_latch.tryAcquire()) {
                    return false;
                }
            }

            display_bm.setPixels(pixels, 0, canvas_width, 0, 0, canvas_width, canvas_height);
            post(new Runnable() {
                @Override
                public void run() {
                    display.invalidate();
                }
            });

            final Runnable cancel_set_display_anim = new Runnable() {
                @Override
                public void run() {
                    foreground.setVisibility(INVISIBLE);
                    set_display_anim_latch.release();
                }
            };

            // do animation
            post(new Runnable() {
                @Override
                public void run() {
                    ViewPropertyAnimator transition_anim = foreground.animate();
                    transition_anim.withEndAction(cancel_set_display_anim);
                    transition_anim.setDuration(speed);
                    transition_anim.alpha(0.0f);
                    transition_anim.start();
                }
            });
        } else {
            display_bm.setPixels(pixels, 0, canvas_width, 0, 0, canvas_width, canvas_height);
            if (ui_draw) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        display.invalidate();
                        normaliseCanvas();
                    }
                });
            } else {
                display.postInvalidate();
            }
        }

        return true;
    }
}

