package jetsetilly.mandelbrot.Widgets;

import android.content.Context;
import android.util.AttributeSet;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;

public class NumPassesSlider extends ReportingSeekBar {
    private final MandelbrotSettings mandelbrotSettings = MandelbrotSettings.getInstance();

    private final int MIN_SCALE_VAL = 1;
    private final int MAX_SCALE_VAL = 4;

    public NumPassesSlider(Context context) {
        this(context, null);
    }

    public NumPassesSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        set((int) mandelbrotSettings.num_passes, MIN_SCALE_VAL, MAX_SCALE_VAL);
    }

    public boolean fixate() {
        // copy selected value to canvas_settings.max_iterations
        //
        // return true if value has changed
        //      false if value has not changed

        mandelbrotSettings.num_passes = this.getInteger();
        return hasChanged();
    }
}
