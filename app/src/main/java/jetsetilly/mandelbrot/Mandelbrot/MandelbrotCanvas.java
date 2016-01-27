package jetsetilly.mandelbrot.Mandelbrot;

public interface MandelbrotCanvas {
    void startDraw(Mandelbrot.RenderMode render_mode);
    void drawPoint(float dx, float dy, int iteration);
    void update();
    void endDraw();

    int getWidth();
    int getHeight();
}
