package jetsetilly.mandelbrot.RenderCanvas.ImageView;

import android.graphics.Bitmap;
import android.support.annotation.UiThread;

import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.mandelbrot.Palette.Palette;
import jetsetilly.tools.LogTools;
import jetsetilly.tools.SimpleAsyncTask;

public class BufferSimple extends Buffer {
    final static public String DBG_TAG = "buffer simple";

    private int[] palette;
    private int[] pixels;
    private int[] palette_frequencies;

    public BufferSimple(RenderCanvas_ImageView canvas) {
        super(canvas);

        palette = Palette.getInstance().getColours();
        pixels = new int[height * width];
        palette_frequencies = new int[palette.length + 1];
    }

    @Override
    void startDraw(Bitmap bitmap) {
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
    }

    @Override
    void update() {
        // in this buffer implementation update() does nothing
        // we don't actually show the bitmap until endDraw() is called
        // so there's nothing meaningful to do
    }

    @UiThread
    @Override
    public void endDraw(boolean cancelled) {
        if (!cancelled) {
            new SimpleAsyncTask("BufferSimple.endDraw()", new Runnable() {
                @Override
                public void run() {
                    boolean ret = render_canvas.setDisplay(
                            pixels,
                            RenderCanvas_ImageView.TransitionType.CROSS_FADE,
                            RenderCanvas_ImageView.TransitionSpeed.NORMAL,
                            false
                    );

                    if (!ret) {
                        LogTools.printDebug(DBG_TAG, "setDisplay() did not complete work");
                    }
                }
            });

            // update the most frequent color so we can use it as the background colour
            int most_frequent = 0;
            for (int i = 0; i < palette_frequencies.length; ++i) {
                if (palette_frequencies[i] > palette_frequencies[most_frequent]) {
                    most_frequent = i;
                }
            }
            render_canvas.background_colour = palette[most_frequent];
        }
    }

    // any thread
    public void plotIterations(int iterations[]) {
        for (int i = 0; i < iterations.length; ++ i) {
            int iteration = iterations[i];
            if (iteration != Mandelbrot.NULL_ITERATIONS) {
                // figure out which colour to use
                int palette_entry = iteration;
                if (iteration >= palette.length) {
                    palette_entry = (iteration % (palette.length - 1)) + 1;
                }

                // put coloured pixel into pixel buffer - ready for flushing
                pixels[i] = palette[palette_entry];

                // update palette frequency
                // we don't want to consider palette[0] for the colour_cnt_highest
                // it's the zero space color it's not really a color
                if (palette_entry > 0) {
                    palette_frequencies[palette_entry]++;
                }
            }
        }
    }
}
