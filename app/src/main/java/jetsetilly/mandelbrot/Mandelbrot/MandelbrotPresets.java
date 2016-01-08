package jetsetilly.mandelbrot.Mandelbrot;

public class MandelbrotPresets {
    static public class Preset {
        public String name;
        public double real_left;
        public double real_right;
        public double imaginary_upper;
        public double imaginary_lower;
        public int max_iterations;
        public double bailout_value;

        public Preset(String name, double rl, double rr, double iu, double il, int mi, double bv) {
            this.name = name;
            real_left = rl;
            real_right = rr;
            imaginary_upper = iu;
            imaginary_lower = il;
            max_iterations = mi;
            bailout_value = bv;
        }
    }

    /* default values -- these are approximately square. Mandelbrot.correctMandelbrotRange() will correct squishiness */
    static public final int DEFAULT_SETTINGS = 0;

    static public Preset[] presets = {
            new Preset("All the way out", -2.11, 1.16, 2.94, -2.88, 60, 4.0),
            new Preset("First test of imagination", -1.8036, -1.7256, 0.0692, -0.0694, 67, 4.0)
    };
}
