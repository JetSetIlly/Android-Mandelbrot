package jetsetilly.mandelbrot.Settings;

import android.content.Context;
import android.content.SharedPreferences;

public class GestureSettings {
    private final float DEF_DOUBLE_TAP_SCALE = 3.0f;
    public float double_tap_scale = DEF_DOUBLE_TAP_SCALE;

    GestureSettings() {
    }

    public void save(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor prefs_editor = prefs.edit();
        prefs_editor.putFloat("double_tap_scale", double_tap_scale);
        prefs_editor.apply();
    }

    public void restore(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        double_tap_scale = prefs.getFloat("double_tap_scale", DEF_DOUBLE_TAP_SCALE);
    }

    /* singleton pattern */
    private static final GestureSettings singleton = new GestureSettings();
    public static GestureSettings getInstance() {
        return singleton;
    }
    /* end of singleton pattern */
}
