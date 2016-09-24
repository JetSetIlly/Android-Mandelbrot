package jetsetilly.mandelbrot.RenderCanvas.ImageView;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.util.AttributeSet;
import android.view.ViewGroup;
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

    // bitmap config to use_next depending on SystemSettings.deep_colour
    Bitmap.Config bitmap_config;

    // dominant colour to use_next as background colour - visible on scroll and zoom out events
    private int background_colour;

    // the animator that handles background_colour changes
    private ValueAnimator background_transition_anim;

    // layout to contain the display and display_curtain ImageViews, defined below
    // all transforms are performed on this layout
    private RelativeLayout display_group;

    // canvas on which the fractal is drawn -- all transforms (scrolling, scaling) affect
    // this view only
    private ImageView display;

    // that ImageView that sits in front of RenderCanvas_ImageView in the layout
    // allows animated changes
    private ImageView display_curtain;

    // the display_bm is a pointer to whatever bitmap is currently displayed in display
    private Bitmap display_bm;

    // reference to the task that prepares the main render thread
    // used to cancel render_task before it has started properly
    private SimpleAsyncTask startup_render_task;

    // prevents setDisplay() animation from running if latch has been acquired
    // and pauses startRender() until an active setDisplay() animation has ended
    // more primitive solution is to run setDisplay().animate.withEndAction() before
    // proceeding with startRender()
    private SimpleLatch set_image_anim_latch = new SimpleLatch();

    // plotter implementation
    private Plotter plotter;
    private SimpleLatch plotter_latch = new SimpleLatch();
    private final long NO_RENDER_ID = -1;
    private long this_render_id = NO_RENDER_ID;

    // the amount of scaling since the last rendered image
    private float cumulative_scale = 1.0f;

    // has max scale been reached -- used to stop further positive scaling
    private boolean max_scale_reached;

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

        /*** display group ***/
        display_group = new RelativeLayout(context);
        display_group.setLayoutParams(new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        display = new ImageView(context);
        display.setScaleType(ImageView.ScaleType.MATRIX);
        display.setLayerType(LAYER_TYPE_HARDWARE, null);

        display_curtain = new ImageView(context);
        display_curtain.setScaleType(ImageView.ScaleType.MATRIX);
        display_curtain.setLayerType(LAYER_TYPE_HARDWARE, null);
        display_curtain.setAlpha(0.0f);

        addView(display_group);
        display_group.addView(display);
        display_group.addView(display_curtain);
        /*** END OF display group ***/

        post(new Runnable() {
            @Override
            public void run() {
                display_bm = Bitmap.createBitmap(geometry.width, geometry.height, bitmap_config);
                display_bm.eraseColor(0xFF000000);

                display.setImageBitmap(display_bm);
                invalidate();
                resetCanvas();
            }
        });
    }
    /*** END OF initialisation ***/

    @Override // View
    public void invalidate() {
        post(new Runnable() {
            @Override
            public void run() {
                display.invalidate();
                display_curtain.invalidate();
            }
        });
        // not calling super method
    }

    public void setBaseColour(int new_colour) {
        if (background_colour != new_colour) {
            background_transition_anim = ValueAnimator.ofObject(new ArgbEvaluator(), background_colour, new_colour);
            background_transition_anim.setIntValues(background_colour, new_colour);
            background_transition_anim.setDuration(getResources().getInteger(R.integer.background_transition));
            background_transition_anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animator) {
                    background_colour = (int) animator.getAnimatedValue();
                    setBackgroundColor(background_colour);
                }
            });
            background_transition_anim.start();
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
    public void startPlot(long render_id) {
        plotter_latch.acquire();

        if (this_render_id != render_id && this_render_id != NO_RENDER_ID) {
            // this shouldn't happen because of the plotter latch
            LogTools.printWTF(DBG_TAG, "starting new MandelbrotCanvas draw session before finishing another");
        }

        this_render_id = render_id;
        incomplete_render = true;

        if (settings.render_mode == Mandelbrot.RenderMode.HARDWARE) {
            plotter = new PlotterSimple(this);
        } else {
            plotter = new PlotterTimer(this);
        }

        plotter.startPlot(display_bm);
    }

    @WorkerThread
    public void plotIterations(long render_id, int iterations[], boolean complete_plot) {
        if (this_render_id != render_id || plotter == null) return;
        plotter.plotIterations(iterations);
    }

    @WorkerThread
    public void plotIteration(long render_id, int cx, int cy, int iteration) {
        if (this_render_id != render_id || plotter == null) return;
        plotter.plotIteration(cx, cy, iteration);
    }

    @UiThread
    public void updatePlot(long render_id) {
        if (this_render_id != render_id || plotter == null) return;
        plotter.updatePlot();
    }

    @UiThread
    public void endPlot(long render_id, boolean cancelled) {
        if (this_render_id != render_id || plotter == null) {
            return;
        }

        plotter.endPlot(cancelled);

        incomplete_render = cancelled;
        this_render_id = NO_RENDER_ID;

        if (!incomplete_render) {
            cumulative_scale = 1.0f;
            max_scale_reached = false;
        }

        renderThreadEnded();
        plotter = null;
        plotter_latch.release();
    }
    /*** END OF MandelbrotCanvas implementation ***/

    private void normaliseCanvas(){
        display_group.setScaleX(1f);
        display_group.setScaleY(1f);
        display_group.setPivotX(geometry.width/2);
        display_group.setPivotY(geometry.height/2);
        display_group.setX(0);
        display_group.setY(0);
    }

    public void startRender() {
        // make sure gestures are paused while we're monkeying with the display
        gestures.pauseGestures();

        stopRender();

        // halt background color changes
        if (background_transition_anim != null) background_transition_anim.cancel();

        startup_render_task = new SimpleAsyncTask("RenderCanvas_ImageView.startRender",
                new SimpleAsyncTask.AsyncRunnable() {
                    @Override
                    public void run() {
                        // wait for any setDisplay() animation to finish before proceeding
                        set_image_anim_latch.monitor();

                        if (isCancelled()) return;

                        // wait for plotter task to finish
                        plotter_latch.monitor();

                        if (isCancelled()) return;

                        // get normalised bitmaps
                        Bitmap block_pixels_bm = getVisibleImage(true);
                        Bitmap smooth_pixels_bm = null;
                        if (mandelbrot_transform.scale > 1.0f) {
                            smooth_pixels_bm = getVisibleImage(false);
                        }

                        if (isCancelled()) return;

                        // display normalised bitmaps and update mandelbrot for new render
                        if (smooth_pixels_bm != null) {
                            setImageInstant(smooth_pixels_bm);
                            setImageFade(block_pixels_bm);
                        } else {
                            setImageInstant(block_pixels_bm);
                        }

                        transformMandelbrot();

                        // unpause gestures
                        gestures.unpauseGestures();

                        // see if MAX_CUMULATIVE_SCALE has been reached
                        if (settings.render_mode == Mandelbrot.RenderMode.HARDWARE && cumulative_scale >= settings.max_cumulative_scale) {
                            max_scale_reached = true;
                        }
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
        if (startup_render_task != null) {
            startup_render_task.cancel();
        }
        stopRenderThread();
    }

    /*** GestureHandler implementation ***/
    public void finishManualGesture() {
        if (!mandelbrot_transform.isIdentity()) {
            startRender();
        }
    }

    @Override // View
    public void scroll(float x, float y) {
        display_group.setX(display_group.getX() - (x / mandelbrot_transform.scale));
        display_group.setY(display_group.getY() - (y / mandelbrot_transform.scale));

        display_group.setPivotX(display_group.getPivotX() + (x / mandelbrot_transform.scale));
        display_group.setPivotY(display_group.getPivotY() + (y / mandelbrot_transform.scale));

        mandelbrot_transform.x += x / mandelbrot_transform.scale;
        mandelbrot_transform.y += y / mandelbrot_transform.scale;
    }

    public boolean autoZoom(float offset_x, float offset_y, boolean zoom_out) {
        // don't allow any further zooming in
        if (!zoom_out && max_scale_reached) return false;

        // this is a special case - we don't want to animate when mandelbrot is not in a safe
        // condition - we do this by seeing if the mandelbrot_transform is in the identity state
        // other options here are to wait for the transform (and the display) to normalise
        // but we'll block the UI thread if we do that
        if (!mandelbrot_transform.isIdentity()) {
            LogTools.printWTF(DBG_TAG, "trying to auto zoom in an unsafe state");
            return false;
        }

        // pause gestures - startRender() will unpause as appropriate
        gestures.pauseGestures();

        float half_width = geometry.width / 2;
        float half_height = geometry.height / 2;

        // offsets are provided such that they are reckoned from top-left corner of the screen
        // however, for animation purposes we want to reckon from the centre of the screen
        offset_x -= half_width;
        offset_y -= half_height;

        // restrict offset_x and offset_y so that the zoomed image doesn't show
        // the background image
        if (offset_x > half_width - (half_width / settings.double_tap_scale)) {
            offset_x = half_width -  (half_width / settings.double_tap_scale);
        } else if (offset_x < -half_width + (half_width / settings.double_tap_scale)) {
            offset_x = -half_width + (half_width / settings.double_tap_scale);
        }

        if (offset_y > half_height - (half_height / settings.double_tap_scale)) {
            offset_y = half_height - (half_height / settings.double_tap_scale);
        } else if (offset_y < -half_height + (half_height / settings.double_tap_scale)) {
            offset_y = -half_height + (half_height / settings.double_tap_scale);
        }

        // prepare final state -- we'll copy these values to mandelbrot_transform in the endAction()
        mandelbrot_transform.x = offset_x;
        mandelbrot_transform.y = offset_y;
        if (zoom_out) {
            // no user setting to control how much to zoom out
            mandelbrot_transform.scale = 0.5f;
        } else {
            // cap cumulative scale at MAX_CUMULATIVE_SCALE
            if (cumulative_scale * settings.double_tap_scale > settings.max_cumulative_scale) {
                mandelbrot_transform.scale = settings.max_cumulative_scale / cumulative_scale;
            } else {
                mandelbrot_transform.scale = settings.double_tap_scale;
            }
        }

        // do animation
        ViewPropertyAnimator anim = display_group.animate();
        anim.setDuration(getResources().getInteger(R.integer.animated_zoom_duration_fast));
        anim.x(-mandelbrot_transform.x * mandelbrot_transform.scale);
        anim.y(-mandelbrot_transform.y * mandelbrot_transform.scale);
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
                cumulative_scale *= mandelbrot_transform.scale;
                startRender();
            }
        });

        anim.start();

        return true;
    }

    public boolean manualZoom(float amount) {
        if (amount == 0) return true;

        // don't allow any further zooming in
        if (amount > 0 && max_scale_reached) return false;

        // calculate scale - adjusting amount sensitivity
        mandelbrot_transform.scale += amount / 1200;

        // limit scale between max in/out ranges
        mandelbrot_transform.scale = Math.max(settings.max_pinch_zoom_out,
                Math.min(settings.max_pinch_zoom_in, mandelbrot_transform.scale));

        display_group.setScaleX(mandelbrot_transform.scale);
        display_group.setScaleY(mandelbrot_transform.scale);

        return true;
    }

    public void endManualZoom() {
        // update cumulative image scale
        incomplete_render = true;
        cumulative_scale *= mandelbrot_transform.scale;
    }
    /*** END OF GestureHandler implementation ***/

    private Bitmap getVisibleImage(boolean block_pixels) {
        Bitmap bm = Bitmap.createBitmap(geometry.width, geometry.height, bitmap_config);
        bm.eraseColor(background_colour);
        Canvas canvas = new Canvas(bm);
        Paint paint = new Paint();
        paint.setFilterBitmap(!block_pixels);
        paint.setAntiAlias(true);
        Matrix matrix = new Matrix();
        matrix.setTranslate(-mandelbrot_transform.x, -mandelbrot_transform.y);
        matrix.postScale(mandelbrot_transform.scale, mandelbrot_transform.scale, geometry.width/2, geometry.height/2);
        canvas.drawBitmap(display_bm, matrix, paint);
        return bm;
    }

    protected void setImageNew(int pixels[]) {
        // acquire latch to prevent conflicting animations
        set_image_anim_latch.acquire();

        // prepare display_curtain. this is the image we transition from
        final Bitmap curtain_bm = Bitmap.createBitmap(display_bm);
        post(new Runnable() {
            @Override
            public void run() {
                display_curtain.setImageBitmap(curtain_bm);
                display_curtain.setAlpha(1.0f);
                display_curtain.invalidate();
            }
        });

        // prepare final image (the image we transition to) this will be
        // obscured by display_curtain until the end of the animation
        display_bm.setPixels(pixels, 0, geometry.width, 0, 0, geometry.width, geometry.height);
        display.postInvalidate();

        // do animation
        post(new Runnable() {
            @Override
            public void run() {
                ViewPropertyAnimator curtain_anim = display_curtain.animate();
                curtain_anim.withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        set_image_anim_latch.release();
                        display_curtain.setImageBitmap(null);
                    }
                });
                curtain_anim.setDuration(getResources().getInteger(R.integer.image_fade_new));
                curtain_anim.alpha(0.0f);
                curtain_anim.start();
            }
        });
    }

    protected void setImageInstant(Bitmap bm) {
        display_bm = bm;
        post(new Runnable() {
            @Override
            public void run() {
                display.setImageBitmap(display_bm);
                normaliseCanvas();
            }
        });
    }

    protected void setImageFade(Bitmap bm) {
        // acquire latch to prevent conflicting animations
        set_image_anim_latch.acquire();

        // prepare display_curtain. this is the image we transition from
        final Bitmap curtain_bm = Bitmap.createBitmap(display_bm);
        post(new Runnable() {
            @Override
            public void run() {
                display_curtain.setImageBitmap(curtain_bm);
                display_curtain.setAlpha(1.0f);
                normaliseCanvas();
            }
        });

        // prepare final image (the image we transition to) this will be
        // obscured by display_curtain until the end of the animation
        display_bm = bm;
        post(new Runnable() {
            @Override
            public void run() {
                display.setImageBitmap(display_bm);
                ViewPropertyAnimator curtain_anim = display_curtain.animate();
                curtain_anim.withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        set_image_anim_latch.release();
                        display_curtain.setImageBitmap(null);
                    }
                });
                curtain_anim.setDuration(getResources().getInteger(R.integer.image_fade_normalise));
                curtain_anim.alpha(0.0f);
                curtain_anim.start();
            }
        });
    }

    private Bitmap getDisplayBitmap() {
        return ((BitmapDrawable)display.getDrawable()).getBitmap();
    }

    public Bitmap getScreenshot() {
        Bitmap display_bm = getDisplayBitmap();
        return display_bm.copy(display_bm.getConfig(), false);
    }
}
