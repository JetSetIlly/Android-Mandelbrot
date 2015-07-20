
package jetsetilly.mandelbrot.Widgets;

import android.content.Context;
import android.util.AttributeSet;

import jetsetilly.mandelbrot.Mandelbrot.MandelbrotSettings;

public class BailoutSlider extends ReportingSeekBar {
    private MandelbrotSettings mandelbrot_settings = MandelbrotSettings.getInstance();

    private final int BAILOUT_SCALE = 10;
    private final double BAILOUT_MAX = 32.0;

    public BailoutSlider(Context context) {
        this(context, null);
    }

    public BailoutSlider(Context context, AttributeSet attrs) {
        super(context, attrs);

        set(mandelbrot_settings.bailout_value,
                0,
                BAILOUT_MAX,
                BAILOUT_SCALE
        );
    }

    public boolean fixate() {
        // copy selected value to mandelbrot_settings.max_iterations
        //
        // return true if value has changed
        //      false if value has not changed

        mandelbrot_settings.bailout_value = this.getDouble();
        return hasChanged();
    }
}
