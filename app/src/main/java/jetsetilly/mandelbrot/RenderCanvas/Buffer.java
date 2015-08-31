package jetsetilly.mandelbrot.RenderCanvas;

public interface Buffer {
    void flush();
    void pushDraw(float cx, float cy, int iteration);
}