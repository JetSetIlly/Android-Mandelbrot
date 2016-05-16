package jetsetilly.mandelbrot.RenderCanvas;

import android.graphics.Bitmap;

import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.mandelbrot.Settings.PaletteSettings;

public class BufferSimple extends Buffer {
    final static public String DBG_TAG = "buffer hardware";

    private final PaletteSettings palette_settings = PaletteSettings.getInstance();

    private Bitmap buffer_bitmap;
    private int[] pixels;

    private int[] palette_frequency;
    private int most_frequent_palette_entry;

    public BufferSimple(RenderCanvas canvas) {
        super(canvas);
        pixels = new int[height * width];
        palette_frequency = new int[palette_settings.numColors() + 1];
    }

    @Override
    void primeBuffer(Bitmap bitmap) {
        this.buffer_bitmap = bitmap.copy(bitmap.getConfig(), true);
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
    }

    @Override
    void flush() {
        buffer_bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
    }

    @Override void endBuffer(boolean cancelled) {
        buffer_bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        render_canvas.setImageBitmap(buffer_bitmap, !cancelled);
        render_canvas.invalidate();
        render_canvas.colour_cache.colourCountUpdate(most_frequent_palette_entry);
    }

    @Override
    public boolean plotIterations(int iterations[]) {
        boolean complete_set_of_iterations = true;

        for (int i = 0; i < iterations.length; ++ i) {
            int iteration = iterations[i];
            if (iteration != Mandelbrot.NULL_ITERATIONS) {
                // figure out which colour to use
                int palette_entry = iteration;
                if (iteration >= palette_settings.numColors()) {
                    palette_entry = (iteration % (palette_settings.numColors() - 1)) + 1;
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
            } else {
                complete_set_of_iterations = false;
            }
        }

        return complete_set_of_iterations;
    }
}