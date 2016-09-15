package jetsetilly.mandelbrot.RenderCanvas.ImageView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.util.AttributeSet;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.mandelbrot.R;
import jetsetilly.mandelbrot.RenderCanvas.Base.RenderCanvas_Base;
import jetsetilly.tools.LogTools;
import jetsetilly.tools.SimpleAsyncTask;
import jetsetilly.tools.SimpleLatch;

public class RenderCanvas_ImageView extends RenderCanvas_Base {
    private final String DBG_TAG = "render canvas";

    // width/height values set in onSizeChanged() - rather than relying on getWidth()/getHeight()
    // which are only callable from the UIThread
    private int canvas_width, half_canvas_width;
    private int canvas_height, half_canvas_height;
    private int num_pixels;

    // bitmap config to use_next depending on SystemSettings.deep_colour
    Bitmap.Config bitmap_config;

    // dominant colour to use_next as background colour - visible on scroll and zoom out events
    private int background_colour;

    // we actually use the background color of this main class but the background ImageView
    // is used to fade between background colors (very low memory usage)
    private ImageView background;

    // layout to contain the display and foreground ImageViews, defined below
    // all transforms are performed on this layout
    private RelativeLayout display_group;

    // canvas on which the fractal is drawn -- all transforms (scrolling, scaling) affect
    // this view only
    private ImageView display;

    // that ImageView that sits in front of RenderCanvas_ImageView in the layout
    // allows animated changes
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
    private Plotter buffer;
    private SimpleLatch buffer_latch = new SimpleLatch();
    private final long NO_RENDER_ID = -1;
    private long this_render_id = NO_RENDER_ID;

    // the amount of scaling since the last rendered image
    private double cumulative_scale = 1.0f;

    // maximum value of cumulative_scale allowed before zoom is paused
    private static float MAX_SCALE = 16.0f;

    // maximum value of cumulative_scale for bilinear filtering of the zoomed image
    private static float BILINEAR_FILTER_LIMIT = 9.0f;

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

        // set initial background colour so that we can fade the first image into
        // view (we actually just need an opaque - or nearly opaque - alpha channel
        // for the fade to work correctly)
        background_colour = 0xFFFFFFFF;

        background = new ImageView(context);
        background.setAlpha(0.0f);

        display_group = new RelativeLayout(context);

        display = new ImageView(context);
        display.setScaleType(ImageView.ScaleType.CENTER);
        display.setLayerType(LAYER_TYPE_HARDWARE, null);

        foreground = new ImageView(context);
        foreground.setLayerType(LAYER_TYPE_HARDWARE, null);
        foreground.setAlpha(0.0f);

        addView(background);
        addView(display_group);
        display_group.addView(display);
        display_group.addView(foreground);

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
        num_pixels = canvas_width * canvas_height;
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

    @Override // View
    public void setBackgroundColor(final int colour) {
        if (background_colour != colour) {
            background_colour = colour;
            super.setBackgroundColor(colour);
        }
    }

    public void resetCanvas() {
        // new render cache
        stopRender();

        super.resetCanvas();
        startRender();
    }

    /*** MandelbrotCanvas implementation ***/
    @WorkerThread
    public void startDraw(long render_id) {
        buffer_latch.acquire();

        if (this_render_id != render_id && this_render_id != NO_RENDER_ID) {
            // this shouldn't happen because of the buffer latch
            LogTools.printWTF(DBG_TAG, "starting new MandelbrotCanvas draw session before finishing another");
        }

        this_render_id = render_id;
        incomplete_render = true;

        if (settings.render_mode == Mandelbrot.RenderMode.HARDWARE) {
            buffer = new PlotterSimple(this);
        } else {
            buffer = new PlotterTimer(this);
        }

        buffer.startDraw(display_bm);
    }

    @WorkerThread
    public void plotIterations(long render_id, int iterations[], boolean complete_plot) {
        if (this_render_id != render_id || buffer == null) return;
        buffer.plotIterations(iterations);
    }

    @WorkerThread
    public void plotIteration(long render_id, int cx, int cy, int iteration) {
        if (this_render_id != render_id || buffer == null) return;
        buffer.plotIteration(cx, cy, iteration);
    }

    @UiThread
    public void update(long render_id) {
        if (this_render_id != render_id || buffer == null) return;
        buffer.update();
    }

