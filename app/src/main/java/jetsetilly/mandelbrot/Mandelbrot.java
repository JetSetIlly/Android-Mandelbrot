package jetsetilly.mandelbrot;

import android.os.AsyncTask;
import android.util.Log;


public class Mandelbrot {
    final static public String DBG_TAG = "mandelbrot";

    private SettingsMandelbrot mandelbrot_settings = SettingsMandelbrot.getInstance();

    private MandelbrotThread render_thr;
    public boolean render_completed = false;

    private MandelbrotCanvas context;
    private int canvas_width;
    private int canvas_height;
    private double canvas_ratio;
    private double pixel_scale;
    private double fractal_ratio;

    /* ui related stuff */
    private final int PASSES_REDRAW = 1;
    private final int PASSES_NEWDRAW = 4;
    private final int UPDATE_REDRAW = 200;
    private final int UPDATE_NEWDRAW = 1;

    private enum RenderMode {TOP_DOWN, CENTRE}
    private RenderMode render_mode;
    public int num_passes = PASSES_NEWDRAW; // in lines
    public int canvas_update_frequency = UPDATE_NEWDRAW; // in lines

    /* render cache useful when moving the fractal around without zooming*/
    private MandelbrotCache cache;

    /* render queue */
    private MandelbrotQueue queue;


    public Mandelbrot(MandelbrotCanvas context) {
        this.context = context;
        this.render_thr = null;

        canvas_width = context.getCanvasWidth();
        canvas_height = context.getCanvasHeight();
        canvas_ratio = (double) canvas_width / (double) canvas_height;

        cache = new MandelbrotCache(canvas_width, canvas_height);
        queue = new MandelbrotQueue(context);
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

        // commit the changes to the cache otherwise the image and cache will be out of sync
        if (!render_completed) {
            cache.resetCache();
        }
    }

    public void startRender(int offset_x, int offset_y, int zoom, boolean force_redraw, boolean no_cache) {
        stopRender();

        // we do this every time in case settings have changed
        correctMandelbrotRange();

        // only allow the force_redraw flag to be obeyed if the last
        // render event completed
        queue.force_redraw = render_completed && force_redraw;

        if (queue.force_redraw) {
            // quicker to draw from the top if data has already been calculated
            render_mode = RenderMode.TOP_DOWN;
            num_passes = PASSES_REDRAW;
            canvas_update_frequency = UPDATE_REDRAW;
        } else {
            // more visually pleasing to draw from the centre
            render_mode = RenderMode.CENTRE;
            num_passes = PASSES_NEWDRAW;
            canvas_update_frequency = UPDATE_NEWDRAW;
        }

        scrollBy(offset_x, offset_y);

        if (zoom != 0) {
            zoomByPixels(zoom);
            no_cache = true;
        }

        if (no_cache) {
            cache.resetCache();
        } else {
            cache.setOffset(offset_x, offset_y);
        }

        calculatePixelScale();
        Log.d(DBG_TAG, this.toString());

        render_thr = new MandelbrotThread();
        render_thr.execute();
    }

    class MandelbrotThread extends AsyncTask<Void, Void, Integer> {
        /* from the android documentation:

        AsyncTasks should ideally be used for short operations (a few seconds at the most.) If you
        need to keep threads running for long periods of time, it is highly recommended you use the
        various APIs provided by the java.util.concurrent package such as Executor,
        ThreadPoolExecutor and FutureTask.
         */

        final static public String DBG_TAG = "render thread";
        private int cache_hits = 0;

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

        private int getIterationValue(double x, double y, int cx, int cy) {
            // returns iteration value as a negative number if it was found in the cache
            int iteration;

            if (cache.isInCache(cx, cy)) {
                iteration = cache.readCache(cx, cy);
                if (iteration != MandelbrotCache.CACHE_UNSET ) {
                    cache.writeSwap(cx, cy, iteration);
                    cache_hits ++;
                    return -iteration;
                }
            }

            iteration = doIterations(x, y);
            cache.writeSwap(cx, cy, iteration);

            return iteration;
        }

        protected void checkUpdate(int pass, int cy) {
            if (!queue.force_redraw) {
                if (((pass + 1) * cy) % canvas_update_frequency == 0)
                    publishProgress();
            } else {
                publishProgress();
            }
        }

        @Override
        protected Integer doInBackground(Void... v) {
            int cx, cy, cyb;
            double x, y, yb;
            int iteration;

            double start_time = System.nanoTime();

            render_completed = false;

            queue.resetQueues();
            cache.resetSwap();

            switch (render_mode) {
                case TOP_DOWN:
                    for (int pass = 0; pass < num_passes; ++ pass) {
                        y = mandelbrot_settings.imaginary_lower + (pixel_scale * pass);
                        for (cy = pass; cy < canvas_height; cy += num_passes, y += (pixel_scale * num_passes)) {

                            x = mandelbrot_settings.real_left;
                            for (cx = 0; cx < canvas_width; ++ cx, x += pixel_scale) {
                                iteration = getIterationValue(x, y, cx, cy);
                                queue.pushDraw(cx, cy, iteration);
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
                            x = mandelbrot_settings.real_left;
                            for (cx = 0; cx < canvas_width; ++ cx, x += pixel_scale) {
                                // bottom half of image
                                iteration = getIterationValue(x, y, cx, half_height + cy);
                                queue.pushDraw(cx, half_height + cy, iteration);

                                // top half of image
                                if (cyb <= half_height) {
                                    iteration = getIterationValue(x, yb, cx, half_height - cyb);
                                    queue.pushDraw(cx, half_height - cyb, iteration);
                                }
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
            cache.commitSwap();

            Log.d(DBG_TAG, "Elapsed time: " + (System.nanoTime() - start_time) + "ms");
            Log.d(DBG_TAG, "Cache hits: " + cache_hits);

            return cache_hits;
        }

        @Override
        protected void onProgressUpdate(Void... v) {
            MainActivity.progress.setBusy();
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
