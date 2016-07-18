package jetsetilly.mandelbrot.Gestures;

public interface GestureHandler {
    void checkActionBar(float x, float y, boolean show);
    void scroll(int x, int y);
    void animatedZoom(int offset_x, int offset_y, boolean zoom_out);
    void pinchZoom(float amount);
    void zoomCorrection(boolean force);
    void finishGesture();
}
