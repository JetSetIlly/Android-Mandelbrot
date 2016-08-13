package jetsetilly.mandelbrot.Settings;

import android.content.Context;
import android.content.SharedPreferences;

import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.mandelbrot.Mandelbrot.Bookmarks;

public class MandelbrotSettings {
    private final int DEF_NUM_PASSES = 2; // in lines

    public double real_left;
    public double real_right;
    public double imaginary_upper;
    public double imaginary_lower;
    public double bailout_value;
    public int max_iterations;
    public Mandelbrot.IterationsRate iterations_rate;
    public Mandelbrot.RenderMode render_mode;
    public int num_passes;
    public Mandelbrot.CalculationMethod calculation_method;

    public void reset() {
        real_left = Bookmarks.presets[Bookmarks.DEFAULT_SETTINGS].real_left;
        real_right = Bookmarks.presets[Bookmarks.DEFAULT_SETTINGS].real_right;
        imaginary_upper = Bookmarks.presets[Bookmarks.DEFAULT_SETTINGS].imaginary_upper;
        imaginary_lower = Bookmarks.presets[Bookmarks.DEFAULT_SETTINGS].imaginary_lower;
        bailout_value = Bookmarks.presets[Bookmarks.DEFAULT_SETTINGS].bailout_value;
        max_iterations = Bookmarks.presets[Bookmarks.DEFAULT_SETTINGS].max_iterations;
        calculation_method = Mandelbrot.CalculationMethod.NATIVE;
    }

    public void save(Context context) {
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor prefs_editor = prefs.edit();

        putDouble(prefs_editor, "real_left", real_left);
        putDouble(prefs_editor, "real_right", real_right);
        putDouble(prefs_editor, "imaginary_upper", imaginary_upper);
        putDouble(prefs_editor, "imaginary_lower", imaginary_lower);
        putDouble(prefs_editor, "bailout_value", bailout_value);
        prefs_editor.putInt("max_iterations", max_iterations);
        prefs_editor.putInt("iterations_rate", iterations_rate.ordinal());
        prefs_editor.putInt("render_mode", render_mode.ordinal());
        prefs_editor.putInt("num_passes", num_passes);
        prefs_editor.putInt("calculation_method", calculation_method.ordinal());

        // Commit the edits!
        prefs_editor.apply();
    }

    public void restore(Context context) {
        // set interface to reflect stored values
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);

        real_left = getDouble(prefs, "real_left", Bookmarks.presets[Bookmarks.DEFAULT_SETTINGS].real_left);
        real_right = getDouble(prefs, "real_right", Bookmarks.presets[Bookmarks.DEFAULT_SETTINGS].real_right);
        imaginary_upper = getDouble(prefs, "imaginary_upper", Bookmarks.presets[Bookmarks.DEFAULT_SETTINGS].imaginary_upper);
        imaginary_lower = getDouble(prefs, "imaginary_lower", Bookmarks.presets[Bookmarks.DEFAULT_SETTINGS].imaginary_lower);
        bailout_value = getDouble(prefs, "bailout_value", Bookmarks.presets[Bookmarks.DEFAULT_SETTINGS].bailout_value);
        max_iterations = prefs.getInt("max_iterations", Bookmarks.presets[Bookmarks.DEFAULT_SETTINGS].max_iterations);
        iterations_rate = Mandelbrot.IterationsRate.values()[prefs.getInt("iterations_rate", Mandelbrot.IterationsRate.NORMAL.ordinal())];
        render_mode = Mandelbrot.RenderMode.values()[prefs.getInt("render_mode", Mandelbrot.RenderMode.SOFTWARE_CENTRE.ordinal())];
        num_passes = prefs.getInt("num_passes", DEF_NUM_PASSES);
        calculation_method = Mandelbrot.CalculationMethod.values()[prefs.getInt("calculation_method", Mandelbrot.CalculationMethod.NATIVE.ordinal())];
    }

    /* double width float support for SharedPreferences */
    private SharedPreferences.Editor putDouble(final SharedPreferences.Editor edit, final String key, final double value) {
        return edit.putLong(key, Double.doubleToRawLongBits(value));
    }

    private double getDouble(final SharedPreferences prefs, final String key, final double defaultValue) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToRawLongBits(defaultValue)));
    }
    /* end of double width float support for SharedPreferences */

    /* singleton pattern */
    private static final MandelbrotSettings singleton = new MandelbrotSettings();
    public static MandelbrotSettings getInstance() {
        return singleton;
    }
    /* end of singleton pattern */
}
