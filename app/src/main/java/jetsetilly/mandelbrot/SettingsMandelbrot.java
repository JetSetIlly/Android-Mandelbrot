package jetsetilly.mandelbrot;

import java.lang.reflect.Field;

public class SettingsMandelbrot {
    private final double DEF_REAL_LEFT = -2.11;
    private final double DEF_REAL_RIGHT = 1.16;
    private final double DEF_IMAG_UPPER = 2.94;
    private final double DEF_IMAG_LOWER = -2.88;
    private final int DEF_MAX_ITERATIONS = 60;
    private final double DEF_BAILOUT_VALUE = 4.0;

    /* default values -- these are approximately square. Mandelbrot.correctMandelbrotRange() will correct squishiness */
    public double real_left;
    public double real_right;
    public double imaginary_upper;
    public double imaginary_lower;

    public int max_iterations;
    public double bailout_value;

    SettingsMandelbrot () {
        resetCoords();
    }

    public void resetCoords() {
        real_left = DEF_REAL_LEFT;
        real_right = DEF_REAL_RIGHT;
        imaginary_upper = DEF_IMAG_UPPER;
        imaginary_lower = DEF_IMAG_LOWER;
        max_iterations = DEF_MAX_ITERATIONS;
        bailout_value = DEF_BAILOUT_VALUE;
    }

    /* singleton pattern */
    private static SettingsMandelbrot singleton = new SettingsMandelbrot();
    public static SettingsMandelbrot getInstance() {
        return singleton;
    }
    /* end of singleton pattern */
}
