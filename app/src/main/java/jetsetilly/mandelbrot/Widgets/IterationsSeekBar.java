package jetsetilly.mandelbrot.Widgets;

import android.content.Context;
import android.util.AttributeSet;

import jetsetilly.mandelbrot.Settings.MandelbrotSettings;

public class IterationsSeekBar extends ReportingSeekBar {
    private final MandelbrotSettings mandelbrot_settings = MandelbrotSettings.getInstance();

    public IterationsSeekBar(Context context) {
        this(context, null);
    }

    public IterationsSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        set(mandelbrot_settings.max_iterations,
                (int) (mandelbrot_settings.max_iterations * 0.25),
                (int) (mandelbrot_settings.max_iterations * 1.5)
        );
    }
}
