package jetsetilly.mandelbrot.Palette;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v7.graphics.Palette;
import android.util.Log;

import java.util.Comparator;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.R;

public class Definition {
    private final String DBG_TAG = "palette definition";

    final static int MAX_COLOURS_TO_PREVIEW = 128;

    public String   name;
    public int[]  colours;
    public Bitmap preview_bm;

    public int base_color;
    public int base_title_text_col;
    public int base_body_text_col;

    static int preview_height = MainActivity.resources.getDimensionPixelSize(R.dimen.palette_activity_preview_height);
    static int preview_width = MAX_COLOURS_TO_PREVIEW * 10;

    public Definition(String name, int[] colours) {
        this.name = name;
        this.colours = colours;

        generatePalettePreview();
    }

    private void generatePalettePreview() {
        int num_colours = Math.min(MAX_COLOURS_TO_PREVIEW, colours.length);
        int stripe_width = Math.max(1, preview_width / num_colours);
        float lft = 0;

        preview_bm = Bitmap.createBitmap(stripe_width * colours.length, preview_height, Bitmap.Config.RGB_565);
        Canvas cnv = new Canvas(preview_bm);
        Paint pnt = new Paint();

        // one stripe per colour
        for (int i = 0; i < num_colours; ++i) {
            lft = (float) i * stripe_width;

            pnt.setColor(colours[i]);
            cnv.drawRect(lft, 0, lft + stripe_width, preview_height, pnt);
        }

        Palette p = Palette.from(preview_bm).generate();
        Palette.Swatch swatch = p.getLightMutedSwatch();
        if (swatch == null) {
            swatch = p.getLightVibrantSwatch();
            if (swatch == null) {
                Log.d(DBG_TAG, "no swatch");
            }
        }

        if (swatch != null) {
            base_color = swatch.getRgb();
            base_title_text_col = swatch.getTitleTextColor();
            base_body_text_col = swatch.getBodyTextColor();
        }
    }
}
