package jetsetilly.mandelbrot.Mandelbrot;

public interface MandelbrotCanvas {
    void startPlot(long render_id);
    void plotIterations(long render_id, int iterations[], boolean complete_plot);
    void plotIteration(long render_id, int dx, int dy, int iteration);
    void updatePlot(long render_id);
    void endPlot(long render_id, boolean cancelled);
}
