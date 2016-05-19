package jetsetilly.mandelbrot.Mandelbrot;

import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.ScriptC_iterations;

public class MandelbrotThread_renderscript extends MandelbrotThread {
    final static public String DBG_TAG = "mandelbrot thread (renderscript)";

    public MandelbrotThread_renderscript(Mandelbrot context) {
        super(context);
    }

    @Override
    protected Void doInBackground(Void... params) {
        super.doInBackground(params);

        int canvas_width = m.canvas.getCanvasWidth();
        int canvas_height = m.canvas.getCanvasHeight();

        Allocation allocation_iterations = Allocation.createSized(MainActivity.render_script,
                Element.I32(MainActivity.render_script), canvas_height * canvas_width,
                Allocation.USAGE_SCRIPT);
        ScriptC_iterations script = new ScriptC_iterations(MainActivity.render_script);

        // set variables/arguments for this render
        script.set_canvas_height(canvas_height);
        script.set_canvas_width(canvas_width);
        script.set_max_iterations(mandelbrot_settings.max_iterations);
        script.set_null_iteration(Mandelbrot.NULL_ITERATIONS);
        script.set_bailout_value(mandelbrot_settings.bailout_value);
        script.set_imaginary_lower(mandelbrot_settings.imaginary_lower);
        script.set_imaginary_upper(mandelbrot_settings.imaginary_upper);
        script.set_real_left(mandelbrot_settings.real_left);
        script.set_real_right(mandelbrot_settings.real_right);
        script.set_pixel_scale(m.pixel_scale);
        script.set_render_left(m.render_area.left);
        script.set_render_right(m.render_area.right);
        script.set_render_top(m.render_area.top);
        script.set_render_bottom(m.render_area.bottom);

        // launch script
        script.forEach_pixel(allocation_iterations);

        // exit early if necessary
        if (isCancelled()) return null;

        publishProgress();

        // get result ...
        int iterations[] = new int[canvas_height * canvas_width];
        allocation_iterations.copyTo(iterations);

        // exit early if necessary
        if (isCancelled()) return null;

        // ... and draw
        m.canvas.plotIterations(canvas_id, iterations);

        return null;
    }
}
