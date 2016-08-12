package jetsetilly.mandelbrot.Gestures;

public interface GestureHandler {
    void checkActionBar(float x, float y, boolean show);
    void scroll(int x, int y);
    void finishScroll();
    void autoZoom(int offset_x, int offset_y, boolean zoom_out);
    void manualZoom(float amount);
    void endManualZoom(boolean force);
}
