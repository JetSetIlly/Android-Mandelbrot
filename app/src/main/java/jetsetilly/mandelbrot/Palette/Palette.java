package jetsetilly.mandelbrot.Palette;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import java.util.HashMap;

import jetsetilly.mandelbrot.R;
import jetsetilly.mandelbrot.Settings.Settings;
import jetsetilly.tools.LogTools;
import jetsetilly.tools.MyMath;

public class Palette {
    private static final String DBG_TAG = "palette";

    private PaletteEntry[] entries;
    private HashMap<String, PaletteEntry> entries_hash;

    public Palette() {
        // just use the presets for now
        entries = presets;
        entries_hash = new HashMap<>();
        for (PaletteEntry entry : entries) {
            entries_hash.put(entry.name, entry);
        }
    }

    public int numPalettes() {
        return entries.length;
    }

    public PaletteEntry getEntry(int id) {
        return entries[id];
    }

    public int[] getColours() {
        Settings settings = Settings.getInstance();
        if (settings.selected_palette_id == "") {
            settings.selected_palette_id = entries[0].name;
        }
        return getColours(entries_hash.get(settings.selected_palette_id).colours, settings.palette_smoothness);
    }

    static private int[] getColours(int[] colours, int smoothness) {
        int[] smooth_colours = new int[2 + (smoothness * (colours.length - 2))];

        // first colour (null colour)
        smooth_colours[0] = colours[0];

        // colours in between
        for (int c = 1; c < colours.length - 1; ++c) {
            int start_red = Color.red(colours[c]);
            int start_green = Color.green(colours[c]);
            int start_blue = Color.blue((colours[c]));
            int end_red = Color.red(colours[c + 1]);
            int end_green = Color.green(colours[c + 1]);
            int end_blue = Color.blue((colours[c + 1]));
            int red_step = (end_red - start_red) / smoothness;
            int green_step = (end_green - start_green) / smoothness;
            int blue_step = (end_blue - start_blue) / smoothness;

            smooth_colours[1 + (c * smoothness) - smoothness] = colours[c];

            for (int s = 0; s < smoothness; ++s) {
                smooth_colours[1 + (c * smoothness - smoothness) + s] = Color.rgb(
                        start_red + (red_step * s),
                        start_green + (green_step * s),
                        start_blue + (blue_step * s)
                );
            }
        }

        // last colour
        smooth_colours[smooth_colours.length - 1] = colours[colours.length - 1];

        return smooth_colours;
    }

    public Bitmap generateSwatch(Context context, String palette_name) {
        return generateSwatch(context, entries_hash.get(palette_name));
    }

    static private Bitmap generateSwatch(Context context, PaletteEntry entry) {
        // get dimensions of swatch ...
        int swatch_size = context.getResources().getDimensionPixelSize(R.dimen.palette_swatch_size);

        // ... and figure out number of horizontal/vertical divisions

        // -1 to length because we ignore the first colour (the null space colour) in the palette
        int num_colours = entry.colours.length - 1;

        // we don't want the number of colours we're considering to prime, even or less than 3
        while ((MyMath.isPrime(num_colours) || MyMath.isEven(num_colours)) && num_colours >= 3)
            -- num_colours;

        // do the swatch division calculation
        int x_divisions = num_colours;
        int y_divisions = 1;
        for (int i = num_colours - 1 ; i > 0; -- i) {
            if (num_colours % i == 0) {
                x_divisions = num_colours / i;
                y_divisions = i;
                break;
            }
        }

        LogTools.printDebug(DBG_TAG, "color division: num_colours=" + num_colours + " x_divisions=" + x_divisions + " y_divisions=" + y_divisions);

        // draw palette colours in the grid
        Bitmap bm = Bitmap.createBitmap(swatch_size, swatch_size, Bitmap.Config.RGB_565);
        Canvas cnv = new Canvas(bm);
        Paint pnt = new Paint();
        int individual_width = swatch_size / x_divisions;
        int individual_height = swatch_size / y_divisions;
        int paint_top = 0;
        int col_idx = 1;
        for(int y = 0; y < y_divisions; ++ y) {
            int paint_left = 0;
            for (int x = 0; x < x_divisions; ++ x) {
                pnt.setColor(entry.colours[col_idx]);
                cnv.drawRect(paint_left, paint_top, paint_left + individual_width, paint_top + individual_height, pnt);

                paint_left += individual_width;
                col_idx = Math.min(num_colours, col_idx + 1);
            }
            paint_top += individual_height;
        }

        // create circular mask
        Bitmap circular_bm = Bitmap.createBitmap(bm.getWidth(), bm.getHeight(), Bitmap.Config.ARGB_8888);
        pnt = new Paint();
        cnv = new Canvas(circular_bm);
        cnv.drawARGB(0, 0, 0, 0);
        cnv.drawCircle(bm.getWidth() / 2, bm.getHeight() / 2, bm.getWidth() / 2, pnt);

        // apply circular mask to the swatch
        Rect rect = new Rect(0, 0, bm.getWidth(), bm.getHeight());
        pnt.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        cnv.drawBitmap(bm, rect, rect, pnt);

        return circular_bm;
    }

    public static class PaletteEntry {
        public String name;
        public int[] colours;

        public PaletteEntry(String name, int[] colours) {
            this.name = name;
            this.colours = colours;
        }
    }

