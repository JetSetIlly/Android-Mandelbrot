package jetsetilly.mandelbrot.Mandelbrot;

import android.content.Context;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Trace;
import android.widget.TextView;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;
import jetsetilly.mandelbrot.Tools;

public class Mandelbrot {
    private final static String DBG_TAG = "mandelbrot";

    public static final int NULL_ITERATIONS = -1;

    public enum RenderMode {HARDWARE, SOFTWARE_TOP_DOWN, SOFTWARE_CENTRE}
    public enum IterationsRate {SLOW, NORMAL, FAST}

    // with the current algorithm, this value is the value of the fastest IterationsRate
    private final int iterations_rate_base = 5;

    private final Context context;
    protected final MandelbrotCanvas canvas;
    private final TextView fractal_info;

    private final MandelbrotSettings mandelbrot_settings = MandelbrotSettings.getInstance();

    private MandelbrotThread render_thr;

    double pixel_scale;

    // render_area is used to define that area of the canvas
    // that needs to be rendered again. pixels outside this area
    // already appear on the canvas
    //
    // if ((pixel.x >= protected_left && pixel.x <= protected_right) || (pixel.y >= protected_top && pixel.y <= protected_bottom))
    // then
    //      render pixel
    // end if
    Rect render_area;

    // is this render a rescaling render (ie. has the zoom level changed)
    // we use this so that progress view is shown immediately
    boolean rescaling_render;

    public Mandelbrot(Context context, MandelbrotCanvas canvas, TextView fractal_info) {
        this.context = context;
        this.canvas = canvas;
        this.fractal_info = fractal_info;
        this.render_thr = null;
    }

    @Override
    public String toString() {
        String info_str = "";

        info_str = info_str + "yu: " + mandelbrot_settings.imaginary_upper + "\n";
        info_str = info_str + "xl: " + mandelbrot_settings.real_left + "\n";
        info_str = info_str + "xr: " + mandelbrot_settings.real_right + "\n";
        info_str = info_str + "yl: " + mandelbrot_settings.imaginary_lower + "\n";
        info_str = info_str + "scale: " + pixel_scale + "\n";
        info_str = info_str + "iterations: " + mandelbrot_settings.max_iterations;

        return info_str;
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

        // save settings
        mandelbrot_settings.save(context);

        // restore straight away - forcing any errors to manifest immediately
        Tools.printDebug(DBG_TAG, "before restore: " + toString());
        mandelbrot_settings.restore(context);
        Tools.printDebug(DBG_TAG, "after restore: " + toString());
    }

    private void transform(double offset_x, double offset_y, double zoom_factor)
    {
        double fractal_width = mandelbrot_settings.real_right - mandelbrot_settings.real_left;
        double fractal_height = mandelbrot_settings.imaginary_upper - mandelbrot_settings.imaginary_lower;

        // zoom
        if (zoom_factor != 0) {
            mandelbrot_settings.real_left += zoom_factor * fractal_width;
            mandelbrot_settings.real_right -= zoom_factor * fractal_width;
            mandelbrot_settings.imaginary_upper -= zoom_factor * fractal_height;
            mandelbrot_settings.imaginary_lower += zoom_factor * fractal_height;

            mandelbrot_settings.max_iterations *= 1 + (zoom_factor / ( iterations_rate_base - mandelbrot_settings.iterations_rate.ordinal()));
        }

        // scroll
        mandelbrot_settings.real_left += offset_x * pixel_scale;
        mandelbrot_settings.real_right += offset_x * pixel_scale;
        mandelbrot_settings.imaginary_upper += offset_y * pixel_scale;
        mandelbrot_settings.imaginary_lower += offset_y * pixel_scale;

        correct();
    }

    /* threading */
    public void stopRender() {
        if (render_thr == null) {
            return;
        }

        render_thr.cancel(false);
        render_thr = null;
    }

    public void transformMandelbrot(double offset_x, double offset_y, double zoom_factor) {
        // this function updates the mandelbrot co-ordinates. stopping any current threads.
        stopRender();
        transform(offset_x, offset_y, zoom_factor);
    }

    public void startRender(double offset_x, double offset_y, double zoom_factor) {
        Trace.beginSection("starting mandelbrot");
        try {
            transformMandelbrot(offset_x, offset_y, zoom_factor);

            // initialise render_area
            render_area = new Rect(0, 0, canvas.getCanvasWidth(), canvas.getCanvasHeight());

            // make sure render mode etc. is set correctly
            rescaling_render = zoom_factor != 0;

            // define render_area
            if (zoom_factor == 0 && canvas.isCompleteRender()) {
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
