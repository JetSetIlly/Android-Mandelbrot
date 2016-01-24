package jetsetilly.mandelbrot.RenderCanvas;

import android.graphics.Bitmap;

public interface Buffer {
    void primeBuffer(Bitmap bitmap);
    void flush(Bitmap bitmap, Boolean forced);
    void pushDraw(float cx, float cy, int iteration);
}