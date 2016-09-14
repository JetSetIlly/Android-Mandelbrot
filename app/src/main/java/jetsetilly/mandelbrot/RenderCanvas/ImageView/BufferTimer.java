package jetsetilly.mandelbrot.RenderCanvas.ImageView;

import android.graphics.Bitmap;

import java.util.Timer;
import java.util.TimerTask;

import jetsetilly.mandelbrot.Palette.Palette;

public class BufferTimer extends Buffer {
    final static public String DBG_TAG = "buffer pixels";

    private Bitmap bitmap;
    private int[] palette;
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

        palette = Palette.getInstance().getColours();
        pixels = new int[height * width];
        palette_frequencies = new int[palette.length + 1];
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
    public void endDraw(boolean cancelled) {
        pixel_scheduler.cancel();
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

        // update the most frequent color so we can use_next it as the background colour
        int most_frequent = 0;
        for (int i = 0; i < palette_frequencies.length; ++ i) {
            if (palette_frequencies[i] > palette_frequencies[most_frequent]) {
                most_frequent = i;
            }
        }
        render_canvas.setBackgroundColor(palette[most_frequent]);

        update();
    }

    @Override
    public void plotIteration(int cx, int cy, int iteration) {
        // do nothing if iteration is less than zero. -1 is used as
        // a place holder to indicate that no iterations have taken place
        if (iteration < 0)
            return;

        // figure out which colour to use_next
        int palette_entry = iteration;
        if (iteration >= palette.length) {
            palette_entry = (iteration % (palette.length - 1)) + 1;
        }

        // put coloured pixel into pixel buffer - ready for flushing
        pixels[(cy * width) + cx] = palette[palette_entry];

        // update palette frequency
        // we don't want to consider palette[0] for the colour_cnt_highest
        // it's the zero space color it's not really a color
        if (palette_entry > 0) {
            palette_frequencies[palette_entry]++;
        }

        pixel_ct ++;
    }
}
