package jetsetilly.mandelbrot.Mandelbrot;

import android.os.AsyncTask;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;

class MandelbrotThread extends AsyncTask<Void, Integer, Integer> {
    /* from the android documentation:

    AsyncTasks should ideally be used for short operations (a few seconds at the most.) If you
    need to keep threads running for long periods of time, it is highly recommended you use the
    various APIs provided by the java.util.concurrent package such as Executor,
    ThreadPoolExecutor and FutureTask.
     */

    final static public String DBG_TAG = "render thread";

    private final MandelbrotSettings mandelbrot_settings = MandelbrotSettings.getInstance();
    private final Mandelbrot m;
    private final int target_iteration;

    /* target_iterations of > 0 will result in a one shot thread for one iteration value */

    public MandelbrotThread(Mandelbrot context, int target_iteration) {
        this.m = context;
        this.target_iteration = target_iteration;
    }

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

    private int doIterations(double x, double y, int target_iteration) {
        double U, V, A, B;

        U = (A = x) * A;
        V = (B = y) * B;

        int i;

        for (i = 1; i < target_iteration; ++ i) {
            B = 2.0 * A * B + y;
            A = U - V + x;
            U = A * A;
            V = B * B;

            if (U + V > mandelbrot_settings.bailout_value) {
                // we've not reached the target iteration so return a negative
                // number to indicate that
                return -1;
            }
        }

        // i is now equal to target_iteration

        B = 2.0 * A * B + y;
        A = U - V + x;
        U = A * A;
        V = B * B;

        if (U + V > mandelbrot_settings.bailout_value) {
            return i;
        }

        if (i == mandelbrot_settings.max_iterations) {
            return 0;
        } else {
            return -1;
        }
    }

    private int doIterations(double x, double y) {
        if (target_iteration > 0) {
            return doIterations(x, y, target_iteration);
        }

        double U, V, A, B;

        U = (A = x) * A;
        V = (B = y) * B;

        int i;

        for (i = 1; i <= mandelbrot_settings.max_iterations; ++ i) {
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

        m.render_completed = false;
        m.canvas.startDraw(mandelbrot_settings.render_mode);

        switch (mandelbrot_settings.render_mode) {
            case TOP_DOWN:
                    /* TODO: rewrite TOP_DOWN so that it uses ignore_x_start/end and canvas_imag_start_end instead of canvas_height/width directly */
                for (int pass = 0; pass < m. num_passes; ++ pass) {
                    my = mandelbrot_settings.imaginary_lower + (m.pixel_scale * pass);
                    for (cy = pass; cy < m.canvas.getHeight(); cy += m.num_passes, my += (m.pixel_scale * m.num_passes)) {

                        mx = mandelbrot_settings.real_left;
                        for (cx = 0; cx < m.canvas.getWidth(); ++ cx, mx += m.pixel_scale) {
                            m.canvas.drawPoint(cx, cy, doIterations(mx, my));
                        }

                        // exit early if necessary
                        if (isCancelled()) return cy;

                        publishProgress(pass);
                    }
                }
                break;

            case CENTRE:
                int half_height = m.canvas.getHeight() / 2;

                for (int pass = 0; pass < m.num_passes; ++ pass) {
                    my = mandelbrot_settings.imaginary_lower + ((half_height + pass) * m.pixel_scale);
                    myb = mandelbrot_settings.imaginary_lower + ((half_height - m.num_passes + pass) * m.pixel_scale);
                    for (cy = pass, cyb = m.num_passes - pass; cy < half_height; cy += m.num_passes, cyb += m.num_passes, my += (m.pixel_scale * m.num_passes), myb -= (m.pixel_scale * m.num_passes)) {
                        int this_line_start;
                        int this_line_end;
                        int y_line;

                        // bottom half of image
                        y_line = half_height + cy;
                        mx = mandelbrot_settings.real_left;
                        if (y_line > m.no_render_area.top && y_line < m.no_render_area.bottom) {
                            mx += (m.pixel_scale * m.no_render_area.left);
                            this_line_start = m.no_render_area.left;
                            this_line_end = m.no_render_area.right;
                        } else {
                            this_line_start = 0;
                            this_line_end = m.canvas.getWidth();
                        }

                        for (cx = this_line_start; cx < this_line_end; ++ cx, mx += m.pixel_scale) {
                            int i = doIterations(mx, my);
                            if (i >= 0) {
                                m.canvas.drawPoint(cx, y_line, i);
                            }
                        }

                        // top half of image
                        y_line = half_height - cyb;
                        mx = mandelbrot_settings.real_left;
                        if (y_line > m.no_render_area.top && y_line < m.no_render_area.bottom) {
                            mx += (m.pixel_scale * m.no_render_area.left);
                            this_line_start = m.no_render_area.left;
                            this_line_end = m.no_render_area.right;
                        } else {
                            this_line_start = 0;
                            this_line_end = m.canvas.getWidth();
                        }

                        for (cx = this_line_start; cx < this_line_end; ++ cx, mx += m.pixel_scale) {
                            int i = doIterations(mx, myb);
                            if (i >= 0) {
                                m.canvas.drawPoint(cx, y_line, i);
                            }
                        }

                        // exit early if necessary
                        if (isCancelled()) return cy;

                        publishProgress(pass);
                    }
                }
                break;
        }

        return 0;
    }

    @Override
    protected void onProgressUpdate(Integer... pass) {
        MainActivity.progress.kick(pass[0], m.num_passes, m.rescaling_render);
        m.canvas.update();
    }

    protected void onPreExecute() {
        MainActivity.progress.register();
    }

    @Override
    protected void onPostExecute(Integer result) {
        m.canvas.endDraw();
        MainActivity.progress.unregister();

        m.render_completed = result >= 0;
    }

    @Override
    protected void onCancelled() {
        onPostExecute(-1);
    }
}
