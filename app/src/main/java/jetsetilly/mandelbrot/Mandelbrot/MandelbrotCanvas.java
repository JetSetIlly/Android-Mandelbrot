package jetsetilly.mandelbrot.Mandelbrot;

public interface MandelbrotCanvas {
    void startDraw(long canvas_id);
    void plotIterations(long canvas_id, int iterations[], boolean complete_plot);
    void plotIteration(long canvas_id, int dx, int dy, int iteration);
    void update(long canvas_id);
    void endDraw(long canvas_id);
    void cancelDraw(long canvas_id);
    int getCanvasWidth();
    int getCanvasHeight();
    boolean isCompleteRender();
}
