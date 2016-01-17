package jetsetilly.mandelbrot.Palette;

import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.ImageView;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.R;
import jetsetilly.mandelbrot.Settings.PaletteSettings;

public class PaletteActivityListAdapter implements ListAdapter {
    private final PaletteSettings palette_settings = PaletteSettings.getInstance();

    private final Context context;
    private final LayoutInflater inflater;

    private View selected_palette;

    public PaletteActivityListAdapter(Context context) {
        super();
        this.context = context;
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
        ((ImageView) view.findViewById(R.id.palette_swatch)).setImageBitmap(palette_settings.palettes[position].swatch);

        // tick this view if this is the currently selected palette
        if (position == palette_settings.selected_id) {
            selected_palette = view;
            selected_palette.findViewById(R.id.palette_selected).setVisibility(View.VISIBLE);
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView id_v = (TextView) v.findViewById(R.id.palette_id);
                int id = Integer.parseInt((String) id_v.getText());

                palette_settings.setColours(id);
                palette_settings.save(context);
                MainActivity.render_canvas.startRender();

                // animate selection
                ImageView swatch = (ImageView) v.findViewById(R.id.palette_swatch);
                swatch.startAnimation(AnimationUtils.loadAnimation(context, R.anim.palette_swatch_click));

                // un-tick palette that was selected
                final ImageView old_selected = (ImageView) selected_palette.findViewById(R.id.palette_selected);
                Animation deselected_anim = AnimationUtils.loadAnimation(context, R.anim.palette_swatch_deselected);
                deselected_anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        old_selected.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                old_selected.startAnimation(deselected_anim);

                // tick this palette
                final ImageView new_selected = (ImageView) v.findViewById(R.id.palette_selected);
                Animation selected_anim = AnimationUtils.loadAnimation(context, R.anim.palette_swatch_selected);
                selected_anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        new_selected.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                new_selected.startAnimation(selected_anim);

                // note which palette entry this is
                selected_palette = v;
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
