package jetsetilly.mandelbrot.RenderCanvas;

public class GestureSettings {
    public float double_tap_scale = 3.0f;

    GestureSettings() {
    }

    /* singleton pattern */
    private static GestureSettings singleton = new GestureSettings();
    public static GestureSettings getInstance() {
        return singleton;
    }
    /* end of singleton pattern */
}
