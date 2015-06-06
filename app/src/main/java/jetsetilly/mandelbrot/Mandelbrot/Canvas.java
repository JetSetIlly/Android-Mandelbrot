package jetsetilly.mandelbrot.Mandelbrot;

public interface Canvas {
    void doDraw(float dx, float dy, int iteration);
    void doDraw(float[] points, int points_len, int iteration);
    void update();
    int getCanvasWidth();
    int getCanvasHeight();
    int getPaletteSize();
}
