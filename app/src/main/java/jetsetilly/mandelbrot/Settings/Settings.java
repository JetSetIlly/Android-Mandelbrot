package jetsetilly.mandelbrot.Settings;

import android.content.Context;
import android.content.SharedPreferences;

import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.tools.SimpleAsyncTask;

public class Settings {
    /* Mandelbrot Settings */
    @Mandelbrot.IterationsRate public int iterations_rate;
    @Mandelbrot.RenderMode public int render_mode;
    public int num_passes;
    @Mandelbrot.CalculationMethod public int calculation_method;

    /* UI Settings */
        // DOUBLE_TAP_SCALE expressed in terms of image_scale
    public float double_tap_scale;
        // PINCH_ZOOM values expressed in terms of fractal_scale
    public float max_pinch_zoom_in;
    public float max_pinch_zoom_out;

        // maximum value of cumulative_scale allowed before zoom is paused
    public float max_cumulative_scale;

    /* Palette Settings */
    public String selected_palette_id;
    public int palette_smoothness;

    /* System Settings */
    public boolean allow_screen_rotation;
    public boolean deep_colour;

    /* Defaults */
    private static final int DEF_NUM_PASSES = 2;
    private static final float DEF_DOUBLE_TAP_SCALE = 3.0f;
    private static final float DEF_MAX_PINCH_ZOOM_IN = 2.0f;
    private static final float DEF_MAX_PINCH_ZOOM_OUT = 0.5f;
    private static final float DEF_MAX_CUMULATIVE_SCALE = 20.0f;
    private static final String DEF_SELECTED_PALETTE_ID = "";
    public static final int DEF_PALETTE_SMOOTHNESS = 4;
    private static final boolean DEF_ALLOW_SCREEN_ROTATION = false;
    private static final boolean DEF_DEEP_COLOUR = true;

    private String prefsName(Context context) {
        return context.getPackageName() + "_settings";
    }

    public void restoreDefaults(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName(context), Context.MODE_PRIVATE);
        SharedPreferences.Editor prefs_editor = prefs.edit();
        prefs_editor.clear();
        prefs_editor.apply();
        restore(context, false);
    }

    public void save(final Context context) {
        new SimpleAsyncTask("Save Settings", new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = context.getSharedPreferences(prefsName(context), Context.MODE_PRIVATE);
                SharedPreferences.Editor prefs_editor = prefs.edit();

                // Mandelbrot Settings
                prefs_editor.putInt("iterations_rate", iterations_rate);
                prefs_editor.putInt("render_mode", render_mode);
                prefs_editor.putInt("num_passes", num_passes);
                prefs_editor.putInt("calculation_method", calculation_method);

                // Gesture Settings
                prefs_editor.putFloat("double_tap_scale", double_tap_scale);
                prefs_editor.putFloat("max_pinch_zoom_in", max_pinch_zoom_in);
                prefs_editor.putFloat("max_pinch_zoom_out", max_pinch_zoom_out);
                prefs_editor.putFloat("max_cumulative_scale", max_cumulative_scale);

                // Palette Settings
                prefs_editor.putString("selected_palette_id", selected_palette_id);
                prefs_editor.putInt("palette_smoothness", palette_smoothness);

                // System Settings
                prefs_editor.putBoolean("allow_screen_rotation", allow_screen_rotation);
                prefs_editor.putBoolean("deep_colour", deep_colour);

                prefs_editor.apply();
            }
        });
    }

    public void restore(final Context context, boolean background) {
        if (background) {
            new SimpleAsyncTask("Load Settings", new Runnable() {
                @Override
                public void run() {
                    restore_immediate(context);
                }
            });
        } else {
            restore_immediate(context);
        }
    }

    @SuppressWarnings("WrongConstant")
    private void restore_immediate(Context context) {
        // set interface to reflect stored values
        SharedPreferences prefs = context.getSharedPreferences(prefsName(context), Context.MODE_PRIVATE);

        // Mandelbrot Settings
        iterations_rate = prefs.getInt("iterations_rate", Mandelbrot.IterationsRate.NORMAL);
        render_mode = prefs.getInt("render_mode", Mandelbrot.RenderMode.SOFTWARE_CENTRE);
        num_passes = prefs.getInt("num_passes", DEF_NUM_PASSES);
        calculation_method = prefs.getInt("calculation_method", Mandelbrot.CalculationMethod.NATIVE);

        // Gesture Settings
        double_tap_scale = prefs.getFloat("double_tap_scale", DEF_DOUBLE_TAP_SCALE);
        max_pinch_zoom_in = prefs.getFloat("max_pinch_zoom_in", DEF_MAX_PINCH_ZOOM_IN);
        max_pinch_zoom_out = prefs.getFloat("max_pinch_zoom_out", DEF_MAX_PINCH_ZOOM_OUT);
        max_cumulative_scale = prefs.getFloat("max_cumulative_scale", DEF_MAX_CUMULATIVE_SCALE);

        // Palette Settings
        selected_palette_id = prefs.getString("selected_palette_id", DEF_SELECTED_PALETTE_ID);
        palette_smoothness = prefs.getInt("palette_smoothness", DEF_PALETTE_SMOOTHNESS);

        // System Settings
        allow_screen_rotation = prefs.getBoolean("allow_screen_rotation", DEF_ALLOW_SCREEN_ROTATION);
        deep_colour = prefs.getBoolean("deep_colour", DEF_DEEP_COLOUR);
    }

    /* singleton pattern */
    private static final Settings singleton = new Settings();
    public static Settings getInstance() {
        return singleton;
    }
    /* end of singleton pattern */
}
