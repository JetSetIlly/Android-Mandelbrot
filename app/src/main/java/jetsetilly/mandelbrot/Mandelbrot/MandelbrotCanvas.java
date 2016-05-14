package jetsetilly.mandelbrot.Mandelbrot;

public interface MandelbrotCanvas {
    void startDraw(long canvas_id, Mandelbrot.RenderMode render_mode);
    void drawPoints(long canvas_id, int iterations[]);
    void drawPoint(long canvas_id, int dx, int dy, int iteration);
    void update(long canvas_id);
    void endDraw(long canvas_id);
    void cancelDraw(long canvas_id);

    int getCanvasWidth();
    int getCanvasHeight();

    boolean isCompleteRender();
}
