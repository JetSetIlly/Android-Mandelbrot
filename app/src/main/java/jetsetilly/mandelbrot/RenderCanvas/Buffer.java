package jetsetilly.mandelbrot.RenderCanvas;

import android.graphics.Bitmap;

import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;

abstract class Buffer {
    protected RenderCanvas_ImageView render_canvas;
    protected int width, height;

    public Buffer(RenderCanvas_ImageView canvas) {
        render_canvas = canvas;
        width = canvas.getCanvasWidth();
        height = canvas.getCanvasHeight();
    }

    abstract void primeBuffer(Bitmap bitmap);
    abstract void flush();
    abstract void endBuffer(boolean cancelled);

    void plotIteration(int cx, int cy, int iteration) {}

    // return whether every iteration has resulted in a new pixel
    // iteration entries of Mandelbrot.NULL_ITERATIONS should not result in a new pixel
    void plotIterations(int iterations[]) {
        for (int i = 0; i < iterations.length; ++ i) {
            if (iterations[i] != Mandelbrot.NULL_ITERATIONS) {
                plotIteration(i % width, i / width, iterations[i]);
            }
        }
    }
}