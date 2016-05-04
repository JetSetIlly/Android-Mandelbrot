package jetsetilly.mandelbrot.RenderCanvas;

import android.graphics.Bitmap;

interface Buffer {
    void primeBuffer(Bitmap bitmap);
    void flush(Boolean final_flush);
    void pushDraw(int cx, int cy, int iteration);
}