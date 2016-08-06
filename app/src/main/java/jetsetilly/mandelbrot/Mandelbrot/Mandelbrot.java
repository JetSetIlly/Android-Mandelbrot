package jetsetilly.mandelbrot.Mandelbrot;

import android.content.Context;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Trace;
import android.widget.TextView;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.R;
import jetsetilly.mandelbrot.RenderCanvas.Transforms;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;
import jetsetilly.mandelbrot.LogTools;

public class Mandelbrot {
    private final static String DBG_TAG = "mandelbrot";

    public static final int NULL_ITERATIONS = -1;

    public enum CalculationMethod {NATIVE, BIG_DECIMAL}

    public enum RenderMode {HARDWARE, SOFTWARE_TOP_DOWN, SOFTWARE_CENTRE, SOFTWARE_SIMPLEST}
    public enum IterationsRate {SLOW, NORMAL, FAST}
    private int[] IterationsRateValues = {50, 40, 30};

    private final Context context;
    protected final MandelbrotCanvas canvas;
    private final TextView fractal_info;

    private final MandelbrotSettings mandelbrot_settings = MandelbrotSettings.getInstance();

    private MandelbrotThread render_thr;

    double pixel_scale;

    // the pixel scale we reckon from when calculating scale amount (the amount we've zoomed in)
    private static final double BASE_PIXEL_SCALE = 0.0030312500000000005;

    // the limit to the amount of scaling the floating point calculations can handle
    // TODO: this limit seems small to me :-(
    private static final double SCALE_LIMIT = 6.0e-17;

    // render_area is used to define that area of the canvas
    // that needs to be rendered again. pixels outside this area
    // already appear on the canvas
    //
    // if ((pixel.x >= protected_left && pixel.x <= protected_right) || (pixel.y >= protected_top && pixel.y <= protected_bottom))
    // then
    //      render pixel
    // end if
    Rect render_area;

    // is this render a rescaling render - we use this so that progress view is shown immediately
    boolean rescaling_render;

    public Mandelbrot(Context context, MandelbrotCanvas canvas, TextView fractal_info) {
        this.context = context;
        this.canvas = canvas;
        this.fractal_info = fractal_info;
        this.render_thr = null;
    }

    @Override
    public String toString() {
        return String.format(context.getResources().getString(R.string.mandelbrot_info_string),
                mandelbrot_settings.imaginary_lower,
                mandelbrot_settings.real_left,
                mandelbrot_settings.real_right,
                mandelbrot_settings.imaginary_lower,
                pixel_scale,
                mandelbrot_settings.max_iterations,
                BASE_PIXEL_SCALE / pixel_scale
        );
    }

    /* transform -> correct -> save */
    private void save() {
        // save settings
        mandelbrot_settings.save(context);

        // restore settings straight away to force out any errors
        // - now removed due to no issues being found
        //mandelbrot_settings.restore(context);
    }

    private void correct()
    {
        // makes sure the pixel_scale is square when spanning the real and imaginary coordinates
        // particularly useful if screen dimensions change, which it does if screen is rotated.

        double canvas_ratio = (double) canvas.getCanvasWidth() / (double) canvas.getCanvasHeight();

        // add padding to real axis
        // note padding will be negative when correcting from landscape to portrait
        double padding = (canvas_ratio * (mandelbrot_settings.imaginary_upper -  mandelbrot_settings.imaginary_lower)) - (mandelbrot_settings.real_right - mandelbrot_settings.real_left);
        mandelbrot_settings.real_right += padding / 2;
        mandelbrot_settings.real_left -= padding / 2;

        // correct pixel scale
        pixel_scale = (mandelbrot_settings.real_right - mandelbrot_settings.real_left) / canvas.getCanvasWidth();

        save();
    }

    private void transform(double offset_x, double offset_y, double fractal_scale)
    {
        double fractal_width = mandelbrot_settings.real_right - mandelbrot_settings.real_left;
        double fractal_height = mandelbrot_settings.imaginary_upper - mandelbrot_settings.imaginary_lower;

        if (fractal_scale != 0) {
            // use image scale value instead of fractal_scale value for calculating max_iterations
            // easier to work with
            double image_scale = Transforms.imageScaleFromFractalScale(fractal_scale);

            if (pixel_scale < SCALE_LIMIT) {
                LogTools.printDebug(DBG_TAG, context.getResources().getString(R.string.scale_limit_reached));
            }

            mandelbrot_settings.real_left += fractal_scale * fractal_width;
            mandelbrot_settings.real_right -= fractal_scale * fractal_width;
            mandelbrot_settings.imaginary_upper -= fractal_scale * fractal_height;
            mandelbrot_settings.imaginary_lower += fractal_scale * fractal_height;

            double iterations_rate = IterationsRateValues[mandelbrot_settings.iterations_rate.ordinal()];
            if (image_scale > 1)
                // scale up
                mandelbrot_settings.max_iterations = mandelbrot_settings.max_iterations + (int) (mandelbrot_settings.max_iterations * image_scale / iterations_rate);
            else {
                // scale down
                mandelbrot_settings.max_iterations = (int) ((mandelbrot_settings.max_iterations * iterations_rate) / (iterations_rate + (1.0/image_scale)));
            }
        }

        mandelbrot_settings.real_left += offset_x * pixel_scale;
        mandelbrot_settings.real_right += offset_x * pixel_scale;
        mandelbrot_settings.imaginary_upper += offset_y * pixel_scale;
        mandelbrot_settings.imaginary_lower += offset_y * pixel_scale;

        correct();
    }
    /* END OF transform -> correct -> save */

    /* threading */
    public void stopRender() {
        if (render_thr == null) {
            return;
        }

        render_thr.cancel(false);
        render_thr = null;
    }

    public void transformMandelbrot(double offset_x, double offset_y, double fractal_scale) {
        // this function updates the mandelbrot co-ordinates. stopping any current threads.
        stopRender();
        transform(offset_x, offset_y, fractal_scale);
    }

    public void startRender(double offset_x, double offset_y, double fractal_scale) {
        Trace.beginSection("starting mandelbrot");
        try {
            transformMandelbrot(offset_x, offset_y, fractal_scale);

            // initialise render_area
            render_area = new Rect(0, 0, canvas.getCanvasWidth(), canvas.getCanvasHeight());

            // make sure render mode etc. is set correctly
            rescaling_render = fractal_scale != 0;

            // define render_area
            if (fractal_scale == 0 && canvas.isCompleteRender()) {
                if (offset_x < 0) {
                    render_area.right = (int) -offset_x;
                } else if (offset_x > 0) {
                    render_area.left = canvas.getCanvasWidth() - (int) offset_x;
                }

                if (offset_y < 0) { // moving down
                    render_area.bottom = (int) -offset_y;
                } else if (offset_y > 0) { // moving up
                    render_area.top = canvas.getCanvasHeight() - (int) offset_y;
                }
            }

            // display mandelbrot info
            fractal_info.setText(this.toString());

            // start render
            MainActivity.progress.startSession();

            if (mandelbrot_settings.render_mode == RenderMode.HARDWARE) {
                render_thr = new MandelbrotThread_renderscript(this);
            } else {
                render_thr = new MandelbrotThread_dalvik(this);
            }

            render_thr.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } finally {
            Trace.endSection();
        }
    }
    /* end of threading */
}
