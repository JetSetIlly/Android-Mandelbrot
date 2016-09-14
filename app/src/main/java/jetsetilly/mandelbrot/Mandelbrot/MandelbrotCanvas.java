package jetsetilly.mandelbrot.Mandelbrot;

public interface MandelbrotCanvas {
    void startDraw(long render_id);
    void plotIterations(long render_id, int iterations[], boolean complete_plot);
    void plotIteration(long render_id, int dx, int dy, int iteration);
    void update(long render_id);
    void endDraw(long render_id, boolean cancelled);
}
