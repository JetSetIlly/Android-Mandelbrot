package jetsetilly.mandelbrot.RenderCanvas;

import android.graphics.Bitmap;

import java.util.Timer;
import java.util.TimerTask;

import jetsetilly.mandelbrot.Settings.PaletteSettings;

public class BufferTimer extends Buffer {
    final static public String DBG_TAG = "buffer pixels";

    private final PaletteSettings palette_settings = PaletteSettings.getInstance();

    private Bitmap bitmap;
    private int[] pixels;
    private long pixel_ct;

    final static long PIXEL_THRESHOLD = 10000;
    final static long PIXEL_UPDATE_FREQ = 100; // in milliseconds; 100 == 10fps

    private int[] palette_frequency;
    private int most_frequent_palette_entry;

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

    public BufferTimer(RenderCanvas canvas) {
        super(canvas);
        pixels = new int[height * width];
        palette_frequency = new int[palette_settings.numColors() + 1];
    }

    @Override
    public void primeBuffer(Bitmap bitmap) {
        this.bitmap = bitmap;
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        pixel_scheduler.schedule(pixel_scheduler_task, 0, PIXEL_UPDATE_FREQ);
        pixel_ct = 0;
    }

    @Override
    public void flush() {
        render_canvas.invalidate();
        render_canvas.colour_cache.colourCountUpdate(most_frequent_palette_entry);
    }

    public void endBuffer(boolean cancelled) {
        pixel_scheduler.cancel();
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        render_canvas.invalidate();
        render_canvas.colour_cache.colourCountUpdate(most_frequent_palette_entry);
    }

    @Override
    public void scheduleDraw(int cx, int cy, int iteration) {
        // do nothing if iteration is less than zero. -1 is used as
        // a place holder to indicate that no iterations have taken place
        if (iteration < 0)
            return;

        // figure out which colour to use
        int palette_entry = iteration;
        if (iteration >= palette_settings.numColors()) {
            palette_entry = (iteration % (palette_settings.numColors() - 1)) + 1;
        }

        // put coloured pixel into pixel buffer - ready for flushing
        pixels[(cy * width) + cx] = palette_settings.colours[palette_entry];

        // update palette frequency
        // we don't want to consider colours[0] for the colour_cnt_highest
        // it's the zero space color it's not really a color
        if (palette_entry > 0) {
            palette_frequency[palette_entry]++;
            if (palette_frequency[palette_entry] > palette_frequency[most_frequent_palette_entry]) {
                most_frequent_palette_entry = palette_entry;
            }
        }

        pixel_ct ++;
    }

    @Override
    public void scheduleDraw(int iterations[]) {
        int cx, cy;

        for (int i = 0; i < iterations.length; ++ i) {
            cy = i / width;
            cx = i - (cy * width);
            scheduleDraw(cx, cy, iterations[i]);
        }
    }
}