    @UiThread
    public void endDraw(long render_id, boolean cancelled) {
        if (this_render_id != render_id || buffer == null) {
            return;
        }

        buffer.endDraw(cancelled);

        incomplete_render = cancelled;
        this_render_id = NO_RENDER_ID;

        if (!incomplete_render) {
            cumulative_scale = 1.0f;
            gestures.unpauseZoom();
        }

        renderThreadEnded();
        buffer = null;
        buffer_latch.release();
    }
    /*** END OF MandelbrotCanvas implementation ***/

    private void normaliseCanvas(){
        display_group.setScaleX(1f);
        display_group.setScaleY(1f);
        display_group.setX(0);
        display_group.setY(0);
        display_group.setPivotX(half_canvas_width);
        display_group.setPivotY(half_canvas_height);
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
                        fixateVisibleImage();
                        fixate_synchronise.monitor();

                        if (settings.render_mode != Mandelbrot.RenderMode.HARDWARE || cumulative_scale < MAX_SCALE) {
                            // unpause zoom gesture if we're below that maximum image scale or
                            // if we're using software rendering
                            gestures.unpauseZoom();
                        }
                        gestures.unpauseScroll();

                        // now that gestures have been unpaused, we'll wait for setDisplay() anim
                        // to complete for starting new render
                        //set_display_anim_latch.monitor();
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
        //stopRender();

        display_group.setX(display_group.getX() - (x / mandelbrot_transform.scale));
        display_group.setY(display_group.getY() - (y / mandelbrot_transform.scale));

        display_group.setPivotX(display_group.getPivotX() + (x / mandelbrot_transform.scale));
        display_group.setPivotY(display_group.getPivotY() + (y / mandelbrot_transform.scale));

        mandelbrot_transform.x += x / mandelbrot_transform.scale;
        mandelbrot_transform.y += y / mandelbrot_transform.scale;
    }

