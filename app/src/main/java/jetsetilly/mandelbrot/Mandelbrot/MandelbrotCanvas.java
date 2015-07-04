package jetsetilly.mandelbrot.Mandelbrot;

public interface MandelbrotCanvas {
    void doDraw(float dx, float dy, int iteration);
    void doDraw(float[] points, int points_len, int iteration);
    void update();
    int getCanvasWidth();
    int getCanvasHeight();
    double getCanvasHypotenuse();
    double getCanvasRatio();
    int getPaletteSize();
}
