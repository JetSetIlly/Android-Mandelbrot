package jetsetilly.mandelbrot.Gestures;

public interface GestureHandler {
    void checkActionBar(int x, int y, boolean allow_show);
    void scroll(int x, int y);
    void finishManualGesture();
    void autoZoom(int offset_x, int offset_y, boolean zoom_out);
    void manualZoom(float amount);
    void endManualZoom();
}