    static private final PaletteEntry[] presets = {
            new PaletteEntry("Candied Stripes",
                    new int[]{0xFF000000,
                            0xFF213877, 0xFF0a5bff, 0xFF2d98c2,
                            0xFF99ccff, 0xFF55dddd, 0xFF33eeff,
                            0xFF00ddee, 0xFF67f391, 0xFF38f070,
                            0xFF00b783, 0xFF0c8260, 0xFF660099,
                            0xFF771199, 0xFFd045e7, 0xFFcc66ee,
                            0xFFec84ef, 0xFFf0adf4, 0xFFf76df7,
                            0xFFff51c5, 0xFFff0fcf
                    }),

            new PaletteEntry("Sunnydale",
                    new int[]{0xFF000000,
                            0xFF660000, 0xFFffaa00, 0xFFfff070,
                            0xFF2d322c, 0xFF221100, 0xFF441100,
                            0xFF0a2805, 0xFF115522, 0xFF115f00,
                            0xFF228811, 0xFF667722, 0xFF505962,
                            0xFF333343, 0xFF2b2177, 0xFF330066,
                            0xFF140f37, 0xFF110022, 0xFF000011
                    }),

            new PaletteEntry("Juniper",
                    new int[]{0xFF000000,
                            0xFF3F4642, 0xFF869BA5, 0xFF09A0FF, 0xFFCDCFDE, 0xFF87CEE8,
                            0xFF2D5643, 0xFF56AE51, 0xFF54A1AD, 0xFFF6D2C6, 0xFF8FCAAE,
                            0xFF00005E, 0xFF0018F5, 0xFF00D5E9, 0xFF0075FF, 0xFF00FFFF,
                            0xFF5A5652, 0xFF1660D1, 0xFF8094B3, 0xFF88ACD5, 0xFFB5BFCF,
                            0xFF2F1A36, 0xFFA9619D, 0xFFE69ED6, 0xFFEED6EF, 0xFFE676A9,
                            0xFFAA5F6A, 0xFFE67596, 0xFFF7F5F6, 0xFFE5CDD2, 0xFFE3B2A9,
                            0xFF724045, 0xFFA95A6F, 0xFFBE6C88, 0xFFB98894, 0xFFD69FAE
                    }),

            new PaletteEntry("Original",
                    new int[]{0xFF000000,
                            0xFF4E474F, 0xFF8E869D,
                            0xFFFF941D, 0xFFFFEE45, 0xFF66B0E2,
                            0xFF4D006C, 0xFF0057DD, 0xFF13B2DC,
                            0xFF00A5FF, 0xFF01F2FF, 0xFF868182,
                            0xFFFF950C, 0xFFFFFF14, 0xFF13A1FF,
                            0xFFA2DADA, 0xFF9D8876, 0xFFFFEB58,
                            0xFF00DEFF, 0xFF32ABFF, 0xFFFFB75B,
                            0xFF807986, 0xFFFF1D00, 0xFFFF8000,
                            0xFFFFFF00, 0xFF2F53F3
                    }),

            new PaletteEntry("Green",
                    new int[]{0xFF000000,
                            0xFF254C23, 0xFFA08A7B,
                            0xFF9380FF, 0xFFC2BAFF, 0xFFB299FF,
                            0xFFEEFF00, 0xFFFFFF00, 0xFFFFFF00,
                            0xFF0041FF, 0xFF949E2C, 0xFF504E4C,
                            0xFFA8A29A, 0xFF908982, 0xFFD1CBC1,
                            0xFFC4BCB3, 0xFF4E474F, 0xFF8E868D,
                            0xFFFF941D, 0xFFFFEE45, 0xFF66B0E2,
                            0xFF4D006C, 0xFF0057DD, 0xFF13B2DC,
                            0xFF00A5FF, 0xFF01F2FF
                    }),

            new PaletteEntry("Orange",
                    new int[]{0xFF000000,
                            0xFF7A6D66, 0xFFFFA50C,
                            0xFF00FFFF, 0xFFFBF5E0, 0xFFB0CEA0,
                            0xFFA78671, 0xFFFFEE53, 0xFF00CFFF,
                            0xFFF9F2DF, 0xFFBAC0C2, 0xFF15287A,
                            0xFF2C1DC3, 0xFF5023FF, 0xFF6400FF,
                            0xFF8B00FF, 0xFF626F45, 0xFFA0A5A6,
                            0xFF808D97, 0xFF8C9CD9, 0xFFC4C4C2,
                            0xFF515F6E, 0xFF8BA0B7, 0xFF7389A1,
                            0xFFDEE6ED, 0xFF9EB7D4
                    }),

            new PaletteEntry("Blue",
                    new int[]{0xFF000000,
                            0xFF007CAC, 0xFFFFEEFF,
                            0xFFFFFFFF, 0xFFD3DCE1, 0xFF00C0FF,
                            0xFF020202, 0xFF3B3B3B, 0xFF7B7B7B,
                            0xFFCECECE, 0xFFFFFFFF, 0xFF232526,
                            0xFF727474, 0xFF4B5252, 0xFFCACAC6,
                            0xFF9DA0A2, 0xFF625475, 0xFF94AAB6,
                            0xFFBCC1CC, 0xFFFFFFFF, 0xFFA08BA4,
                            0xFF2B2B2B, 0xFF666666, 0xFF8A8A8A,
                            0xFFC1C1C1, 0xFFFFFF
                    }),

            new PaletteEntry("Monochrome",
                    new int[]{0xFF000000, 0xFFDDDDDD, 0xFFAAAAAA})
    };

    /* singleton pattern */
    private static final Palette singleton = new Palette();
    public static Palette getInstance() {
        return singleton;
    }
    /* end of singleton pattern */

}
