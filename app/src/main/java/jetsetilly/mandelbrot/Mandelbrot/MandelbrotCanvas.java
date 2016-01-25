package jetsetilly.mandelbrot.Mandelbrot;

public interface MandelbrotCanvas {
    void startDraw(Mandelbrot.RenderMode render_mode);
    void drawPoint(float dx, float dy, int iteration);
    void endDraw();
    void update();

    int getWidth();
    int getHeight();
}
