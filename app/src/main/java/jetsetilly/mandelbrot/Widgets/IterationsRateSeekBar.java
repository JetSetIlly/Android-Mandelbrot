package jetsetilly.mandelbrot.Widgets;

import android.content.Context;
import android.util.AttributeSet;

import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.mandelbrot.R;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;

public class IterationsRateSeekBar extends ReportingSeekBar {
    private final MandelbrotSettings mandelbrot_settings = MandelbrotSettings.getInstance();

    public IterationsRateSeekBar(Context context) {
        this(context, null);
    }

    public IterationsRateSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        reset();
    }

    public void reset() {
        set(mandelbrot_settings.iterations_rate.ordinal(), 0, Mandelbrot.IterationsRate.values().length-1);
    }

    @Override
    public String translateValue(Object value) {
        if (value == Mandelbrot.IterationsRate.FAST.ordinal())
            return getResources().getString(R.string.settings_iterations_rate_fast);
        else if (value == Mandelbrot.IterationsRate.NORMAL.ordinal())
            return getResources().getString(R.string.settings_iterations_rate_normal);
        else if (value == Mandelbrot.IterationsRate.SLOW.ordinal())
            return getResources().getString(R.string.settings_iterations_rate_slow);

        return getResources().getString(R.string.error_ui_alert);
    }
}
