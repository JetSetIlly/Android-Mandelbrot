package jetsetilly.mandelbrot.Widgets;

import android.content.Context;
import android.util.AttributeSet;

import jetsetilly.mandelbrot.Settings.GestureSettings;
import jetsetilly.mandelbrot.Widgets.ReportingSeekBar;

public class DoubleTapScaleSlider extends ReportingSeekBar {
    private final GestureSettings canvas_settings = GestureSettings.getInstance();

    private final int MIN_SCALE_VAL = 1;
    private final int MAX_SCALE_VAL = 5;

    public DoubleTapScaleSlider(Context context) {
        this(context, null);
    }

    public DoubleTapScaleSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        set((int) canvas_settings.double_tap_scale, MIN_SCALE_VAL, MAX_SCALE_VAL);
    }

    public boolean fixate() {
        // copy selected value to canvas_settings.max_iterations
        //
        // return true if value has changed
        //      false if value has not changed

        canvas_settings.double_tap_scale = (float) this.getInteger();
        return hasChanged();
    }
}
