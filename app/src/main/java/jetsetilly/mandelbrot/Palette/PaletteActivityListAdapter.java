package jetsetilly.mandelbrot.Palette;

import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.R;
import jetsetilly.mandelbrot.Settings.PaletteSettings;
import jetsetilly.mandelbrot.Widgets.CircularImageView;

public class PaletteActivityListAdapter implements ListAdapter {
    private final PaletteSettings palette_settings = PaletteSettings.getInstance();

    private final Context context;
    private final Resources resources;
    private final LayoutInflater inflater;

    public PaletteActivityListAdapter(Context context) {
        super();
        this.context = context;
        this.resources = context.getResources();
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convert_view, ViewGroup parent) {
        final View view;

        if (convert_view == null) {
            view = inflater.inflate(R.layout.activity_palette_entry, parent, false);
        } else {
            view = convert_view;
        }

        ((TextView) view.findViewById(R.id.palette_label)).setText(palette_settings.palettes[position].name);
        ((TextView) view.findViewById(R.id.palette_id)).setText(String.format("%d", position));
        ((CircularImageView) view.findViewById(R.id.palette_swatch)).setImageBitmap(palette_settings.palettes[position].swatch);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView id_v = (TextView) v.findViewById(R.id.palette_id);
                int id = Integer.parseInt((String) id_v.getText());

                palette_settings.setColours(id);
                palette_settings.save(context);
                MainActivity.render_canvas.startRender();
            }
        });

        return view;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return getCount() == 0;
    }

    @Override
    public int getCount() {
        return palette_settings.palettes.length;
    }

    @Override
    public int getItemViewType(int position)
    {
        return R.layout.activity_palette_entry;
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public Object getItem(int position)
    {
        return palette_settings.palettes[position];
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

}
