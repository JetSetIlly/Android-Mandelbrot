package jetsetilly.mandelbrot.RenderCanvas;

import android.graphics.Bitmap;

import jetsetilly.mandelbrot.Settings.MandelbrotSettings;
import jetsetilly.mandelbrot.Settings.PaletteSettings;

public class BufferPixels_non_threaded implements Buffer {
    final static public String DBG_TAG = "buffer pixels (simple)";

    private Bitmap bitmap;
    private RenderCanvas render_canvas;
    private final PaletteSettings palette_settings = PaletteSettings.getInstance();

    private int width, height;
    private int[] pixels;
    private int pixel_ct = 0;

    private int[] palette_frequency;
    private int most_frequent_palette_entry;

    public BufferPixels_non_threaded(RenderCanvas canvas) {
        this.render_canvas = canvas;
        width = canvas.getCanvasWidth();
        height = canvas.getCanvasHeight();

        pixels = new int[canvas.getCanvasHeight() * width];
        pixel_ct = 0;

        palette_frequency = new int[
                Math.min(palette_settings.numColors(),
                        MandelbrotSettings.getInstance().max_iterations) + 1
                ];
    }

    @Override
    public void primeBuffer(Bitmap bitmap) {
        this.bitmap = bitmap;
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
    }

    @Override
    public void flush(Boolean final_flush) {
        if (final_flush || pixel_ct > 10000) {
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            render_canvas.invalidate();

            render_canvas.render_cache.colourCountUpdate(most_frequent_palette_entry);
            pixel_ct = 0;
        }
    }

    @Override
    public void pushDraw(float cx, float cy, int iteration) {
        // figure out which colour to use
        int palette_entry = iteration;

        if (iteration >= palette_settings.numColors()) {
            palette_entry = (iteration % (palette_settings.numColors() - 1)) + 1;
        }

        // put coloured pixel into pixel buffer - ready for flushing
        pixels[((int)cy * width) + (int)cx] = palette_settings.colours[palette_entry];

        // update palette frequency
        palette_frequency[palette_entry] ++;
        if (palette_frequency[palette_entry] > palette_frequency[most_frequent_palette_entry]) {
            most_frequent_palette_entry = palette_entry;
        }

        pixel_ct ++;
    }
}
