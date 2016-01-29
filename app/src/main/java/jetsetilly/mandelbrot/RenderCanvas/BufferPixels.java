package jetsetilly.mandelbrot.RenderCanvas;

import android.graphics.Bitmap;
import android.os.AsyncTask;

import jetsetilly.mandelbrot.Settings.MandelbrotSettings;
import jetsetilly.mandelbrot.Settings.PaletteSettings;

public class BufferPixels implements Buffer {
    final static public String DBG_TAG = "buffer pixels";

    private final PaletteSettings palette_settings = PaletteSettings.getInstance();
    private RenderCanvas render_canvas;

    private int width, height;

    private int[] pixels;
    private int pixel_ct;
    private boolean pixel_flush;

    private int[] palette_frequency;
    private int most_frequent_palette_entry;

    private AsyncTask<Bitmap, Integer, Void> set_pixels_task;

    public BufferPixels(RenderCanvas canvas) {
        render_canvas = canvas;
        width = canvas.getWidth();
        height = canvas.getHeight();

        pixels = new int[canvas.getHeight() * width];
        pixel_ct = 0;
        pixel_flush = false;

        palette_frequency = new int[MandelbrotSettings.getInstance().max_iterations];

        set_pixels_task = new AsyncTask<Bitmap, Integer, Void>() {
            private int num_of_calls = 0;
            private Bitmap flush_bitmap;

            private void flushPixels() {
                flush_bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            }

            @Override
            protected Void doInBackground(Bitmap... bitmap) {
                flush_bitmap = bitmap[0];

                while (!isCancelled() ) {
                    if (pixel_flush) {
                        flushPixels();
                        publishProgress(num_of_calls++);
                        pixel_flush = false;
                    }
                }

                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... pass) {
                render_canvas.invalidate();
            }

            @Override
            protected void onCancelled() {
                flushPixels();
                render_canvas.invalidate();
            }
        };
    }

    @Override
    public void primeBuffer(Bitmap bitmap) {
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        set_pixels_task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, bitmap);
    }

    @Override
    public void flush(final Bitmap bitmap, Boolean final_flush) {
        if (final_flush || pixel_ct > 10000) {
            pixel_flush = true;
            palette_settings.updateCount(most_frequent_palette_entry);
            pixel_ct = 0;

            if (final_flush) {
                set_pixels_task.cancel(false);
            }
        }
    }

    @Override
    public void pushDraw(float cx, float cy, int iteration) {
        int palette_entry = iteration;

        if (iteration >= palette_settings.numColors()) {
            palette_entry = (iteration % (palette_settings.numColors() - 1)) + 1;
        }

        pixels[((int)cy * width) + (int)cx] = palette_settings.selected_palette.colours[palette_entry];

        palette_frequency[palette_entry] ++;
        if (palette_frequency[palette_entry] > palette_frequency[most_frequent_palette_entry]) {
            most_frequent_palette_entry = palette_entry;
        }

        pixel_ct ++;
    }
}
