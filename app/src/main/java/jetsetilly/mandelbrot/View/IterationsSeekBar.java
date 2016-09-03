package jetsetilly.mandelbrot.View;

import android.content.Context;
import android.util.AttributeSet;

import jetsetilly.mandelbrot.Settings.MandelbrotCoordinates;

public class IterationsSeekBar extends ReportingSeekBar {
    private final MandelbrotCoordinates mandelbrot_coordinates = MandelbrotCoordinates.getInstance().getInstance();

    public IterationsSeekBar(Context context) {
        this(context, null);
    }

    public IterationsSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        set(mandelbrot_coordinates.max_iterations,
                (int) (mandelbrot_coordinates.max_iterations * 0.25),
                (int) (mandelbrot_coordinates.max_iterations * 1.5)
        );
    }
}
