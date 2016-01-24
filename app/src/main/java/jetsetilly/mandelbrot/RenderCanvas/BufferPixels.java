package jetsetilly.mandelbrot.RenderCanvas;

import android.graphics.Bitmap;

import jetsetilly.mandelbrot.Settings.MandelbrotSettings;
import jetsetilly.mandelbrot.Settings.PaletteSettings;

public class BufferPixels implements Buffer {
    final static public String DBG_TAG = "buffer pixels";

    private final PaletteSettings palette_settings = PaletteSettings.getInstance();

    private int width, height;

    private int[] pixels;
    private int pixel_ct;

    private int[] freqPaletteEntry;
    private int mostFreqPaletteEntry;

    public BufferPixels(RenderCanvas canvas) {
        width = canvas.getCanvasWidth();
        height = canvas.getCanvasHeight();

        pixels = new int[canvas.getCanvasHeight() * width];
        pixel_ct = 0;

        freqPaletteEntry = new int[MandelbrotSettings.getInstance().max_iterations];
    }

    @Override
    public void primeBuffer(Bitmap bitmap) {
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
    }

    @Override
    public void flush(Bitmap bitmap, Boolean forced) {
        if (forced || pixel_ct > 10000) {
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            palette_settings.updateCount(mostFreqPaletteEntry);
            pixel_ct = 0;
        }
    }

    @Override
    public void pushDraw(float cx, float cy, int iteration) {
        int palette_entry = iteration;

        if (iteration >= palette_settings.numColors()) {
            palette_entry = (iteration % (palette_settings.numColors() - 1)) + 1;
        }

        pixels[((int)cy * width) + (int)cx] = palette_settings.selected_palette.colours[palette_entry];

        freqPaletteEntry[palette_entry] ++;
        if (freqPaletteEntry[palette_entry] > freqPaletteEntry[mostFreqPaletteEntry]) {
            mostFreqPaletteEntry = palette_entry;
        }

        pixel_ct ++;
    }
}
