package jetsetilly.mandelbrot.Mandelbrot;

public class Bookmarks {
    static public class Bookmark {
        public final String name;
        public final double real_left;
        public final double real_right;
        public final double imaginary_upper;
        public final double imaginary_lower;
        public final int max_iterations;
        public final double bailout_value;

        public Bookmark(String name, double rl, double rr, double iu, double il, int mi, double bv) {
            this.name = name;
            real_left = rl;
            real_right = rr;
            imaginary_upper = iu;
            imaginary_lower = il;
            max_iterations = mi;
            bailout_value = bv;
        }
    }

    // default values -- these are approximately square
    // Mandelbrot.correctMandelbrotRange() will correct bad ratios
    static public final int DEFAULT_SETTINGS = 0;

    static public final Bookmark[] presets = {
            new Bookmark("All the way out", -2.11, 1.16, 2.94, -2.88, 60, 4.0),
            new Bookmark("First test of imagination", -1.8036, -1.7256, 0.0692, -0.0694, 67, 4.0)
    };
}
