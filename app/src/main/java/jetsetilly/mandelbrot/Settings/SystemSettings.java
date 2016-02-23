package jetsetilly.mandelbrot.Settings;

import android.content.Context;
import android.content.SharedPreferences;

public class SystemSettings {
    private final boolean DEF_ALLOW_SCREEN_ROTATION = false;
    public boolean allow_screen_rotation;

    SystemSettings() {
    }

    public void save(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor prefs_editor = prefs.edit();
        prefs_editor.putBoolean("allow_screen_rotation", allow_screen_rotation);
        prefs_editor.apply();
    }

    public void restore(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        allow_screen_rotation = prefs.getBoolean("allow_screen_rotation", DEF_ALLOW_SCREEN_ROTATION);
    }

    /* singleton pattern */
    private static final SystemSettings singleton = new SystemSettings();
    public static SystemSettings getInstance() {
        return singleton;
    }
    /* end of singleton pattern */
}
