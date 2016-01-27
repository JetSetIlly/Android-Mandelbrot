package jetsetilly.mandelbrot.RenderCanvas;

import android.graphics.Bitmap;

public interface Buffer {
    void primeBuffer(Bitmap bitmap);
    boolean flush(Bitmap bitmap, Boolean forced);   // returns true if bitmap has been altered
    void pushDraw(float cx, float cy, int iteration);
}