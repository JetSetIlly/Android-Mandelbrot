package jetsetilly.mandelbrot.Mandelbrot;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.IntDef;

import jetsetilly.mandelbrot.R;
import jetsetilly.mandelbrot.Settings.MandelbrotCoordinates;
import jetsetilly.mandelbrot.Settings.Settings;
import jetsetilly.tools.LogTools;

public class Mandelbrot {
    private final static String DBG_TAG = "mandelbrot";

    public static final int NULL_ITERATIONS = -1;

    public @IntDef({CalculationMethod.NATIVE, CalculationMethod.BIG_DECIMAL})
    @interface CalculationMethod {
        int NATIVE = 0;
        int BIG_DECIMAL = 1;
    }

    public @IntDef({RenderMode.HARDWARE, RenderMode.SOFTWARE_TOP_DOWN, RenderMode.SOFTWARE_CENTRE})
    @interface RenderMode {
        int HARDWARE = 0;
        int SOFTWARE_TOP_DOWN =1;
        int SOFTWARE_CENTRE = 2;
    }

    public @IntDef({IterationsRate.SLOW, IterationsRate.NORMAL, IterationsRate.FAST})
    @interface IterationsRate {
        int SLOW = 0;
        int NORMAL = 1;
        int FAST =2;
        int COUNT =3;
    }
    public int[] IterationsRateValues = {50, 40, 30};

    private final Context context;

    private final MandelbrotCoordinates mandelbrot_coordinates = MandelbrotCoordinates.getInstance();
    private final Settings settings = Settings.getInstance();

    // the pixel scale we reckon from when calculating scale amount (the amount we've zoomed in)
    private static final double BASE_PIXEL_SCALE = 0.0030312500000000005;

    // the limit to the amount of scaling the floating point calculations can handle
    private static final double SCALE_LIMIT = 6.0e-17;

    protected final int canvas_width, canvas_height;
    protected final double canvas_ratio;
    protected double pixel_scale;

    // render_area is used to define that area of the canvas
    // that needs to be rendered again. pixels outside this area
    // already appear on the canvas
    //
    // if ((pixel.x >= protected_left && pixel.x <= protected_right) || (pixel.y >= protected_top && pixel.y <= protected_bottom))
    // then
    //      render pixel
    // end if
    protected Rect render_area;

    // has the scale of the mandelbrot changed
    protected boolean rescaling_render;

    public Mandelbrot(Context context, int canvas_width, int canvas_height) {
        this.context = context;
        this.canvas_width = canvas_width;
        this.canvas_height = canvas_height;
        this.canvas_ratio = (double) canvas_width / (double) canvas_height;
        this.pixel_scale = (mandelbrot_coordinates.real_right - mandelbrot_coordinates.real_left) / canvas_width;
    }

    @Override
    public String toString() {
        return String.format(context.getResources().getString(R.string.mandelbrot_info_string),
                mandelbrot_coordinates.imaginary_lower,
                mandelbrot_coordinates.real_left,
                mandelbrot_coordinates.real_right,
                mandelbrot_coordinates.imaginary_lower,
                pixel_scale,
                mandelbrot_coordinates.max_iterations,
                BASE_PIXEL_SCALE / pixel_scale
        );
    }

    public void transformMandelbrot(MandelbrotTransform transform, boolean redraw_all) {
        double fractal_width = mandelbrot_coordinates.real_right - mandelbrot_coordinates.real_left;
        double fractal_height = mandelbrot_coordinates.imaginary_upper - mandelbrot_coordinates.imaginary_lower;

        if (transform.scale != 0) {
            if (pixel_scale < SCALE_LIMIT) {
                LogTools.printDebug(DBG_TAG, context.getResources().getString(R.string.scale_limit_reached));
            }

            float factor = (transform.scale - 1.0f) / (2.0f * transform.scale);
            mandelbrot_coordinates.real_left += factor * fractal_width;
            mandelbrot_coordinates.real_right -= factor * fractal_width;
            mandelbrot_coordinates.imaginary_upper -= factor * fractal_height;
            mandelbrot_coordinates.imaginary_lower += factor * fractal_height;

            double iterations_rate = IterationsRateValues[settings.iterations_rate];
            if (transform.scale > 1) {
                // scale up
                mandelbrot_coordinates.max_iterations = mandelbrot_coordinates.max_iterations + (int) (mandelbrot_coordinates.max_iterations * transform.scale / iterations_rate);
            } else if (transform.scale < 1 ) {
                // scale down
                mandelbrot_coordinates.max_iterations = (int) ((mandelbrot_coordinates.max_iterations * iterations_rate) / (iterations_rate + (1.0/transform.scale)));
            }
        }

        mandelbrot_coordinates.real_left += transform.x * pixel_scale;
        mandelbrot_coordinates.real_right += transform.x * pixel_scale;
        mandelbrot_coordinates.imaginary_upper += transform.y * pixel_scale;
        mandelbrot_coordinates.imaginary_lower += transform.y * pixel_scale;

        /* CORRECT */

        // makes sure the pixel_scale is square when spanning the real and imaginary coordinates
        // particularly useful if screen dimensions change, which it does if screen is rotated.

        // add padding to real axis
        // note padding will be negative when correcting from landscape to portrait
        double padding = (canvas_ratio * (mandelbrot_coordinates.imaginary_upper -  mandelbrot_coordinates.imaginary_lower)) - (mandelbrot_coordinates.real_right - mandelbrot_coordinates.real_left);
        mandelbrot_coordinates.real_right += padding / 2;
        mandelbrot_coordinates.real_left -= padding / 2;

        // correct pixel scale
        pixel_scale = (mandelbrot_coordinates.real_right - mandelbrot_coordinates.real_left) / canvas_width;

        /* SAVE */
        mandelbrot_coordinates.save(context);

        /* FINISH OFF - some calculations that might help some render modes */

        // define render_area
        render_area = new Rect(0, 0, canvas_width, canvas_height);
        if (transform.scale == 0 && !redraw_all) {
            if (transform.x < 0) {
                render_area.right = (int) -transform.x;
            } else if (transform.x > 0) {
                render_area.left = canvas_width - (int) transform.x;
            }

            if (transform.y < 0) { // moving down
                render_area.bottom = (int) -transform.y;
            } else if (transform.y > 0) { // moving up
                render_area.top = canvas_height - (int) transform.y;
            }
        }

        rescaling_render = transform.scale != 0;
    }
}
