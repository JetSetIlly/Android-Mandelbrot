package jetsetilly.mandelbrot.Palette;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.ImageView;

import java.util.HashMap;

import jetsetilly.mandelbrot.PaletteActivity;
import jetsetilly.mandelbrot.R;
import jetsetilly.mandelbrot.Settings.Settings;
import jetsetilly.tools.LogTools;

public class PaletteAdapter implements ListAdapter {
    private final String DBG_TAG = "palette list adapter";

    private final PaletteActivity context;
    private final LayoutInflater inflater;

    private View selected_palette;
    private String selected_palette_id;
    private HashMap<String, Bitmap> swatches;

    public PaletteAdapter(Context context) {
        super();
        this.context = (PaletteActivity) context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        selected_palette_id = Settings.getInstance().selected_palette_id;
        swatches = new HashMap();
    }

    public String getSelectedPaletteID() {
        return selected_palette_id;
    }

    @Override
    public View getView(int position, View convert_view, ViewGroup parent) {
        final View view;

        if (convert_view == null) {
            view = inflater.inflate(getItemViewType(position), parent, false);
        } else {
            view = convert_view;
        }

        final Palette.PaletteEntry item = (Palette.PaletteEntry) getItem(position);

        // set label
        ((TextView) view.findViewById(R.id.palette_label)).setText(item.name);

        // get swatch from cache or generate (and put into cache) if necessary
        ImageView swatch = (ImageView) view.findViewById(R.id.palette_swatch);
        if (!swatches.containsKey(item.name)) {
            swatches.put(item.name, Palette.getInstance().generateSwatch(context, item.name));
        }

        // set swatch image
        Bitmap swatch_bm = swatches.get(item.name);
        swatch.setImageBitmap(swatch_bm);

        // tick this view if this is the currently selected palette
        if (item.name.equals(selected_palette_id)) {
            selected_palette = view;
            view.findViewById(R.id.palette_selected_icon).setVisibility(View.VISIBLE);
        } else {
            // set the palette selected icon to invisible if this isn't the selected palette
            view.findViewById(R.id.palette_selected_icon).setVisibility(View.INVISIBLE);
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View palette_entry) {
                // animate selection
                ImageView swatch = (ImageView) palette_entry.findViewById(R.id.palette_swatch);
                swatch.startAnimation(AnimationUtils.loadAnimation(context, R.anim.palette_swatch_click));

                // un-tick palette that was selected
                if (selected_palette != null) {
                    final ImageView old_selected_icon = (ImageView) selected_palette.findViewById(R.id.palette_selected_icon);
                    Animation deselected_anim = AnimationUtils.loadAnimation(context, R.anim.palette_swatch_deselected);
                    deselected_anim.setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            LogTools.printDebug(DBG_TAG, "old selected icon anim start");
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            old_selected_icon.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });

                    old_selected_icon.startAnimation(deselected_anim);
                }

                // tick this palette
                final ImageView new_selected_icon = (ImageView) palette_entry.findViewById(R.id.palette_selected_icon);
                Animation selected_anim = AnimationUtils.loadAnimation(context, R.anim.palette_swatch_selected);
                selected_anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        new_selected_icon.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
                new_selected_icon.startAnimation(selected_anim);

                // note which palette entry this is
                selected_palette = palette_entry;
                selected_palette_id = item.name;
            }
        });

        // i don't like this call but it's required because animations will not always be started
        // in the onClick listener - specifically, the deselect animation will not be run if the
        // position of the selected icon is position 0
        // note that if we don't test for this condition the scrollview can go berserk!
        if (position == 0)
            context.adapter_getView_callback();

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
        return Palette.getInstance().numPalettes();
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
        return Palette.getInstance().getEntry(position);
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
