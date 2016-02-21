package jetsetilly.mandelbrot.Widgets;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import jetsetilly.mandelbrot.R;

public class ReportingSeekBar extends LinearLayout {
    private final TextView label;
    private final TextView value;
    private final SeekBar slider;

    // start_progress is the raw start value of the SeekBar
    private int start_progress;

    private int start_value;
    private int base_value;
    private int scale;

    public ReportingSeekBar(Context context) {
        this(context, null);
    }

    public ReportingSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources resources = context.getResources();
        this.setOrientation(VERTICAL);

        label = new TextView(context);
        this.addView(label);

        value = new TextView(context);
        value.setGravity(Gravity.END);
        this.addView(value);

        slider = new SeekBar(context);
        this.addView(slider);

        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (scale == 1) {
                    value.setText(translateValue(slider.getProgress() + base_value));
                } else {
                    value.setText(translateValue((double) (slider.getProgress() + base_value) / scale));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        // set attributes
        TypedArray s_attrs = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ReportingSeekBar, 0, 0);
        try {
            label.setText(s_attrs.getString(R.styleable.ReportingSeekBar_label));
            if (label.getText() == "") {
                label.setVisibility(GONE);
            } else {
                label.setTextAppearance(context, s_attrs.getResourceId(R.styleable.ReportingSeekBar_label_appearance, 0));
            }

            int scale = s_attrs.getInteger(R.styleable.ReportingSeekBar_value_scale, 1);
            float min = s_attrs.getFloat(R.styleable.ReportingSeekBar_value_min, 0);
            float max = s_attrs.getFloat(R.styleable.ReportingSeekBar_value_max, 100);
            float val = s_attrs.getFloat(R.styleable.ReportingSeekBar_value_initial, min);
            set(val, min, max, scale);

            int thumb_width = s_attrs.getDimensionPixelSize(R.styleable.ReportingSeekBar_thumb_size,
                    getResources().getDimensionPixelSize(R.dimen.seekbar_thumb_width));

            // add thumb to slider
            ShapeDrawable thumb = new ShapeDrawable(new OvalShape());
            thumb.setIntrinsicHeight(resources.getDimensionPixelSize(R.dimen.seekbar_thumb_height));
            thumb.setIntrinsicWidth(thumb_width);
            thumb.getPaint().setColor(resources.getColor(R.color.seekbar_thumb_colour));
            slider.setThumb(thumb);

        } finally {
            s_attrs.recycle();
        }
    }

    public int getProgress() {
        // return the actual value of the slide
        return slider.getProgress();
    }

    public int getInteger() {
        // return the process integer value
        return Integer.parseInt(value.getText().toString());
    }

    public double getDouble() {
        // return the process double value
        return Double.parseDouble(value.getText().toString());
    }

    public float getFloat() {
        // return the process float value
        return Float.parseFloat(value.getText().toString());
    }

    public void set(int val, int min, int max) {
        set(val, min, max, 1);
    }

    public void set(double val, double min, double max, int scale) {
        this.scale = scale;
        start_progress = (int) val;
        start_value = (int) (val * scale);
        base_value = (int) (min * scale);
        slider.setMax((int) (max * scale) - base_value);
        this.set(start_value);
    }

    public void set(int val) {
        slider.setProgress(val - base_value);
    }

    public void set(double val) {
        slider.setProgress((int) (val * scale) - base_value);
    }

    public void set(float val) {
        set((double) val);
    }

    public boolean hasChanged() {
        return start_progress != this.getProgress();
    }

    public String translateValue(Object value) {
        return String.valueOf(value);
    }
}
