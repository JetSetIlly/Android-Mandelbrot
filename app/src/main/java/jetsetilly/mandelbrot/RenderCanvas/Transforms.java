package jetsetilly.mandelbrot.RenderCanvas;

public class Transforms {
    static public double imageScaleFromFractalScale(double fractal_scale) {
        return 1.0 / (1.0 - (2.0 * fractal_scale));
    }

    static public double fractalScaleFromImageScale(float image_scale) {
        return (image_scale - 1.0) / (2.0 * image_scale);
    }
}
