package jetsetilly.mandelbrot.Mandelbrot;

class MandelbrotThread_dalvik extends MandelbrotThread {
    final static public String DBG_TAG = "mandelbrot thread (dalvik)";

    public MandelbrotThread_dalvik(Mandelbrot context) {
        super(context);
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

        int canvas_width = m.canvas.getCanvasWidth();
        int canvas_height = m.canvas.getCanvasHeight();

        // protected area accounting
        int this_line_start;
        int this_line_end;
        int y_line;

        switch (mandelbrot_settings.render_mode) {
            case SOFTWARE_TOP_DOWN:
                for (int pass = 0; pass < mandelbrot_settings.num_passes; ++pass) {
                    my = mandelbrot_settings.imaginary_lower + (m.pixel_scale * pass);

                    for (cy = pass; cy < canvas_height; cy += mandelbrot_settings.num_passes, my += (m.pixel_scale * mandelbrot_settings.num_passes)) {
                        y_line = cy;

                        // PROTECTED AREA ACCOUNTING
                        mx = mandelbrot_settings.real_left;
                        if (y_line >= m.render_area.top && y_line <= m.render_area.bottom) {
                            this_line_start = 0;
                            this_line_end = canvas_width;
                        } else {
                            mx += (m.pixel_scale * m.render_area.left);
                            this_line_start = m.render_area.left;
                            this_line_end = m.render_area.right;
                        }

                        // END OF PROTECTED AREA ACCOUNTING
                        for (cx = this_line_start; cx < this_line_end; ++cx, mx += m.pixel_scale) {
                            m.canvas.plotIteration(canvas_id, cx, cy, doIterations(mx, my));
                        }

                        publishProgress(pass);

                        // exit early if necessary
                        if (isCancelled()) return null;
                    }
                }
                break;

            case SOFTWARE_CENTRE:
                int num_iterations;
                int half_height = canvas_height / 2;

                for (int pass = 0; pass < mandelbrot_settings.num_passes; ++pass) {
                    my = mandelbrot_settings.imaginary_lower + ((half_height + pass) * m.pixel_scale);
                    myb = mandelbrot_settings.imaginary_lower + ((half_height - mandelbrot_settings.num_passes + pass) * m.pixel_scale);
                    for (cy = pass, cyb = mandelbrot_settings.num_passes - pass; cy < half_height; cy += mandelbrot_settings.num_passes, cyb += mandelbrot_settings.num_passes, my += (m.pixel_scale * mandelbrot_settings.num_passes), myb -= (m.pixel_scale * mandelbrot_settings.num_passes)) {

                        // bottom half of image
                        y_line = half_height + cy;

                        // PROTECTED AREA ACCOUNTING
                        mx = mandelbrot_settings.real_left;
                        if (y_line >= m.render_area.top && y_line <= m.render_area.bottom) {
                            this_line_start = 0;
                            this_line_end = canvas_width;
                        } else {
                            mx += (m.pixel_scale * m.render_area.left);
                            this_line_start = m.render_area.left;
                            this_line_end = m.render_area.right;
                        }
                        // END OF PROTECTED AREA ACCOUNTING

                        for (cx = this_line_start; cx < this_line_end; ++cx, mx += m.pixel_scale) {
                            num_iterations = doIterations(mx, my);
                            if (num_iterations >= 0) {
                                m.canvas.plotIteration(canvas_id, cx, y_line, num_iterations);
                            }
                        }

                        // top half of image
                        y_line = half_height - cyb;

                        // PROTECTED AREA ACCOUNTING
                        mx = mandelbrot_settings.real_left;
                        if (y_line >= m.render_area.top && y_line <= m.render_area.bottom) {
                            this_line_start = 0;
                            this_line_end = canvas_width;
                        } else {
                            mx += (m.pixel_scale * m.render_area.left);
                            this_line_start = m.render_area.left;
                            this_line_end = m.render_area.right;
                        }
                        // END OF PROTECTED AREA ACCOUNTING

                        for (cx = this_line_start; cx < this_line_end; ++cx, mx += m.pixel_scale) {
                            num_iterations = doIterations(mx, myb);
                            if (num_iterations >= 0) {
                                m.canvas.plotIteration(canvas_id, cx, y_line, num_iterations);
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
}
