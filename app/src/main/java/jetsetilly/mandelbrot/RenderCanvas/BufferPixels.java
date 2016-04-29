package jetsetilly.mandelbrot.RenderCanvas;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import java.util.Timer;
import java.util.TimerTask;

import jetsetilly.mandelbrot.Settings.MandelbrotSettings;
import jetsetilly.mandelbrot.Settings.PaletteSettings;

public class BufferPixels implements Buffer {
    final static public String DBG_TAG = "buffer pixels";

    private final PaletteSettings palette_settings = PaletteSettings.getInstance();

    private RenderCanvas render_canvas;
    private Bitmap bitmap;
    private int width, height;

    private int[] palette_frequency;
    private int most_frequent_palette_entry;

    private int[] pixels;

    final static long PIXEL_THRESHOLD = 10000;
    private long pixel_ct;

    final static long PIXEL_UPDATE_FREQ = 100; // 100 == 10fps
    Timer pixel_scheduler = new Timer();
    TimerTask pixel_scheduler_task = new TimerTask() {
        @Override
        public void run() {
            if (pixel_ct > PIXEL_THRESHOLD) {
                bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
                pixel_ct = 0;
            }
        }
    };

    public BufferPixels(RenderCanvas canvas) {
        render_canvas = canvas;
        width = canvas.getCanvasWidth();
        height = canvas.getCanvasHeight();

        pixels = new int[height * width];
        palette_frequency = new int[
                Math.min(palette_settings.numColors(),
                        MandelbrotSettings.getInstance().max_iterations) + 1
                ];
    }

    @Override
    public void primeBuffer(Bitmap bitmap) {
        this.bitmap = bitmap;
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        pixel_scheduler.schedule(pixel_scheduler_task, 0, PIXEL_UPDATE_FREQ);
        pixel_ct = 0;
    }

    @Override
    public void flush(Boolean final_flush) {
        render_canvas.invalidate();

        if (final_flush) {
            pixel_scheduler.cancel();
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        }

        render_canvas.render_cache.colourCountUpdate(most_frequent_palette_entry);
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
