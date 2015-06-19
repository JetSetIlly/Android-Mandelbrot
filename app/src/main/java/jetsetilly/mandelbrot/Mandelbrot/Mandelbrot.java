package jetsetilly.mandelbrot.Mandelbrot;

import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.Log;

import jetsetilly.mandelbrot.MainActivity;


public class Mandelbrot {
    final static public String DBG_TAG = "mandelbrot";

    private Settings mandelbrot_settings = Settings.getInstance();

    private MandelbrotThread render_thr;
    public boolean render_completed = false;

    private MandelbrotCanvas context;
    private int canvas_width;
    private int canvas_height;
    private double canvas_ratio;
    private double pixel_scale;
    private double fractal_ratio;

    /* no_render_area is used to define that area of the canvas
    that do not need to be rendered again because those pixels (hopefully)
    already appear on the canvas
     */
    private Rect no_render_area;

    /* ui related stuff */
    private final int DEF_NUM_PASSES = 4;
    private final int DEF_UPDATE_FREQ = 1;

    private enum RenderMode {TOP_DOWN, CENTRE}
    private RenderMode render_mode;
    public int num_passes = DEF_NUM_PASSES; // in lines
    public int canvas_update_frequency = DEF_UPDATE_FREQ; // in lines

    /* render queue */
    private Queue queue;


    public Mandelbrot(MandelbrotCanvas context) {
        this.context = context;
        this.render_thr = null;

        canvas_width = context.getCanvasWidth();
        canvas_height = context.getCanvasHeight();
        canvas_ratio = (double) canvas_width / (double) canvas_height;

        queue = new Queue(context);
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
        pixel_scale = (mandelbrot_settings.real_right - mandelbrot_settings.real_left) / canvas_width;
        fractal_ratio = (mandelbrot_settings.real_right - mandelbrot_settings.real_left) / (mandelbrot_settings.imaginary_upper -  mandelbrot_settings.imaginary_lower);
    }

    public void scrollBy(int pixel_x, int pixel_y) {
        mandelbrot_settings.real_left += pixel_x * pixel_scale;
        mandelbrot_settings.real_right += pixel_x * pixel_scale;
        mandelbrot_settings.imaginary_upper += pixel_y * pixel_scale;
        mandelbrot_settings.imaginary_lower += pixel_y * pixel_scale;
    }

    private void zoomByPixels(int pixels) {
        double fractal_width = mandelbrot_settings.real_right - mandelbrot_settings.real_left;
        double fractal_height = mandelbrot_settings.imaginary_upper - mandelbrot_settings.imaginary_lower;
        double zoom_factor = pixels / Math.hypot(canvas_height, canvas_width);

        mandelbrot_settings.real_left += zoom_factor * fractal_width;
        mandelbrot_settings.real_right -= zoom_factor * fractal_width;
        mandelbrot_settings.imaginary_upper -= zoom_factor * fractal_height;
        mandelbrot_settings.imaginary_lower += zoom_factor * fractal_height;

        mandelbrot_settings.max_iterations += mandelbrot_settings.max_iterations * zoom_factor / 3;
    }

    public void correctMandelbrotRange()
    {
        double  padding;

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

        Log.d(DBG_TAG, this.toString());
    }

    /* threading */

    public void stopRender() {
        // we do not call ProgressView.unsetBusy() here because it plays havoc
        // with the way we handle the invisibility setting of the ProgressView

        if ( render_thr == null ) {
            return;
        }

        render_thr.cancel(true);
        render_thr = null;
    }

    public void startRender(int offset_x, int offset_y, int zoom) {
        stopRender();

        // initialise no_render_area
        no_render_area = new Rect(0, 0, canvas_width, canvas_height);

        // we do this every time in case settings have changed
        correctMandelbrotRange();

        // make sure render mode etc. is set correctly
        render_mode = RenderMode.CENTRE;
        num_passes = DEF_NUM_PASSES;
        canvas_update_frequency = DEF_UPDATE_FREQ;

        scrollBy(offset_x, offset_y);

        if (zoom != 0) {
            zoomByPixels(zoom);
        } else if (render_completed) {
            /* define no_render_area more accurately */
            if (offset_x < 0) {
                no_render_area.right = -offset_x;
            } else if (offset_x > 0) {
                no_render_area.left = canvas_width - offset_x;
            }

            if (offset_y < 0) {
                no_render_area.top = -offset_y;
            } else if (offset_y > 0) {
                no_render_area.bottom = canvas_height - offset_y;
            }
        }

        calculatePixelScale();
        Log.d(DBG_TAG, this.toString());

        render_thr = new MandelbrotThread();
        render_thr.execute();
    }

