package jetsetilly.mandelbrot.RenderCanvas.ImageView;

import android.graphics.Bitmap;
import android.support.annotation.UiThread;

import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.mandelbrot.Settings.PaletteSettings;

public class BufferSimple extends Buffer {
    final static public String DBG_TAG = "buffer hardware";

    private final PaletteSettings palette_settings = PaletteSettings.getInstance();

    private Bitmap buffer_bitmap;
    private int[] pixels;

    private int[] palette_frequency;
    private int most_frequent_palette_entry;
    private int num_colours;

    public BufferSimple(RenderCanvas_ImageView canvas) {
        super(canvas);
        pixels = new int[height * width];
        num_colours = palette_settings.numColors();
        palette_frequency = new int[num_colours + 1];
    }

    @Override
    void startDraw(Bitmap bitmap) {
        this.buffer_bitmap = bitmap.copy(bitmap.getConfig(), true);
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
    }

    @Override
    void update() {
        // in this buffer implementation update() does nothing
        // we don't actually show the bitmap until endDraw() is called
        // so there's nothing meaningful to do
    }

    @UiThread
    @Override void endDraw(boolean cancelled) {
        if (!cancelled) {
            buffer_bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            render_canvas.setNextTransition(RenderCanvas_ImageView.TransitionType.CROSS_FADE);
            render_canvas.showBitmap(buffer_bitmap);
            render_canvas.background_colour = palette_settings.colours[most_frequent_palette_entry];
        }
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
                    palette_frequency[palette_entry]++;
                    if (palette_frequency[palette_entry] > palette_frequency[most_frequent_palette_entry]) {
                        most_frequent_palette_entry = palette_entry;
                    }
                }
            }
        }
    }
}
