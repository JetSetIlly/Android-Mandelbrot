package jetsetilly.mandelbrot.Mandelbrot;

public class MandelbrotSettings {
    public double real_left;
    public double real_right;
    public double imaginary_upper;
    public double imaginary_lower;

    public int max_iterations;
    public double bailout_value;

    MandelbrotSettings() {
        resetCoords();
    }

    public void resetCoords() {
        real_left = Presets.presets[Presets.DEFAULT_SETTINGS].real_left;
        real_right = Presets.presets[Presets.DEFAULT_SETTINGS].real_right;
        imaginary_upper = Presets.presets[Presets.DEFAULT_SETTINGS].imaginary_upper;
        imaginary_lower = Presets.presets[Presets.DEFAULT_SETTINGS].imaginary_lower;
        max_iterations = Presets.presets[Presets.DEFAULT_SETTINGS].max_iterations;
        bailout_value = Presets.presets[Presets.DEFAULT_SETTINGS].bailout_value;
    }

    /* singleton pattern */
    private static MandelbrotSettings singleton = new MandelbrotSettings();
    public static MandelbrotSettings getInstance() {
        return singleton;
    }
    /* end of singleton pattern */
}