    class MandelbrotThread extends AsyncTask<Void, Integer, Integer> {
        /* from the android documentation:

        AsyncTasks should ideally be used for short operations (a few seconds at the most.) If you
        need to keep threads running for long periods of time, it is highly recommended you use the
        various APIs provided by the java.util.concurrent package such as Executor,
        ThreadPoolExecutor and FutureTask.
         */

        final static public String DBG_TAG = "render thread";

        private int doIterations(double x, double y) {
            double A, B, U, V;
            int iteration;

            U = (A = x) * A;
            V = (B = y) * B;

            for (iteration = 1; iteration < mandelbrot_settings.max_iterations; ++ iteration) {
                B = 2.0 * A * B + y;
                A = U - V + x;
                U = A * A;
                V = B * B;

                if (U + V > mandelbrot_settings.bailout_value) {
                    return iteration;
                }
            }

            return 0;
        }

        protected void checkUpdate(int pass, int cy) {
            if (((pass + 1) * cy) % canvas_update_frequency == 0) {
                publishProgress(pass);
            }
        }

        @Override
        protected Integer doInBackground(Void... v) {
            int cx, cy, cyb;
            double x, y, yb;

            render_completed = false;
            queue.resetQueues();

            switch (render_mode) {
                case TOP_DOWN:
                    /* TODO: rewrite TOP_DOWN so that it uses ignore_x_start/end and canvas_imag_start_end instead of canvas_height/width directly */
                    for (int pass = 0; pass < num_passes; ++ pass) {
                        y = mandelbrot_settings.imaginary_lower + (pixel_scale * pass);
                        for (cy = pass; cy < canvas_height; cy += num_passes, y += (pixel_scale * num_passes)) {

                            x = mandelbrot_settings.real_left;
                            for (cx = 0; cx < canvas_width; ++ cx, x += pixel_scale) {
                                queue.pushDraw(cx, cy, doIterations(x, y));
                            }

                            // exit early if necessary
                            if (isCancelled()) return cy;

                            // update if necessary
                            checkUpdate(pass, cy);
                        }
                    }
                    break;

                case CENTRE:
                    int half_height = canvas_height / 2;

                    for (int pass = 0; pass < num_passes; ++ pass) {
                        y = mandelbrot_settings.imaginary_lower + ((half_height + pass) * pixel_scale);
                        yb = mandelbrot_settings.imaginary_lower + ((half_height - num_passes + pass) * pixel_scale);
                        for (cy = pass, cyb = num_passes - pass; cy < half_height; cy += num_passes, cyb += num_passes, y += (pixel_scale * num_passes), yb -= (pixel_scale * num_passes)) {
                            int this_line_start;
                            int this_line_end;
                            int y_line;

                            // bottom half of image
                            y_line = half_height + cy;
                            x = mandelbrot_settings.real_left;
                            if (y_line > no_render_area.top && y_line < no_render_area.bottom) {
                                x += (pixel_scale * no_render_area.left);
                                this_line_start = no_render_area.left;
                                this_line_end = no_render_area.right;
                            } else {
                                this_line_start = 0;
                                this_line_end = canvas_width;
                            }

                            for (cx = this_line_start; cx < this_line_end; ++ cx, x += pixel_scale) {
                                queue.pushDraw(cx, y_line, doIterations(x, y));
                            }

                            // top half of image
                            y_line = half_height - cyb;
                            x = mandelbrot_settings.real_left;
                            if (y_line > no_render_area.top && y_line < no_render_area.bottom) {
                                x += (pixel_scale * no_render_area.left);
                                this_line_start = no_render_area.left;
                                this_line_end = no_render_area.right;
                            } else {
                                this_line_start = 0;
                                this_line_end = canvas_width;
                            }

                            for (cx = this_line_start; cx < this_line_end; ++ cx, x += pixel_scale) {
                                queue.pushDraw(cx, y_line, doIterations(x, yb));
                            }

                            // exit early if necessary
                            if (isCancelled()) return cy;

                            // update if necessary
                            checkUpdate(pass, cy);
                        }
                    }
                    break;
            }

            queue.finaliseDraw();

            return 0;
        }

        @Override
        protected void onProgressUpdate(Integer... pass) {
            MainActivity.progress.setBusy(pass[0], num_passes);
            context.update();
        }

        @Override
        protected void onPostExecute(Integer result) {
            context.update();
            render_completed = true;
            MainActivity.progress.unsetBusy();
            MainActivity.render_canvas.completeRender();
        }
    }

    /* end of threading */
}
