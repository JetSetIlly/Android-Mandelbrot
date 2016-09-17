package jetsetilly.mandelbrot.Gestures;

public interface GestureHandler {
    void checkActionBar(float x, float y, boolean allow_show);
    void scroll(float x, float y);
    void finishManualGesture();
    boolean autoZoom(float offset_x, float offset_y, boolean zoom_out);
    boolean manualZoom(float amount);
    void endManualZoom();
}
