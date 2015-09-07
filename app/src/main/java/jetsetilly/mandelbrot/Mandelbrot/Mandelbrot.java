package jetsetilly.mandelbrot.Mandelbrot;

import android.content.Context;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.Log;

import jetsetilly.mandelbrot.MainActivity;

public class Mandelbrot {
    final static public String DBG_TAG = "mandelbrot";

    public enum RenderMode {TOP_DOWN, CENTRE, MIN_TO_MAX}

    private Context context;
    private MandelbrotSettings mandelbrot_settings = MandelbrotSettings.getInstance();

    private MandelbrotThread render_thr;
    public boolean render_completed = false;

    private MandelbrotCanvas canvas;
    private double pixel_scale;
    private double fractal_ratio;

    /* no_render_area is used to define that area of the canvas
    that do not need to be rendered again because those pixels (hopefully)
    already appear on the canvas
     */
    private Rect no_render_area;

    /* ui related stuff */
    private final int DEF_NUM_PASSES = 2;
    private final int DEF_UPDATE_FREQ = 1;

    public int num_passes = DEF_NUM_PASSES; // in lines
    public int canvas_update_frequency = DEF_UPDATE_FREQ; // in lines

    // is this render a rescaling render (ie. has the zoom level changed)
    // we use this so that progress view is shown immediately
    private boolean rescaling_render;

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
        pixel_scale = (mandelbrot_settings.real_right - mandelbrot_settings.real_left) / canvas.getCanvasWidth();
        fractal_ratio = (mandelbrot_settings.real_right - mandelbrot_settings.real_left) / (mandelbrot_settings.imaginary_upper -  mandelbrot_settings.imaginary_lower);

