package jetsetilly.mandelbrot.Palette;

import android.content.Context;
import android.content.res.Resources;
import android.database.DataSetObserver;
import android.support.v7.widget.CardView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

import jetsetilly.mandelbrot.ColoursActivity;
import jetsetilly.mandelbrot.R;

public class PaletteListAdapter implements ListAdapter {
    final static public String DBG_TAG = "colours adapter";
    final static int MAX_COLOURS_TO_PREVIEW = 128;

    private PaletteSettings palette_settings = PaletteSettings.getInstance();
    private ColoursActivity context;

    // z heights for selected/unselected cards
    private float selected_z;
    private float unselected_z;

    // colors for selected/unselected cards
    private int selected_color;
    private int unselected_color;

    // currently selected row view - used to unset a previously selected card
    private View selected_row_view = null;


    public PaletteListAdapter(ColoursActivity context) {
        super();

        this.context = context;

        Resources rs =  context.getResources();

        selected_z = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                rs.getDimension(R.dimen.palette_activity_selected_card),
                rs.getDisplayMetrics());

        unselected_z = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                rs.getDimension(R.dimen.palette_activity_unselected_card),
                rs.getDisplayMetrics());

        selected_color = rs.getColor(R.color.palette_activity_selected_card);
        unselected_color = rs.getColor(R.color.palette_activity_unselected_card);
    }

    /* implementation of ListAdapter */
    public View getView(final int position, View convert_view, ViewGroup parent) {
        final View row_view;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convert_view == null) {
            row_view = inflater.inflate(R.layout.activity_colours_palette_preview, parent, false);
        } else {
            row_view = convert_view;
        }

        // set title
        TextView title = (TextView) row_view.findViewById(R.id.palette_title);
        title.setText(palette_settings.palettes[position].name + "  (" + palette_settings.numColors(position) + ")");

        final ImageView iv = (ImageView) row_view.findViewById(R.id.palette_preview);
        // setting image bitmap immediately seems to work without queueing it
        // by calling iv.post()
        iv.setImageBitmap(palette_settings.palettes[position].preview_bm);

        // make sure we unset the palette card in case this row_view is being
        // reused -- we'll set it again below if necessary
        unsetPaletteCard(row_view);

        // set palette card
        if (position == palette_settings.selected_id) {
            setPaletteCard(row_view);
        }

        return row_view;
    }

    public void unsetPaletteCard(View row_view) {
        CardView cv;

        cv = (CardView) row_view.findViewById(R.id.palette_card);
        cv.setTranslationZ(unselected_z);
        cv.setCardBackgroundColor(unselected_color);
    }

    public void setPaletteCard(View row_view) {
        CardView cv;

        if (selected_row_view != null) {
            unsetPaletteCard(selected_row_view);
        }

        cv = (CardView) row_view.findViewById(R.id.palette_card);
        cv.setTranslationZ(selected_z);
        cv.setCardBackgroundColor(selected_color);

        selected_row_view = row_view;
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
        return palette_settings.palettes.length;
    }

    public int getItemViewType(int position)
    {
        return R.layout.activity_colours_palette_preview;
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
        return palette_settings.palettes[position];
    }

    public boolean isEnabled(int position) {
        return true;
    }

    public boolean areAllItemsEnabled() {
        return true;
    }
    /* end of implementation of ListAdapter */
}
