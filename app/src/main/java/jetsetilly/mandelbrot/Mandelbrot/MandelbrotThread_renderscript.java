package jetsetilly.mandelbrot.Mandelbrot;

import android.support.annotation.WorkerThread;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.ScriptC_mandelbrot;
import jetsetilly.tools.MyDebug;

public class MandelbrotThread_renderscript extends MandelbrotThread {
    final static public String DBG_TAG = "mandelbrot thread (renderscript)";

    public MandelbrotThread_renderscript(Mandelbrot mandelbrot, MandelbrotCanvas canvas) {
        super(mandelbrot, canvas);
    }
    private ScriptC_mandelbrot script;

    @Override
    @WorkerThread
    protected Void doInBackground(Void... params) {
        super.doInBackground(params);

        Allocation allocation_iterations = Allocation.createSized(MainActivity.render_script,
                Element.I32(MainActivity.render_script), m.canvas_height * m.canvas_width,
                Allocation.USAGE_SCRIPT);

        script = new ScriptC_mandelbrot(MainActivity.render_script);

        // set variables/arguments for this render
        script.set_canvas_height(m.canvas_height);
        script.set_canvas_width(m.canvas_width);
        script.set_max_iterations(mandelbrot_coordinates.max_iterations);
        script.set_null_iteration(Mandelbrot.NULL_ITERATIONS);
        script.set_bailout_value(mandelbrot_coordinates.bailout_value);
        script.set_imaginary_lower(mandelbrot_coordinates.imaginary_lower);
        script.set_imaginary_upper(mandelbrot_coordinates.imaginary_upper);
        script.set_real_left(mandelbrot_coordinates.real_left);
        script.set_real_right(mandelbrot_coordinates.real_right);
        script.set_pixel_scale(m.pixel_scale);
        script.set_render_left(m.render_area.left);
        script.set_render_right(m.render_area.right);
        script.set_render_top(m.render_area.top);
        script.set_render_bottom(m.render_area.bottom);
        script.set_cancel_flag(false);

        if (isCancelled()) return null;

        // launch script
        script.forEach_pixel(allocation_iterations);

        if (isCancelled()) return null;

        publishProgress();

        // exit early if necessary
        if (isCancelled()) return null;

        // get result ...
        int iterations[] = new int[m.canvas_height * m.canvas_width];
        allocation_iterations.copyTo(iterations);

        if (isCancelled()) return null;

        // ... and draw
        boolean complete_plot;
        complete_plot = m.render_area.left == 0 && m.render_area.right == m.canvas_width && m.render_area.top == 0 && m.render_area.bottom == m.canvas_height;
        c.plotIterations(canvas_id, iterations, complete_plot);

        return null;
    }
}
