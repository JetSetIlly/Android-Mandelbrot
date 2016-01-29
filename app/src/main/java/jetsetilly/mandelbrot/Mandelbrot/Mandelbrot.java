package jetsetilly.mandelbrot.Mandelbrot;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;

public class Mandelbrot {
    final static public String DBG_TAG = "mandelbrot";

    public enum RenderMode {TOP_DOWN, CENTRE, MIN_TO_MAX}

    private final Context context;
    private final MandelbrotSettings mandelbrot_settings = MandelbrotSettings.getInstance();

    private MandelbrotThread render_thr[];
    protected boolean render_completed = false;

    protected final MandelbrotCanvas canvas;
    protected double pixel_scale;
    private double fractal_ratio;

    /* no_render_area is used to define that area of the canvas
    that do not need to be rendered again because those pixels (hopefully)
    already appear on the canvas
     */
    protected Rect no_render_area;

    /* ui related stuff */
    private final int DEF_NUM_PASSES = 2;
    private final int DEF_UPDATE_FREQ = 1;

    public int num_passes = DEF_NUM_PASSES; // in lines
    public int canvas_update_frequency = DEF_UPDATE_FREQ; // in lines

    // is this render a rescaling render (ie. has the zoom level changed)
    // we use this so that progress view is shown immediately
    protected boolean rescaling_render;

    public Mandelbrot(Context context, MandelbrotCanvas canvas) {
        this.context = context;
        this.canvas = canvas;
        this.render_thr = null;
    }

    @Override
    public String toString() {
        String ret_val = super.toString() + "\n";

        ret_val = ret_val + "xl: " + mandelbrot_settings.real_left + " xr: " + mandelbrot_settings.real_right + " yu: " + mandelbrot_settings.imaginary_upper + " yl: " + mandelbrot_settings.imaginary_lower;
        ret_val = ret_val + "pixel scale: " + pixel_scale + "\n";
        ret_val = ret_val + "fractal ratio: " + fractal_ratio + "\n";
        ret_val = ret_val + "max iterations: " + mandelbrot_settings.max_iterations + "\n";
        return ret_val;
    }

    private void calculatePixelScale() {
        pixel_scale = (mandelbrot_settings.real_right - mandelbrot_settings.real_left) / canvas.getWidth();
        fractal_ratio = (mandelbrot_settings.real_right - mandelbrot_settings.real_left) / (mandelbrot_settings.imaginary_upper -  mandelbrot_settings.imaginary_lower);

        Log.d(DBG_TAG, this.toString());
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

        Log.d(DBG_TAG, this.toString());
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

        for (int i = 0; i < render_thr.length; ++ i) {
            if (render_thr[i] != null) {
                render_thr[i].cancel(true);
                render_thr[i] = null;
            }
        }
    }

    public void startRender(double offset_x, double offset_y, double zoom_factor) {
        MainActivity.progress.startSession();
        stopRender();

        scrollAndZoom(offset_x, offset_y, zoom_factor);

        // initialise no_render_area
        no_render_area = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());

        // make sure render mode etc. is set correctly
        rescaling_render = zoom_factor != 0;

        num_passes = DEF_NUM_PASSES;
        canvas_update_frequency = DEF_UPDATE_FREQ;

        if (zoom_factor == 0 && render_completed) {
            /* define no_render_area more accurately */
            if (offset_x < 0) {
                no_render_area.right = (int) -offset_x;
            } else if (offset_x > 0) {
                no_render_area.left = canvas.getWidth() - (int) offset_x;
            }

            if (offset_y < 0) {
                no_render_area.top = (int) -offset_y;
            } else if (offset_y > 0) {
                no_render_area.bottom = canvas.getHeight() - (int) offset_y;
            }
        }

        calculatePixelScale();

        if (mandelbrot_settings.parallel_render) {
            render_thr = new MandelbrotThread[mandelbrot_settings.max_iterations];
            for (int i = 0; i < mandelbrot_settings.max_iterations; ++ i){
                render_thr[i] = new MandelbrotThread(this, i+1);
                render_thr[i].execute();
            }
        } else {
            render_thr = new MandelbrotThread[1];
            render_thr[0] = new MandelbrotThread(this, -1);
            render_thr[0].execute();
        }
    }
    /* end of threading */
}
