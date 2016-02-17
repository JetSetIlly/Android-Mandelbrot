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

        ShapeDrawable thumb = new ShapeDrawable(new OvalShape());
        thumb.setIntrinsicHeight(resources.getDimensionPixelSize(R.dimen.seekbar_thumb_height));
        thumb.setIntrinsicWidth(resources.getDimensionPixelSize(R.dimen.seekbar_thumb_width));
        thumb.getPaint().setColor(resources.getColor(R.color.seekbar_thumb_colour));

        slider = new SeekBar(context);
        slider.setThumb(thumb);
        this.addView(slider);

        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (scale == 1) {
                    value.setText(String.format("%d", slider.getProgress() + base_value));
                } else {
                    value.setText(String.format("%s", (double) (slider.getProgress() + base_value) / scale));
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

            final int scale = s_attrs.getInteger(R.styleable.ReportingSeekBar_value_scale, 1);
            final float min = s_attrs.getFloat(R.styleable.ReportingSeekBar_value_min, 0);
            final float max = s_attrs.getFloat(R.styleable.ReportingSeekBar_value_max, 100);
            final float val = s_attrs.getFloat(R.styleable.ReportingSeekBar_value_initial, min);
            set(val, min, max, scale);
        } finally {
            s_attrs.recycle();
        }
    }

    public int getInteger() {
        return Integer.parseInt(value.getText().toString());
    }

    public double getDouble() {
        return Double.parseDouble(value.getText().toString());
    }

    public float getFloat() {
        return Float.parseFloat(value.getText().toString());
    }

    public void set(int val, int min, int max) {
        set(val, min, max, 1);
    }

    public void set(double val, double min, double max, int scale) {
        this.scale = scale;
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
        set((double)val);
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
