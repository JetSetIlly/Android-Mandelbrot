package jetsetilly.mandelbrot.RenderCanvas;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import java.util.concurrent.Semaphore;

import jetsetilly.mandelbrot.Settings.MandelbrotSettings;
import jetsetilly.mandelbrot.Settings.PaletteSettings;

public class BufferPixels implements Buffer {
    final static public String DBG_TAG = "buffer pixels";

    private final PaletteSettings palette_settings = PaletteSettings.getInstance();
    private RenderCanvas render_canvas;

    private int width, height;

    private int[] pixels;
    private int pixel_ct;

    private int[] palette_frequency;
    private int most_frequent_palette_entry;

    private SetPixelsTask set_pixels_task;

    public BufferPixels(RenderCanvas canvas) {
        render_canvas = canvas;
        width = canvas.getCanvasWidth();
        height = canvas.getCanvasHeight();

        pixels = new int[canvas.getCanvasHeight() * width];
        pixel_ct = 0;

        palette_frequency = new int[
                Math.min(palette_settings.numColors(),
                        MandelbrotSettings.getInstance().max_iterations) + 1
                ];

        set_pixels_task = new SetPixelsTask();
    }

    @Override
    public void primeBuffer(Bitmap bitmap) {
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        set_pixels_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, bitmap);
    }

    @Override
    public void flush(Boolean final_flush) {
        if (final_flush || pixel_ct > 10000) {
            render_canvas.render_cache.colourCountUpdate(most_frequent_palette_entry);
            pixel_ct = 0;

            if (final_flush) {
                set_pixels_task.finish();
                set_pixels_task = null;
            } else {
                set_pixels_task.draw();
            }
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

    private class SetPixelsTask extends AsyncTask<Bitmap, Void, Void> {
        private Bitmap flush_bitmap = null;
        private Semaphore signal = new Semaphore(1);
        private Boolean finish = false;

        public void finish() {
            finish = true;
            signal.release();
        }

        public void draw() {
            signal.release();
        }

        private void flushPixels() {
            flush_bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        }

        @Override
        protected Void doInBackground(Bitmap... bitmap) {
            flush_bitmap = bitmap[0];

            while (!finish && !isCancelled()) {
                try {
                    signal.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                flushPixels();
                publishProgress();
                signal.release();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... v) {
            render_canvas.invalidate();
        }

        @Override
        protected void onPostExecute(Void v) {
            flushPixels();
            render_canvas.invalidate();
        }
    }
}
