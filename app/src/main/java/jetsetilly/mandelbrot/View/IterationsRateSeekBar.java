package jetsetilly.mandelbrot.View;

import android.content.Context;
import android.util.AttributeSet;

import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.mandelbrot.R;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;

public class IterationsRateSeekBar extends ReportingSeekBar {
    private final MandelbrotSettings mandelbrot_settings = MandelbrotSettings.getInstance();

    public IterationsRateSeekBar(Context context) {
        this(context, null);
        reset();
    }

    public IterationsRateSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        reset();
    }

    public void reset() {
        set(mandelbrot_settings.iterations_rate, 0, Mandelbrot.IterationsRate.COUNT-1);
    }

    @Override
    public String translateValue(Object value) {
        if (value == Mandelbrot.IterationsRate.FAST)
            return getResources().getString(R.string.settings_iterations_rate_fast);
        else if (value == Mandelbrot.IterationsRate.NORMAL)
            return getResources().getString(R.string.settings_iterations_rate_normal);
        else if (value == Mandelbrot.IterationsRate.SLOW)
            return getResources().getString(R.string.settings_iterations_rate_slow);

        return getResources().getString(R.string.error_ui_alert);
    }
}
