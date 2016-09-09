package jetsetilly.mandelbrot.Mandelbrot;

import android.support.annotation.WorkerThread;

public class MandelbrotThread_dalvik extends MandelbrotThread {
    final static public String DBG_TAG = "mandelbrot thread (dalvik)";

    public MandelbrotThread_dalvik(Mandelbrot mandelbrot, MandelbrotCanvas canvas) {
        super(mandelbrot, canvas);
    }

    private int doIterations(double x, double y) {
        double U, V, A, B;

        U = (A = x) * A;
        V = (B = y) * B;

        int i;

        for (i = 1; i <= mandelbrot_coordinates.max_iterations; ++ i) {
            B = 2.0 * A * B + y;
            A = U - V + x;
            U = A * A;
            V = B * B;

            if (U + V > mandelbrot_coordinates.bailout_value) {
                return i;
            }
        }

        return 0;
    }

    @Override
    @WorkerThread
    protected Void doInBackground(Void... v) {
        int cx, cy, cyb;
        double mx, my, myb;

        // protected area accounting
        int this_line_start;
        int this_line_end;
        int y_line;

        super.doInBackground();

        switch (settings.render_mode) {
            case Mandelbrot.RenderMode.SOFTWARE_TOP_DOWN:
                for (int pass = 0; pass < settings.num_passes; ++pass) {
                    my = mandelbrot_coordinates.imaginary_lower + (m.pixel_scale * pass);

                    for (cy = pass; cy < m.canvas_height; cy += settings.num_passes, my += (m.pixel_scale * settings.num_passes)) {
                        y_line = cy;

                        // PROTECTED AREA ACCOUNTING
                        mx = mandelbrot_coordinates.real_left;
                        if (y_line >= m.render_area.top && y_line <= m.render_area.bottom) {
                            this_line_start = 0;
                            this_line_end = m.canvas_width;
                        } else {
                            mx += (m.pixel_scale * m.render_area.left);
                            this_line_start = m.render_area.left;
                            this_line_end = m.render_area.right;
                        }

                        // END OF PROTECTED AREA ACCOUNTING
                        for (cx = this_line_start; cx < this_line_end; ++cx, mx += m.pixel_scale) {
                            c.plotIteration(canvas_id, cx, cy, doIterations(mx, my));
                        }

                        publishProgress();

                        // exit early if necessary
                        if (isCancelled()) return null;
                    }
                }
                break;

            case Mandelbrot.RenderMode.SOFTWARE_CENTRE:
                int num_iterations;
                int half_height = m.canvas_height / 2;

                for (int pass = 0; pass < settings.num_passes; ++pass) {
                    my = mandelbrot_coordinates.imaginary_lower + ((half_height + pass) * m.pixel_scale);
                    myb = mandelbrot_coordinates.imaginary_lower + ((half_height - settings.num_passes + pass) * m.pixel_scale);
                    for (cy = pass, cyb = settings.num_passes - pass; cy < half_height; cy += settings.num_passes, cyb += settings.num_passes, my += (m.pixel_scale * settings.num_passes), myb -= (m.pixel_scale * settings.num_passes)) {

                        // bottom half of image
                        y_line = half_height + cy;

                        // PROTECTED AREA ACCOUNTING
                        mx = mandelbrot_coordinates.real_left;
                        if (y_line >= m.render_area.top && y_line <= m.render_area.bottom) {
                            this_line_start = 0;
                            this_line_end = m.canvas_width;
                        } else {
                            mx += (m.pixel_scale * m.render_area.left);
                            this_line_start = m.render_area.left;
                            this_line_end = m.render_area.right;
                        }
                        // END OF PROTECTED AREA ACCOUNTING

                        for (cx = this_line_start; cx < this_line_end; ++cx, mx += m.pixel_scale) {
                            num_iterations = doIterations(mx, my);
                            c.plotIteration(canvas_id, cx, y_line, num_iterations);
                        }

                        // top half of image
                        y_line = half_height - cyb;

                        // PROTECTED AREA ACCOUNTING
                        mx = mandelbrot_coordinates.real_left;
                        if (y_line >= m.render_area.top && y_line <= m.render_area.bottom) {
                            this_line_start = 0;
                            this_line_end = m.canvas_width;
                        } else {
                            mx += (m.pixel_scale * m.render_area.left);
                            this_line_start = m.render_area.left;
                            this_line_end = m.render_area.right;
                        }
                        // END OF PROTECTED AREA ACCOUNTING

                        for (cx = this_line_start; cx < this_line_end; ++cx, mx += m.pixel_scale) {
                            num_iterations = doIterations(mx, myb);
                            c.plotIteration(canvas_id, cx, y_line, num_iterations);
                        }

                        publishProgress();

                        // exit early if necessary
                        if (isCancelled()) return null;
                    }
                }
                break;
        }

        return null;
    }
}
