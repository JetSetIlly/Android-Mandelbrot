package jetsetilly.mandelbrot.RenderCanvas;

import android.graphics.Bitmap;

import java.util.concurrent.ExecutionException;

public interface Buffer {
    void primeBuffer(Bitmap bitmap);
    void flush(Boolean final_flush);
    void pushDraw(float cx, float cy, int iteration);
}