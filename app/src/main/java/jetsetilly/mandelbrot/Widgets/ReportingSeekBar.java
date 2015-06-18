package jetsetilly.mandelbrot.Widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import jetsetilly.mandelbrot.Mandelbrot.Settings;

public class ReportingSeekBar extends LinearLayout {
    private TextView value;
    private SeekBar slider;

    private int start_value;
    private int base_value;
    private int scale;

    public ReportingSeekBar(Context context) {
        this(context, null);
    }

    public ReportingSeekBar(Context context, AttributeSet attrs) {
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
                if (scale == 1) {
                    value.setText("" + (slider.getProgress() + base_value));
                } else {
                    value.setText("" + (double) (slider.getProgress() + base_value) / scale);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    public int getInteger() {
        return Integer.parseInt(value.getText().toString());
    }

    public double getDouble() {
        return Double.parseDouble(value.getText().toString());
    }

    public void set(int val, int min, int max) {
        set(val, min, max, 1);
    }

    public void set(double val, double min, double max, int scale) {
        this.scale = scale;
        start_value = (int) (val * scale);
        base_value = (int) (min * scale);
        slider.setMax((int) (max * scale));
        this.set(start_value);
        }

    public void set(int val) {
        slider.setProgress(val - base_value);
    }

    public void reset() {
        slider.setProgress(start_value - base_value);
    }

    public boolean hasChanged() {
        if (scale == 1) {
            return start_value != this.getInteger();
        } else {
            return start_value != (int) this.getDouble() * scale;
        }
    }
}
