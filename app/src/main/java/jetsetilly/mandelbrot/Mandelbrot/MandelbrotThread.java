package jetsetilly.mandelbrot.Mandelbrot;

import android.os.AsyncTask;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;

class MandelbrotThread extends AsyncTask<Void, Integer, Void> {
    /* from the android documentation:

    AsyncTasks should ideally be used for short operations (a few seconds at the most.) If you
    need to keep threads running for long periods of time, it is highly recommended you use the
    various APIs provided by the java.util.concurrent package such as Executor,
    ThreadPoolExecutor and FutureTask.
     */

    final static public String DBG_TAG = "render thread";

    private final MandelbrotSettings mandelbrot_settings = MandelbrotSettings.getInstance();
    private final Mandelbrot m;

    public MandelbrotThread(Mandelbrot context) {
        this.m = context;
    }

    private int doIterations(double x, double y) {
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
    protected Void doInBackground(Void... v) {
        int cx, cy, cyb;
        double mx, my, myb;

        m.render_completed = false;

        switch (mandelbrot_settings.render_mode) {
            case TOP_DOWN:
            /* TODO: rewrite TOP_DOWN so that it uses ignore_x_start/end and canvas_imag_start_end instead of canvas_height/width directly */
                for (int pass = 0; pass < m.num_passes; ++pass) {
                    my = mandelbrot_settings.imaginary_lower + (m.pixel_scale * pass);
                    for (cy = pass; cy < m.canvas.getHeight(); cy += m.num_passes, my += (m.pixel_scale * m.num_passes)) {

                        mx = mandelbrot_settings.real_left;
                        for (cx = 0; cx < m.canvas.getWidth(); ++cx, mx += m.pixel_scale) {
                            m.canvas.drawPoint(cx, cy, doIterations(mx, my));
                        }

                        publishProgress(pass);

                        // exit early if necessary
                        if (isCancelled()) return null;
                    }
                }
                break;

            case CENTRE:
                int half_height = m.canvas.getHeight() / 2;

                for (int pass = 0; pass < m.num_passes; ++pass) {
                    my = mandelbrot_settings.imaginary_lower + ((half_height + pass) * m.pixel_scale);
                    myb = mandelbrot_settings.imaginary_lower + ((half_height - m.num_passes + pass) * m.pixel_scale);
                    for (cy = pass, cyb = m.num_passes - pass; cy < half_height; cy += m.num_passes, cyb += m.num_passes, my += (m.pixel_scale * m.num_passes), myb -= (m.pixel_scale * m.num_passes)) {
                        int this_line_start;
                        int this_line_end;
                        int y_line;

                        // bottom half of image
                        y_line = half_height + cy;
                        mx = mandelbrot_settings.real_left;
                        if (y_line > m.protected_render_area.top && y_line < m.protected_render_area.bottom) {
                            mx += (m.pixel_scale * m.protected_render_area.left);
                            this_line_start = m.protected_render_area.left;
                            this_line_end = m.protected_render_area.right;
                        } else {
                            this_line_start = 0;
                            this_line_end = m.canvas.getWidth();
                        }

                        for (cx = this_line_start; cx < this_line_end; ++cx, mx += m.pixel_scale) {
                            int i = doIterations(mx, my);
                            if (i >= 0) {
                                m.canvas.drawPoint(cx, y_line, i);
                            }
                        }

                        // top half of image
                        y_line = half_height - cyb;
                        mx = mandelbrot_settings.real_left;
                        if (y_line > m.protected_render_area.top && y_line < m.protected_render_area.bottom) {
                            mx += (m.pixel_scale * m.protected_render_area.left);
                            this_line_start = m.protected_render_area.left;
                            this_line_end = m.protected_render_area.right;
                        } else {
                            this_line_start = 0;
                            this_line_end = m.canvas.getWidth();
                        }

                        for (cx = this_line_start; cx < this_line_end; ++cx, mx += m.pixel_scale) {
                            int i = doIterations(mx, myb);
                            if (i >= 0) {
                                m.canvas.drawPoint(cx, y_line, i);
                            }
                        }

                        publishProgress(pass);

                        // exit early if necessary
                        if (isCancelled()) return null;
                    }
                }
                break;
        }

        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... pass) {
        MainActivity.progress.kick(pass[0], m.num_passes, m.rescaling_render);
        m.canvas.update();
    }

    protected void onPreExecute() {
        MainActivity.progress.register();
        m.canvas.startDraw(mandelbrot_settings.render_mode);
    }

    @Override
    protected void onPostExecute(Void v) {
        MainActivity.progress.unregister();
        m.canvas.endDraw();
        m.render_completed = true;
    }

    @Override
    protected void onCancelled() {
        MainActivity.progress.unregister();
        // not called m.canvas.endDraw()
        m.render_completed = false;
    }
}
