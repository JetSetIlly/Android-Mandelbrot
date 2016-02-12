package jetsetilly.mandelbrot.Mandelbrot;

import android.content.Context;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.widget.TextView;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;
import jetsetilly.mandelbrot.Tools;

public class Mandelbrot {
    final static public String DBG_TAG = "mandelbrot";

    public enum RenderMode {TOP_DOWN, CENTRE, MIN_TO_MAX}

    private final Context context;
    protected final MandelbrotCanvas canvas;
    private final TextView fractal_info;

    private final MandelbrotSettings mandelbrot_settings = MandelbrotSettings.getInstance();

    private MandelbrotThread render_thr;
    protected boolean render_completed = false;

    protected double pixel_scale;
    private double fractal_ratio;

    /* protected_render_area is used to define that area of the canvas
    that do not need to be rendered again because those pixels (hopefully)
    already appear on the canvas
     */
    protected Rect protected_render_area;

    /* ui related stuff */
    // TODO: this should be part of MandelbrotSettings
    public int num_passes = 2; // in lines

    // is this render a rescaling render (ie. has the zoom level changed)
    // we use this so that progress view is shown immediately
    protected boolean rescaling_render;

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

    private void calculatePixelScale() {
        pixel_scale = (mandelbrot_settings.real_right - mandelbrot_settings.real_left) / canvas.getWidth();
        fractal_ratio = (mandelbrot_settings.real_right - mandelbrot_settings.real_left) / (mandelbrot_settings.imaginary_upper -  mandelbrot_settings.imaginary_lower);
        fractal_info.setText(this.toString());
        Tools.printDebug(DBG_TAG, this.toString());
    }

    public void correctMandelbrotRange()
    {
        double padding;
        double canvas_ratio = (double) canvas.getWidth() / (double) canvas.getHeight();

        fractal_ratio = (mandelbrot_settings.real_right - mandelbrot_settings.real_left) / (mandelbrot_settings.imaginary_upper -  mandelbrot_settings.imaginary_lower);

        // correct range according to canvas dimensions
        if (fractal_ratio > canvas_ratio) {
            // canvas is taller than fractal - expand fractal vertically
            padding = ((mandelbrot_settings.real_right - mandelbrot_settings.real_left) / canvas_ratio) - (mandelbrot_settings.imaginary_upper -  mandelbrot_settings.imaginary_lower);
            mandelbrot_settings.imaginary_upper += padding / 2;
            mandelbrot_settings.imaginary_lower -= padding / 2;
        } else if (fractal_ratio < canvas_ratio) {
            // canvas is wider than fractal - expand fractal horizontally
            padding = (canvas_ratio * (mandelbrot_settings.imaginary_upper -  mandelbrot_settings.imaginary_lower)) - (mandelbrot_settings.real_right - mandelbrot_settings.real_left);
            mandelbrot_settings.real_right += padding / 2;
            mandelbrot_settings.real_left -= padding / 2;
        }

        calculatePixelScale();

        mandelbrot_settings.save(context);
    }

    private void scrollAndZoom(double offset_x, double offset_y, double zoom_factor)
    {
        double fractal_width = mandelbrot_settings.real_right - mandelbrot_settings.real_left;
        double fractal_height = mandelbrot_settings.imaginary_upper - mandelbrot_settings.imaginary_lower;

        // zoom
        if (zoom_factor != 0) {
            mandelbrot_settings.real_left += zoom_factor * fractal_width;
            mandelbrot_settings.real_right -= zoom_factor * fractal_width;
            mandelbrot_settings.imaginary_upper -= zoom_factor * fractal_height;
            mandelbrot_settings.imaginary_lower += zoom_factor * fractal_height;
            mandelbrot_settings.max_iterations *= 1 + (zoom_factor / 3);
        }

        // scroll
        mandelbrot_settings.real_left += offset_x * pixel_scale;
        mandelbrot_settings.real_right += offset_x * pixel_scale;
        mandelbrot_settings.imaginary_upper += offset_y * pixel_scale;
        mandelbrot_settings.imaginary_lower += offset_y * pixel_scale;

        correctMandelbrotRange();
    }

    /* threading */
    public void stopRender() {
        if ( render_thr == null ) {
            return;
        }

        render_thr.cancel(false);
        render_thr = null;
    }

    public void preRender(double offset_x, double offset_y, double zoom_factor) {
        /* this function updates the mandelbrot co-ordinates. stopping any current threads.
        note that it also sets render_completed to false. this forces startRender to not create
        a protected area and will force the entire defined mandelbrot space to be recalculated.

        this is used to support RenderCanvas.scaleCorrection(). this is a clumsy way of allowing
        for the chaining of pinch-zoom and scrolling. we can remove this method once a simpler way is
        figured out.
        */
        stopRender();
        scrollAndZoom(offset_x, offset_y, zoom_factor);
        render_completed = false;
    }

    public void startRender(double offset_x, double offset_y, double zoom_factor) {
        stopRender();
        scrollAndZoom(offset_x, offset_y, zoom_factor);

        // initialise protected_render_area
        protected_render_area = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());

        // make sure render mode etc. is set correctly
        rescaling_render = zoom_factor != 0;

        if (zoom_factor == 0 && render_completed) {
            /* define protected_render_area more accurately */
            if (offset_x < 0) {
                protected_render_area.right = (int) -offset_x;
            } else if (offset_x > 0) {
                protected_render_area.left = canvas.getWidth() - (int) offset_x;
            }

            if (offset_y < 0) {
                protected_render_area.top = (int) -offset_y;
            } else if (offset_y > 0) {
                protected_render_area.bottom = canvas.getHeight() - (int) offset_y;
            }
        }

        MainActivity.progress.startSession();
        render_thr = new MandelbrotThread(this);
        render_thr.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
    /* end of threading */
}
