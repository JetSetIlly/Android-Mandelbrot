package jetsetilly.mandelbrot.Gestures;

public interface GestureHandler {
    void checkActionBar(float x, float y, boolean allow_show);
    void scroll(float x, float y);
    void finishManualGesture();
    void autoZoom(float offset_x, float offset_y, boolean zoom_out);
    void manualZoom(float amount);
    void endManualZoom();
}
