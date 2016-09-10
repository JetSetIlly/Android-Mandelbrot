package jetsetilly.mandelbrot.Settings;

import android.content.Context;
import android.content.SharedPreferences;

import jetsetilly.tools.LogTools;
import jetsetilly.tools.SimpleAsyncTask;

public class MandelbrotCoordinates {
    public double real_left;
    public double real_right;
    public double imaginary_upper;
    public double imaginary_lower;
    public double bailout_value;
    public int max_iterations;

    private String prefsName(Context context) {
        return context.getPackageName() + "_coordinates";
    }

    public void restoreDefaults(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(prefsName(context), Context.MODE_PRIVATE);
        SharedPreferences.Editor prefs_editor = prefs.edit();
        prefs_editor.clear();
        prefs_editor.apply();
        restore(context);
    }

    public void save(final Context context) {
        new SimpleAsyncTask("save coords", new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = context.getSharedPreferences(prefsName(context), Context.MODE_PRIVATE);
                SharedPreferences.Editor prefs_editor = prefs.edit();

                putDouble(prefs_editor, "real_left", real_left);
                putDouble(prefs_editor, "real_right", real_right);
                putDouble(prefs_editor, "imaginary_upper", imaginary_upper);
                putDouble(prefs_editor, "imaginary_lower", imaginary_lower);
                putDouble(prefs_editor, "bailout_value", bailout_value);
                prefs_editor.putInt("max_iterations", max_iterations);

                prefs_editor.apply();
            }
        });
    }

    public void restore(final Context context) {
        new SimpleAsyncTask("load coords", new Runnable() {
            @Override
            public void run() {
                // set interface to reflect stored values
                SharedPreferences prefs = context.getSharedPreferences(prefsName(context), Context.MODE_PRIVATE);
                real_left = getDouble(prefs, "real_left", -2.11);
                real_right = getDouble(prefs, "real_right", 1.16);
                imaginary_upper = getDouble(prefs, "imaginary_upper", 2.94);
                imaginary_lower = getDouble(prefs, "imaginary_lower", -2.88);
                bailout_value = getDouble(prefs, "bailout_value", 4.0);
                max_iterations = prefs.getInt("max_iterations", 60);
            }
        });
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
    private static final MandelbrotCoordinates singleton = new MandelbrotCoordinates();
    public static MandelbrotCoordinates getInstance() {
        return singleton;
    }
    /* end of singleton pattern */
}
