package jetsetilly.mandelbrot.RenderCanvas;

import android.graphics.Bitmap;

interface Buffer {
    void primeBuffer(Bitmap bitmap);
    void flush(Boolean final_flush);
    void pushDraw(float cx, float cy, int iteration);
}