        Log.d(DBG_TAG, this.toString());
    }

    public void correctMandelbrotRange()
    {
        double  padding;

        fractal_ratio = (mandelbrot_settings.real_right - mandelbrot_settings.real_left) / (mandelbrot_settings.imaginary_upper -  mandelbrot_settings.imaginary_lower);

        // correct range according to canvas dimensions
        if (fractal_ratio > canvas.getCanvasRatio()) {
            // canvas is taller than fractal - expand fractal vertically
            padding = ((mandelbrot_settings.real_right - mandelbrot_settings.real_left) / canvas.getCanvasRatio()) - (mandelbrot_settings.imaginary_upper -  mandelbrot_settings.imaginary_lower);
            mandelbrot_settings.imaginary_upper += padding / 2;
            mandelbrot_settings.imaginary_lower -= padding / 2;
        } else if (fractal_ratio < canvas.getCanvasRatio()) {
            // canvas is wider than fractal - expand fractal horizontally
            padding = (canvas.getCanvasRatio() * (mandelbrot_settings.imaginary_upper -  mandelbrot_settings.imaginary_lower)) - (mandelbrot_settings.real_right - mandelbrot_settings.real_left);
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
            mandelbrot_settings.max_iterations *= 1 + (zoom_factor / 2);
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
        // we do not call ProgressView.unsetBusy() here because it plays havoc
        // with the way we handle the invisibility setting of the ProgressView

        if ( render_thr == null ) {
            return;
        }

        render_thr.cancel(true);
        render_thr = null;
    }

    public void startRender(double offset_x, double offset_y, double zoom_factor) {
        stopRender();

        scrollAndZoom(offset_x, offset_y, zoom_factor);

        // initialise no_render_area
        no_render_area = new Rect(0, 0, canvas.getCanvasWidth(), canvas.getCanvasHeight());

        // make sure render mode etc. is set correctly
        if (zoom_factor == 0) {
            rescaling_render = false;
        } else {
            rescaling_render = true;
        }

        num_passes = DEF_NUM_PASSES;
        canvas_update_frequency = DEF_UPDATE_FREQ;

        if (zoom_factor == 0 && render_completed) {
            /* define no_render_area more accurately */
            if (offset_x < 0) {
                no_render_area.right = (int) -offset_x;
            } else if (offset_x > 0) {
                no_render_area.left = canvas.getCanvasWidth() - (int) offset_x;
            }

            if (offset_y < 0) {
                no_render_area.top = (int) -offset_y;
            } else if (offset_y > 0) {
                no_render_area.bottom = canvas.getCanvasHeight() - (int) offset_y;
            }
        }

        calculatePixelScale();

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

        private int doIterations(MandelbrotPoint p, int max_iterations) {
            int i;

            for (i = Math.abs(p.iteration); i <= max_iterations; ++ i) {
                p.B = 2.0 * p.A * p.B + p.y;
                p.A = p.U - p.V + p.x;
                p.U = p.A * p.A;
                p.V = p.B * p.B;

                if (p.U + p.V > mandelbrot_settings.bailout_value) {
                    return p.iteration = i;
                }
            }

            if (i < mandelbrot_settings.max_iterations)
                return p.iteration = -i;

            return p.iteration = 0;
        }

        private int doIterations(double x, double y) {
            return doIterations(x, y, mandelbrot_settings.max_iterations);
        }

        private int doIterations(double x, double y, int max_iterations) {
            double U, V, A, B;

            U = (A = x) * A;
            V = (B = y) * B;

            int i;

            for (i = 1; i <= max_iterations; ++ i) {
                B = 2.0 * A * B + y;
                A = U - V + x;
                U = A * A;
                V = B * B;

                if (U + V > mandelbrot_settings.bailout_value) {
                    return i;
                }
            }

            return 0;
        }

        @Override
        protected Integer doInBackground(Void... v) {
            int cx, cy, cyb;
            double mx, my, myb;

            render_completed = false;
            canvas.startDraw(mandelbrot_settings.render_mode);

            switch (mandelbrot_settings.render_mode) {
                case MIN_TO_MAX:
                    int[][] cache = new int[canvas.getCanvasHeight()][canvas.getCanvasWidth()];

                    for (cy = 0; cy < canvas.getCanvasHeight(); ++ cy) {
                        for (cx = 0; cx < canvas.getCanvasWidth(); ++ cx) {
                            cache[cy][cx] = 0;
                        }
                    }

                    // TODO: take into account no_render_area instead of working with entire canvas height and width
                    for (int max_iteration = 1; max_iteration <= mandelbrot_settings.max_iterations; ++ max_iteration) {
                        my = mandelbrot_settings.imaginary_lower;
                        for (cy = 0; cy < canvas.getCanvasHeight(); ++ cy, my += pixel_scale) {
                            mx = mandelbrot_settings.real_left;
                            for (cx = 0; cx < canvas.getCanvasWidth(); ++ cx, mx += pixel_scale) {
                                if (cache[cy][cx] == 0) {
                                    cache[cy][cx] = doIterations(mx, my, max_iteration);
                                    if (cache[cy][cx] != 0 || max_iteration == mandelbrot_settings.max_iterations) {
                                        canvas.drawPoint(cx, cy, cache[cy][cx]);
                                    }
                                }
                            }

                            // exit early if necessary
                            if (isCancelled()) return cy;

                            publishProgress(1);
                        }
                    }

                    break;

                case TOP_DOWN:
                    /* TODO: rewrite TOP_DOWN so that it uses ignore_x_start/end and canvas_imag_start_end instead of canvas_height/width directly */
                    for (int pass = 0; pass < num_passes; ++ pass) {
                        my = mandelbrot_settings.imaginary_lower + (pixel_scale * pass);
                        for (cy = pass; cy < canvas.getCanvasHeight(); cy += num_passes, my += (pixel_scale * num_passes)) {

                            mx = mandelbrot_settings.real_left;
                            for (cx = 0; cx < canvas.getCanvasWidth(); ++ cx, mx += pixel_scale) {
                                canvas.drawPoint(cx, cy, doIterations(mx, my));
                            }

                            // exit early if necessary
                            if (isCancelled()) return cy;

                            publishProgress(pass);
                        }
                    }
                    break;

                case CENTRE:
                    int half_height = canvas.getCanvasHeight() / 2;

                    for (int pass = 0; pass < num_passes; ++ pass) {
                        my = mandelbrot_settings.imaginary_lower + ((half_height + pass) * pixel_scale);
                        myb = mandelbrot_settings.imaginary_lower + ((half_height - num_passes + pass) * pixel_scale);
                        for (cy = pass, cyb = num_passes - pass; cy < half_height; cy += num_passes, cyb += num_passes, my += (pixel_scale * num_passes), myb -= (pixel_scale * num_passes)) {
                            int this_line_start;
                            int this_line_end;
                            int y_line;

                            // bottom half of image
                            y_line = half_height + cy;
                            mx = mandelbrot_settings.real_left;
                            if (y_line > no_render_area.top && y_line < no_render_area.bottom) {
                                mx += (pixel_scale * no_render_area.left);
                                this_line_start = no_render_area.left;
                                this_line_end = no_render_area.right;
                            } else {
                                this_line_start = 0;
                                this_line_end = canvas.getCanvasWidth();
                            }

                            for (cx = this_line_start; cx < this_line_end; ++ cx, mx += pixel_scale) {
                                canvas.drawPoint(cx, y_line, doIterations(mx, my));
                            }

                            // top half of image
                            y_line = half_height - cyb;
                            mx = mandelbrot_settings.real_left;
                            if (y_line > no_render_area.top && y_line < no_render_area.bottom) {
                                mx += (pixel_scale * no_render_area.left);
                                this_line_start = no_render_area.left;
                                this_line_end = no_render_area.right;
                            } else {
                                this_line_start = 0;
                                this_line_end = canvas.getCanvasWidth();
                            }

                            for (cx = this_line_start; cx < this_line_end; ++ cx, mx += pixel_scale) {
                                canvas.drawPoint(cx, y_line, doIterations(mx, myb));
                            }

                            // exit early if necessary
                            if (isCancelled()) return cy;

                            publishProgress(pass);
                        }
                    }
                    break;
            }

            canvas.endDraw();

            return 0;
        }

        @Override
        protected void onProgressUpdate(Integer... pass) {
            MainActivity.progress.setBusy(pass[0], num_passes, rescaling_render);
            canvas.update();
        }

        @Override
        protected void onPostExecute(Integer result) {
            canvas.update();
            MainActivity.progress.unsetBusy();
            MainActivity.render_canvas.completeRender();

            render_completed = result >= 0;
        }

        @Override
        protected void onCancelled() {
            onPostExecute(-1);
        }
    }

    /* end of threading */
}
