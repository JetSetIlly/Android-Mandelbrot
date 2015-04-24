package jetsetilly.mandelbrot;

import android.content.Context;
import android.database.DataSetObserver;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.lang.reflect.Field;

public class SettingsAdapter implements ListAdapter {
    final static public String DBG_TAG = "settings adapter";

    private SettingsMandelbrot mandelbrot_settings = SettingsMandelbrot.getInstance();
    private SettingsActivity context;

    public SettingsAdapter(SettingsActivity context) {
        this.context = context;
    }

    /* implementation of ListAdapter */
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View rowView = inflater.inflate(R.layout.activity_settings_entry, parent, false);

        // set label
        TextView title = (TextView) rowView.findViewById(R.id.setting_label);
        title.setText(mandelbrot_settings.presentable_settings[position][0]);

        // reflect mandelbrot_settings value onto value TextView
        final TextView value = (TextView) rowView.findViewById(R.id.setting_val);
        final Field f;
        // declared as final because they are accessed by the TextWatcher() instance below

        try {
            f = mandelbrot_settings.getClass().getField(mandelbrot_settings.presentable_settings[position][1]);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            return null;
        }

        try {
            switch (f.getType().toString()) {
                case "int":
                    value.setText("" + f.getInt(mandelbrot_settings));
                    break;

                case "double":
                    value.setText("" + f.getDouble(mandelbrot_settings));
                    break;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }

        // add TextWatcher to update entry in mandelbrot_settings
        value.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                // dirty flag so that SettingsActivity knows to perform a re-render
                context.dirty_settings = true;

                String v = value.getText().toString().trim();
                if (v.equals(""))
                    return;

                try {
                    switch (f.getType().toString()) {
                        case "int":
                            f.setInt(mandelbrot_settings, Integer.parseInt(v));
                            break;

                        case "double":
                            f.setDouble(mandelbrot_settings, Double.parseDouble(v));
                            break;
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });

        return rowView;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public boolean isEmpty() {
        return getCount() == 0;
    }

    public int getCount() {
        return mandelbrot_settings.presentable_settings.length;
    }

    public int getItemViewType(int position)
    {
        return R.layout.activity_settings_entry;
    }

    public void unregisterDataSetObserver(DataSetObserver observer) {
    }

    public void registerDataSetObserver(DataSetObserver observer) {
    }

    public int getViewTypeCount() {
        return 1;
    }

    public Object getItem(int position)
    {
        return mandelbrot_settings.presentable_settings[position];
    }

    public boolean isEnabled(int position) {
        return true;
    }

    public boolean areAllItemsEnabled() {
        return true;
    }
    /* end of implementation of ListAdapter */
}
