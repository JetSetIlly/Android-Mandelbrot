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

    void scheduleDraw(int cx, int cy, int iteration) {}
    void scheduleDraw(int iterations[]) {}
}