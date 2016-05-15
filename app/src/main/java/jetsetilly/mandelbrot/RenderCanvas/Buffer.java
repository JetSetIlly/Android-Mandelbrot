package jetsetilly.mandelbrot.RenderCanvas;

import android.graphics.Bitmap;

abstract class Buffer {
    protected RenderCanvas render_canvas;
    protected int width, height;

    public Buffer(RenderCanvas canvas) {
        render_canvas = canvas;
        width = canvas.getCanvasWidth();
        height = canvas.getCanvasHeight();
    }

    abstract void primeBuffer(Bitmap bitmap);
    abstract void flush();
    abstract void endBuffer(boolean cancelled);

    void plotIteration(int cx, int cy, int iteration) {}

    // return whether every iteration has resulted in a new pixel
    // iteration entries of Mandelbrot.NULL_ITERATION should not result in a new pixel
    boolean plotIterations(int iterations[]) {
        return false;
    }
}