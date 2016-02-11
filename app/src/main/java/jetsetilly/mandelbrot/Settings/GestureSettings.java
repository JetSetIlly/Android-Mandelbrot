package jetsetilly.mandelbrot.Settings;

import android.content.Context;
import android.content.SharedPreferences;

public class GestureSettings {
    private final float DEF_DOUBLE_TAP_SCALE = 3.0f;
    public final float DEF_MAX_PINCH_ZOOM_IN = 0.35f;
    public final float DEF_MAX_PINCH_ZOOM_OUT = -0.35f;
    public float double_tap_scale;
    public float max_pinch_zoom_in;
    public float max_pinch_zoom_out;

    GestureSettings() {
    }

    public void save(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor prefs_editor = prefs.edit();
        prefs_editor.putFloat("double_tap_scale", double_tap_scale);
        prefs_editor.putFloat("max_pinch_zoom_in", max_pinch_zoom_in);
        prefs_editor.putFloat("max_pinch_zoom_out", max_pinch_zoom_out);
        prefs_editor.apply();
    }

    public void restore(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        double_tap_scale = prefs.getFloat("double_tap_scale", DEF_DOUBLE_TAP_SCALE);
        max_pinch_zoom_in = prefs.getFloat("max_pinch_zoom_in", DEF_MAX_PINCH_ZOOM_IN);
        max_pinch_zoom_out = prefs.getFloat("max_pinch_zoom_out", DEF_MAX_PINCH_ZOOM_OUT);
    }

    /* singleton pattern */
    private static final GestureSettings singleton = new GestureSettings();
    public static GestureSettings getInstance() {
        return singleton;
    }
    /* end of singleton pattern */
}
