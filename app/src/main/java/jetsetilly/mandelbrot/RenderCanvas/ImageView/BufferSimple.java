package jetsetilly.mandelbrot.RenderCanvas.ImageView;

import android.graphics.Bitmap;
import android.support.annotation.UiThread;

import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.mandelbrot.Settings.PaletteSettings;
import jetsetilly.tools.LogTools;

public class BufferSimple extends Buffer {
    final static public String DBG_TAG = "buffer hardware";

    private final PaletteSettings palette_settings = PaletteSettings.getInstance();

    private int[] pixels;

    private int[] palette_frequencies;
    private int num_colours;

    public BufferSimple(RenderCanvas_ImageView canvas) {
        super(canvas);
        pixels = new int[height * width];
        num_colours = palette_settings.numColors();
        palette_frequencies = new int[num_colours + 1];
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
    public int endDraw(boolean cancelled) {
        LogTools.printDebug(DBG_TAG, "endDraw(cancelled == " + cancelled + ")");
        if (!cancelled) {
            render_canvas.setNextTransition(RenderCanvas_ImageView.TransitionType.CROSS_FADE);
            int speed = render_canvas.setDisplay(pixels);

            // update the most frequent color so we can use it as the background colour
            int most_frequent = 0;
            for (int i = 0; i < palette_frequencies.length; ++ i) {
                if (palette_frequencies[i] > palette_frequencies[most_frequent]) {
                    most_frequent = i;
                }
            }
            render_canvas.background_colour = palette_settings.colours[most_frequent];

            return speed;
        }

        return 0;
    }

    // any thread
    public void plotIterations(int iterations[]) {
        for (int i = 0; i < iterations.length; ++ i) {
            int iteration = iterations[i];
            if (iteration != Mandelbrot.NULL_ITERATIONS) {
                // figure out which colour to use
                int palette_entry = iteration;
                if (iteration >= num_colours) {
                    palette_entry = (iteration % (num_colours - 1)) + 1;
                }

                // put coloured pixel into pixel buffer - ready for flushing
                pixels[i] = palette_settings.colours[palette_entry];

                // update palette frequency
                // we don't want to consider colours[0] for the colour_cnt_highest
                // it's the zero space color it's not really a color
                if (palette_entry > 0) {
                    palette_frequencies[palette_entry]++;
                }
            }
        }
    }
}
