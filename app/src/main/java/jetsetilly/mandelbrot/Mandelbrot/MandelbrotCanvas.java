package jetsetilly.mandelbrot.Mandelbrot;

public interface MandelbrotCanvas {
    void startDraw();
    void drawPoint(float dx, float dy, int iteration);
    void endDraw();
    void update();

    int getCanvasWidth();
    int getCanvasHeight();
    double getCanvasHypotenuse();
    double getCanvasRatio();
    int getPaletteSize();
}
