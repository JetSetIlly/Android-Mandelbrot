package jetsetilly.mandelbrot.Widgets;

import android.content.Context;
import android.util.AttributeSet;

import jetsetilly.mandelbrot.Settings.MandelbrotSettings;

public class IterationsSlider extends ReportingSeekBar {
    private final MandelbrotSettings mandelbrot_settings = MandelbrotSettings.getInstance();

    public IterationsSlider(Context context) {
        this(context, null);
    }

    public IterationsSlider(Context context, AttributeSet attrs) {
        super(context, attrs);

        set(mandelbrot_settings.max_iterations,
                (int) (mandelbrot_settings.max_iterations * 0.25),
                (int) (mandelbrot_settings.max_iterations * 1.5)
        );
    }

    public boolean fixate() {
        // copy selected value to mandelbrot_settings.max_iterations
        //
        // return true if value has changed
        //      false if value has not changed

        mandelbrot_settings.max_iterations = this.getInteger();
        return hasChanged();
    }
}
