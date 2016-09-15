package jetsetilly.mandelbrot.RenderCanvas.ImageView;

import android.graphics.Bitmap;

import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;

abstract class Plotter {
    protected RenderCanvas_ImageView render_canvas;
    protected int width, height;

    public Plotter(RenderCanvas_ImageView canvas) {
        render_canvas = canvas;
        width = canvas.getWidth();
        height = canvas.getHeight();
    }

    abstract void startPlot(Bitmap bitmap);
    abstract void updatePlot();
    abstract void endPlot(boolean cancelled);

    public void plotIteration(int cx, int cy, int iteration) {}

    // return whether every iteration has resulted in a new pixel
    // iteration entries of Mandelbrot.NULL_ITERATIONS should not result in a new pixel
    public void plotIterations(int iterations[]) {
        for (int i = 0; i < iterations.length; ++ i) {
            if (iterations[i] != Mandelbrot.NULL_ITERATIONS) {
                plotIteration(i % width, i / width, iterations[i]);
            }
        }
    }
}
