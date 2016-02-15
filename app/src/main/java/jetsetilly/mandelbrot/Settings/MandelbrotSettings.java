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
    public double bailout_value;
    public int max_iterations;
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

        putDouble(prefs_editor, "real_left", (float) real_left);
        putDouble(prefs_editor, "real_right", (float) real_right);
        putDouble(prefs_editor, "imaginary_upper", (float) imaginary_upper);
        putDouble(prefs_editor, "imaginary_lower", (float) imaginary_lower);
        putDouble(prefs_editor, "bailout_value", (float) bailout_value);
        prefs_editor.putInt("max_iterations", max_iterations);
        prefs_editor.putInt("render_mode", render_mode.ordinal());

        // Commit the edits!
        prefs_editor.apply();
    }

    public void restore(Context context) {
        // set interface to reflect stored values
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);

        real_left = getDouble(prefs, "real_left", (float) MandelbrotPresets.presets[MandelbrotPresets.DEFAULT_SETTINGS].real_left);
        real_right = getDouble(prefs, "real_right", (float) MandelbrotPresets.presets[MandelbrotPresets.DEFAULT_SETTINGS].real_right);
        imaginary_upper = getDouble(prefs, "imaginary_upper", (float) MandelbrotPresets.presets[MandelbrotPresets.DEFAULT_SETTINGS].imaginary_upper);
        imaginary_lower = getDouble(prefs, "imaginary_lower", (float) MandelbrotPresets.presets[MandelbrotPresets.DEFAULT_SETTINGS].imaginary_lower);
        bailout_value = getDouble(prefs, "bailout_value", (float) MandelbrotPresets.presets[MandelbrotPresets.DEFAULT_SETTINGS].bailout_value);
        max_iterations = prefs.getInt("max_iterations", MandelbrotPresets.presets[MandelbrotPresets.DEFAULT_SETTINGS].max_iterations);
        render_mode = Mandelbrot.RenderMode.values()[prefs.getInt("render_mode", Mandelbrot.RenderMode.CENTRE.ordinal())];
    }

    /* double width float support for SharedPreferences */
    SharedPreferences.Editor putDouble(final SharedPreferences.Editor edit, final String key, final double value) {
        return edit.putLong(key, Double.doubleToRawLongBits(value));
    }

    double getDouble(final SharedPreferences prefs, final String key, final double defaultValue) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
    }
    /* end of double width float support for SharedPreferences */

    /* singleton pattern */
    private static final MandelbrotSettings singleton = new MandelbrotSettings();
    public static MandelbrotSettings getInstance() {
        return singleton;
    }
    /* end of singleton pattern */
}
