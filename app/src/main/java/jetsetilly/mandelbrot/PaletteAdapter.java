package jetsetilly.mandelbrot;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class PaletteAdapter implements ListAdapter {
    final static public String DBG_TAG = "colours adapter";
    final static int MAX_COLOURS_TO_PREVIEW = 128;

    private PaletteControl palette_settings = PaletteControl.getInstance();
    private PaletteActivity context;

    public PaletteAdapter(PaletteActivity context) {
        this.context = context;
    }

    /* implementation of ListAdapter */
    public View getView(final int position, View convertView, ViewGroup parent) {
        View rowView;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            rowView = inflater.inflate(R.layout.activity_palette_entry, parent, false);
        } else {
            rowView = convertView;
        }

        // set title
        TextView title = (TextView) rowView.findViewById(R.id.palette_title);
        title.setText(palette_settings.palettes[position].name + "  (" + palette_settings.numColors(position) + ")");

        // defer drawing of paint colours preview until such time ImageView is fully initialised
        // TODO: put this into an AsyncTask?
        final ImageView iv = (ImageView) rowView.findViewById(R.id.palette_preview);
        iv.post(new Runnable() {
            public void run() {
                paintPalettePreview(iv, position);
            }
        });

        return rowView;
    }

    private void paintPalettePreview(ImageView iv, int position) {
        Bitmap bm = Bitmap.createBitmap(iv.getMeasuredWidth(), iv.getMeasuredHeight(), Bitmap.Config.RGB_565);
        Canvas cnv = new Canvas(bm);
        Paint pnt = new Paint();
        int num_colours = Math.min(MAX_COLOURS_TO_PREVIEW, palette_settings.numColors(position));
        int stripe_width = Math.max(1, bm.getWidth() / num_colours);
        float lft = 0;

        // one stripe per colour
        for (int i = 0; i < num_colours; ++ i) {
            lft = (float) i * stripe_width;

            pnt.setColor(palette_settings.palettes[position].colours[i]);
            cnv.drawRect(lft, 0, lft + stripe_width, iv.getHeight(), pnt);
        }
        // widen the last colour to make sure all the entire width of the bitmap is used
        cnv.drawRect(lft, 0, iv.getWidth(), iv.getHeight(), pnt);

        // attach bitmap to image view
        iv.setImageBitmap(bm);
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
        return R.layout.activity_palette_entry;
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
