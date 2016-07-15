package jetsetilly.mandelbrot.Gestures;

public interface GestureHandler {
    void checkActionBar(float x, float y, boolean show);
    void scroll(int x, int y);
    void animatedZoom(int offset_x, int offset_y);
    void pinchZoom(float amount);
    void zoomCorrection(boolean force);

    // shared with MandelbrotCanvas interface
    int getCanvasWidth();
    int getCanvasHeight();

    // shared with RenderCanvas interface
    void startRender();
}
