package jetsetilly.mandelbrot.Widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import jetsetilly.mandelbrot.Mandelbrot.Settings;

public class IterationsSlider extends LinearLayout {
    private Settings mandelbrot_settings = Settings.getInstance();

    private TextView value;
    private SeekBar slider;

    private int iteration_min;
    private int iteration_max;

    public IterationsSlider(Context context) {
        this(context, null);
    }

    public IterationsSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOrientation(VERTICAL);

        value = new TextView(context);
        slider = new SeekBar(context);
        value.setGravity(Gravity.END);
        this.addView(value);
        this.addView(slider);

        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                value.setText("" + (slider.getProgress() + iteration_min));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        this.set(mandelbrot_settings.max_iterations);
    }

    public int get() {
        return Integer.parseInt(value.getText().toString());
    }

    public void set(int value) {
        iteration_min = (int) (value * 0.25);
        iteration_max = (int) (value * 1.5);
        slider.setMax(iteration_max);
        slider.setProgress(value - iteration_min);
    }

    public void reset() {
        slider.setProgress(mandelbrot_settings.max_iterations - iteration_min);
    }

    public boolean fixate() {
        // copy selected value to mandelbrot_settings.max_iterations
        //
        // return true if value has changed
        //      false if value has not changed

        boolean ret = mandelbrot_settings.max_iterations != this.get();

        mandelbrot_settings.max_iterations = this.get();
        return ret;
    }
}
