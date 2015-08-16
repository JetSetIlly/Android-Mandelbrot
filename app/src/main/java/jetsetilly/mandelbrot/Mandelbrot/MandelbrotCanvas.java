package jetsetilly.mandelbrot.Mandelbrot;

public interface MandelbrotCanvas {
    void startDrawSequence();
    void doDraw(float dx, float dy, int iteration);
    void doDraw(float[] points, int points_len, int iteration);
    void notifyDraw(Buffer buffer, int iteration);
    void endDrawSequence();
    void update();
    int getCanvasWidth();
    int getCanvasHeight();
    double getCanvasHypotenuse();
    double getCanvasRatio();
    int getPaletteSize();
}
