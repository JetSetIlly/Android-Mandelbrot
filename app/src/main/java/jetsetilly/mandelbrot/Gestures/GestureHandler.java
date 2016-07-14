package jetsetilly.mandelbrot.Gestures;

public interface GestureHandler {
    void checkActionBar(float x, float y, boolean show);
    int getCanvasWidth();
    int getCanvasHeight();
    void scrollBy(int x, int y);
    void animatedZoom(int offset_x, int offset_y);
    void pinchZoom(float amount);
    void zoomCorrection(boolean force);
}
