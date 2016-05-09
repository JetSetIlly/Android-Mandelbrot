package jetsetilly.mandelbrot.Mandelbrot;

public interface MandelbrotCanvas {
    void startDraw(Mandelbrot.RenderMode render_mode);
    void drawPoints(int iterations[]);
    void drawPoint(int dx, int dy, int iteration);
    void update();
    void endDraw();
    void cancelDraw();

    int getCanvasWidth();
    int getCanvasHeight();
}
