package jetsetilly.mandelbrot.Gestures;

public interface GestureHandler {
    void scroll(float x, float y);
    void finishManualGesture();
    boolean autoZoom(float offset_x, float offset_y, boolean zoom_out);
    boolean manualZoom(float amount);
    void endManualZoom();
}
