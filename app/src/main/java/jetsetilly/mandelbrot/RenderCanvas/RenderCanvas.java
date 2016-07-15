package jetsetilly.mandelbrot.RenderCanvas;

import android.graphics.Bitmap;

import jetsetilly.mandelbrot.MainActivity;

public interface RenderCanvas {
    void initialise(MainActivity main_activity);
    void startRender();
    void stopRender();
    void resetCanvas();
    void reRender();
    Bitmap getVisibleImage (boolean bilinear_filter);
}
