#pragma version(1)
#pragma rs java_package_name(jetsetilly.mandelbrot)
#pragma rs_fp_full

static const char * DBG_TAG = "iteration.rs";

int canvas_height, canvas_width;
int max_iterations;
int null_iteration;
double bailout_value;
double imaginary_lower, imaginary_upper;
double real_left, real_right;
double pixel_scale;

int render_left, render_right;
int render_top, render_bottom;

static int doIterations(double x, double y) {
    double U, V, A, B;

    U = (A = x) * A;
    V = (B = y) * B;

    int i;

    for (i = 1; i <= max_iterations; ++ i) {
        B = 2.0 * A * B + y;
        A = U - V + x;
        U = A * A;
        V = B * B;

        if (U + V > bailout_value) {
            return i;
        }
    }

    return 0;
}

int32_t __attribute__((kernel)) pixel(uint32_t x) {
    int cx, cy;

    cy = x / canvas_width;
    cx = x - (cy * canvas_width);

    if ((cx >= render_left && cx <= render_right) || (cy >= render_top && cy <= render_bottom))
    {
        return doIterations(
            real_left + (cx * pixel_scale),
            imaginary_lower + (cy * pixel_scale)
        );
    }

    return null_iteration;
}