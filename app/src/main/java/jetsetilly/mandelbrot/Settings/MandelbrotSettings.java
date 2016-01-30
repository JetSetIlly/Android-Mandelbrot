package jetsetilly.mandelbrot.Settings;

import android.content.Context;
import android.content.SharedPreferences;

import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.mandelbrot.Mandelbrot.MandelbrotPresets;

public class MandelbrotSettings {
    public double real_left;
    public double real_right;
    public double imaginary_upper;
    public double imaginary_lower;

    public int max_iterations;
    public double bailout_value;

    public Mandelbrot.RenderMode render_mode;

    MandelbrotSettings() {
        reset();
    }

    public void reset() {
        real_left = MandelbrotPresets.presets[MandelbrotPresets.DEFAULT_SETTINGS].real_left;
        real_right = MandelbrotPresets.presets[MandelbrotPresets.DEFAULT_SETTINGS].real_right;
        imaginary_upper = MandelbrotPresets.presets[MandelbrotPresets.DEFAULT_SETTINGS].imaginary_upper;
        imaginary_lower = MandelbrotPresets.presets[MandelbrotPresets.DEFAULT_SETTINGS].imaginary_lower;
        max_iterations = MandelbrotPresets.presets[MandelbrotPresets.DEFAULT_SETTINGS].max_iterations;
        bailout_value = MandelbrotPresets.presets[MandelbrotPresets.DEFAULT_SETTINGS].bailout_value;
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

        // Commit the edits!
        prefs_editor.apply();
    }

    public void restore(Context context) {
        // set interface to reflect stored values
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        real_left = prefs.getFloat("real_left", (float) MandelbrotPresets.presets[MandelbrotPresets.DEFAULT_SETTINGS].real_left);
        real_right = prefs.getFloat("real_right", (float) MandelbrotPresets.presets[MandelbrotPresets.DEFAULT_SETTINGS].real_right);
        imaginary_upper = prefs.getFloat("imaginary_upper", (float) MandelbrotPresets.presets[MandelbrotPresets.DEFAULT_SETTINGS].imaginary_upper);
        imaginary_lower = prefs.getFloat("imaginary_lower", (float) MandelbrotPresets.presets[MandelbrotPresets.DEFAULT_SETTINGS].imaginary_lower);
        max_iterations = prefs.getInt("max_iterations", MandelbrotPresets.presets[MandelbrotPresets.DEFAULT_SETTINGS].max_iterations);
        bailout_value = prefs.getFloat("bailout_value", (float) MandelbrotPresets.presets[MandelbrotPresets.DEFAULT_SETTINGS].bailout_value);
        render_mode = Mandelbrot.RenderMode.values()[prefs.getInt("render_mode", Mandelbrot.RenderMode.TOP_DOWN.ordinal())];
    }

    /* singleton pattern */
    private static final MandelbrotSettings singleton = new MandelbrotSettings();
    public static MandelbrotSettings getInstance() {
        return singleton;
    }
    /* end of singleton pattern */
}
