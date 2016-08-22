package jetsetilly.mandelbrot.RenderCanvas.ImageView;

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

    private int[] palette_frequencies;

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

    public BufferTimer(RenderCanvas_ImageView canvas) {
        super(canvas);
        pixels = new int[height * width];
        palette_frequencies = new int[palette_settings.numColors() + 1];
    }

    @Override
    public void startDraw(Bitmap bitmap) {
        this.bitmap = bitmap;
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        pixel_scheduler.schedule(pixel_scheduler_task, 0, PIXEL_UPDATE_FREQ);
        pixel_ct = 0;
    }

    @Override
    public void update() {
        render_canvas.invalidate();
    }

    @Override
    public int endDraw(boolean cancelled) {
        pixel_scheduler.cancel();
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        // update the most frequent color so we can use it as the background colour
        int most_frequent = 0;
        for (int i = 0; i < palette_frequencies.length; ++ i) {
            if (palette_frequencies[i] > palette_frequencies[most_frequent]) {
                most_frequent = i;
            }
        }
        render_canvas.background_color = palette_settings.colours[most_frequent];

        update();
        return 0;
    }

    @Override
    public void plotIteration(int cx, int cy, int iteration) {
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
            palette_frequencies[palette_entry]++;
        }

        pixel_ct ++;
    }
}