    public void autoZoom(float offset_x, float offset_y, boolean zoom_out) {
        // some combinations of scroll and zooming don't work
        if (!(display_group.getX() == 0 && display_group.getY() == 0 && mandelbrot_transform.scale == 1.0f)) {
            gestures.pauseZoom(true);
            return;
        }

        stopRender();

        // pause gestures - startRender() will unpause as appropriate
        gestures.pauseZoom(false);
        gestures.pauseScroll();

        // offsets are provided such that they are reckoned from top-left corner of the screen
        // however, for animation purposes we want to reckon from the centre of the screen
        offset_x -= half_canvas_width;
        offset_y -= half_canvas_height;

        // restrict offset_x and offset_y so that the zoomed image doesn't show
        // the background image
        if (offset_x > half_canvas_width - (half_canvas_width / settings.double_tap_scale)) {
            offset_x = half_canvas_width -  (half_canvas_width / settings.double_tap_scale);
        } else if (offset_x < - half_canvas_width + (half_canvas_width / settings.double_tap_scale)) {
            offset_x = - half_canvas_width + (half_canvas_width / settings.double_tap_scale);
        }

        if (offset_y > half_canvas_height - (half_canvas_height / settings.double_tap_scale)) {
            offset_y = half_canvas_height - (half_canvas_height / settings.double_tap_scale);
        } else if (offset_y < - half_canvas_height + (half_canvas_height / settings.double_tap_scale)) {
            offset_y = - half_canvas_height + (half_canvas_height / settings.double_tap_scale);
        }

        // set mandelbrot transform ready for the new render
        mandelbrot_transform.x = offset_x;
        mandelbrot_transform.y = offset_y;
        if (zoom_out) {
            // no user setting to control how much to zoom out
            mandelbrot_transform.scale *= 0.5f;
        } else {
            mandelbrot_transform.scale *= settings.double_tap_scale;
        }

        // update cumulative image scale
        incomplete_render = true;
        cumulative_scale *= mandelbrot_transform.scale;

        // do animation
        ViewPropertyAnimator anim = display_group.animate();
        anim.setDuration(getResources().getInteger(R.integer.animated_zoom_duration_fast));
        anim.x(-offset_x * mandelbrot_transform.scale);
        anim.y(-offset_y * mandelbrot_transform.scale);
        anim.scaleX(mandelbrot_transform.scale);
        anim.scaleY(mandelbrot_transform.scale);

        anim.withStartAction(new Runnable() {
            @Override
            public void run() {
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
        mandelbrot_transform.scale += amount/1000;

        // limit scale between max in/out ranges
        mandelbrot_transform.scale = Math.max(settings.max_pinch_zoom_out,
                Math.min(settings.max_pinch_zoom_in, mandelbrot_transform.scale));

        // update cumulative image scale
        incomplete_render = true;
        cumulative_scale *= mandelbrot_transform.scale;

        display_group.setScaleX(mandelbrot_transform.scale);
        display_group.setScaleY(mandelbrot_transform.scale);
    }

    public void endManualZoom() {
        // do nothing
    }
    /*** END OF GestureHandler implementation ***/

    private void fixateVisibleImage() {
        Bitmap block_pixels_bm = getVisibleImage(true);

        if (mandelbrot_transform.scale > 1.0f && cumulative_scale < BILINEAR_FILTER_LIMIT) {
            Bitmap smooth_pixels_bm = getVisibleImage(false);
            setImageInstant(smooth_pixels_bm);
            setImageNormalise(block_pixels_bm);
        } else {
            setImageInstant(block_pixels_bm);
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

    private Bitmap getVisibleImage(boolean block_pixels) {
        Bitmap bm = Bitmap.createBitmap(canvas_width, canvas_height, bitmap_config);
        bm.eraseColor(background_colour);
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint();
        paint.setFilterBitmap(!block_pixels);
        Matrix matrix = new Matrix();
        matrix.setTranslate(-mandelbrot_transform.x, -mandelbrot_transform.y);
        matrix.postScale(mandelbrot_transform.scale, mandelbrot_transform.scale, half_canvas_width, half_canvas_height);
        canvas.drawBitmap(display_bm, matrix, paint);
        return bm;
    }

    protected boolean setImageNew(int pixels[]) {
        // acquire latch to prevent conflicting animations
        // don't try too hard though - don't continue if someone
        // else has the latch
        if (!set_display_anim_latch.tryAcquire()) {
            return false;
        }

        // prepare foreground. this is the image we transition from
        int foreground_pixels[] = new int[num_pixels];
        display_bm.getPixels(foreground_pixels, 0, canvas_width, 0, 0, canvas_width, canvas_height);
        foreground_bm.setPixels(foreground_pixels, 0, canvas_width, 0, 0, canvas_width, canvas_height);

        postOnAnimation(new Runnable() {
            @Override
            public void run() {
                foreground.setAlpha(1.0f);
                foreground.invalidate();
            }
        });

        // prepare final image (the image we transition to) this will be
        // obscured by foreground until the end of the animation
        display_bm.setPixels(pixels, 0, canvas_width, 0, 0, canvas_width, canvas_height);
        display.postInvalidate();

        // do animation
        postOnAnimation(new Runnable() {
            @Override
            public void run() {
                ViewPropertyAnimator transition_anim = foreground.animate();
                transition_anim.withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        set_display_anim_latch.release();
                    }
                });
                transition_anim.setDuration(getResources().getInteger(R.integer.image_fade_new));
                transition_anim.alpha(0.0f);
                transition_anim.start();
            }
        });

        return true;
    }

    protected void setImageInstant(Bitmap bm) {
        display_bm = bm;
        postOnAnimation(new Runnable() {
            @Override
            public void run() {
                display.setImageBitmap(display_bm);
                normaliseCanvas();
            }
        });
    }

    protected void setImageNormalise(Bitmap bm) {
        // acquire latch to prevent conflicting animations
        set_display_anim_latch.acquire();

        // prepare foreground. this is the image we transition from
        foreground_bm = display_bm;

        postOnAnimation(new Runnable() {
            @Override
            public void run() {
                foreground.setImageBitmap(foreground_bm);
                foreground.setAlpha(1.0f);
                normaliseCanvas();
            }
        });

        // prepare final image (the image we transition to) this will be
        // obscured by foreground until the end of the animation
        display_bm = bm;

        postOnAnimation(new Runnable() {
            @Override
            public void run() {
                display.setImageBitmap(display_bm);
                ViewPropertyAnimator transition_anim = foreground.animate();
                transition_anim.withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        set_display_anim_latch.release();
                    }
                });
                transition_anim.setDuration(getResources().getInteger(R.integer.image_fade_normalise));
                transition_anim.alpha(0.0f);
                transition_anim.start();
            }
        });
    }

    public Bitmap getScreenshot() {
        return display_bm.copy(display_bm.getConfig(), false);
    }

}

