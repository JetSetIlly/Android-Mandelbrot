package jetsetilly.mandelbrot.Mandelbrot;

public class MandelbrotTransform {
    // the amount of deviation (offset) from the current coordinates
    public float x;
    public float y;

    // the amount by which the mandelbrot needs to scale in order to match the display
    public float scale;

    public MandelbrotTransform() {
        reset();
    }

    public void reset() {
        x = 0.0f;
        y = 0.0f;
        scale = 1.0f;
    }

    public boolean isIdentity() {
        return x == 0.0f && y == 0.0f && scale == 1.0f;
    }
}
