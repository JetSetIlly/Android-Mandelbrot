package jetsetilly.mandelbrot.Mandelbrot;

import android.content.Context;
import android.content.SharedPreferences;

public class MandelbrotSettings {
    public double real_left;
    public double real_right;
    public double imaginary_upper;
    public double imaginary_lower;

    public int max_iterations;
    public double bailout_value;

    public Mandelbrot.RenderMode render_mode;
    public boolean parallel_render;

    MandelbrotSettings() {
        resetCoords();
    }

    public void resetCoords() {
        real_left = Presets.presets[Presets.DEFAULT_SETTINGS].real_left;
        real_right = Presets.presets[Presets.DEFAULT_SETTINGS].real_right;
        imaginary_upper = Presets.presets[Presets.DEFAULT_SETTINGS].imaginary_upper;
        imaginary_lower = Presets.presets[Presets.DEFAULT_SETTINGS].imaginary_lower;
        max_iterations = Presets.presets[Presets.DEFAULT_SETTINGS].max_iterations;
        bailout_value = Presets.presets[Presets.DEFAULT_SETTINGS].bailout_value;
    }

    public void save(Context context) {
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor prefs_editor = prefs.edit();

        prefs_editor.putFloat("real_left", (float) real_left);
        prefs_editor.putFloat("real_right", (float) real_right);
        prefs_editor.putFloat("imaginary_upper", (float) imaginary_upper);
        prefs_editor.putFloat("imaginary_lower", (float) imaginary_lower);
        prefs_editor.putInt("max_iterations", max_iterations);
        prefs_editor.putFloat("bailout_value", (float) bailout_value);
        prefs_editor.putInt("render_mode", render_mode.ordinal());
        prefs_editor.putBoolean("parallel_render", parallel_render);

        // Commit the edits!
        prefs_editor.apply();
    }

    public void restore(Context context) {
        // set interface to reflect stored values
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        real_left = prefs.getFloat("real_left", (float) Presets.presets[Presets.DEFAULT_SETTINGS].real_left);
        real_right = prefs.getFloat("real_right", (float) Presets.presets[Presets.DEFAULT_SETTINGS].real_right);
        imaginary_upper = prefs.getFloat("imaginary_upper", (float) Presets.presets[Presets.DEFAULT_SETTINGS].imaginary_upper);
        imaginary_lower = prefs.getFloat("imaginary_lower", (float) Presets.presets[Presets.DEFAULT_SETTINGS].imaginary_lower);
        max_iterations = prefs.getInt("max_iterations", Presets.presets[Presets.DEFAULT_SETTINGS].max_iterations);
        bailout_value = prefs.getFloat("bailout_value", (float) Presets.presets[Presets.DEFAULT_SETTINGS].bailout_value);
        render_mode = Mandelbrot.RenderMode.values()[prefs.getInt("render_mode", Mandelbrot.RenderMode.TOP_DOWN.ordinal())];
        parallel_render = prefs.getBoolean("parallel_render", false);
    }

    /* singleton pattern */
    private static MandelbrotSettings singleton = new MandelbrotSettings();
    public static MandelbrotSettings getInstance() {
        return singleton;
    }
    /* end of singleton pattern */
}
