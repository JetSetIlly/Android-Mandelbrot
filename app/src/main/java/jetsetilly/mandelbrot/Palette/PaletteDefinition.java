package jetsetilly.mandelbrot.Palette;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.graphics.Palette;
import android.util.Log;

import java.util.Comparator;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.R;

public class PaletteDefinition {
    private final String DBG_TAG = "palette definition";

    final static int MAX_COLOURS_TO_PREVIEW = 128;

    public enum PaletteMode {INDEX, WEIGHTED_INDEX};

    public String   name;
    public PaletteMode palette_mode;
    public int[]  colours;
    public int num_colours;
    public Bitmap preview_bm;

    public int base_color;
    public int base_title_text_col;
    public int base_body_text_col;

    static int preview_height = MainActivity.resources.getDimensionPixelSize(R.dimen.palette_activity_preview_height);
    static int preview_width = MAX_COLOURS_TO_PREVIEW * 10;

    /* initialisation - if interpolate == true then colours[] should be of the form

        {null_space_colour, starting_color, ending_colour, number of steps}

       final length of palette will be number of steps, plus one (for the null space colour)
     */

    public PaletteDefinition(String name, PaletteMode palette_mode, int[] colours) {
        init(name, palette_mode, colours, false);
    }

    public PaletteDefinition(String name, PaletteMode palette_mode, int[] colours, boolean interpolate) {
        init(name, palette_mode, colours, interpolate);
    }

    private void init(String name, PaletteMode palette_mode, int[] colours, boolean interpolate) {
        this.name = name;
        this.palette_mode = palette_mode;

        if (interpolate) {
            this.colours = interpolatePalette(colours);
        } else {
            this.colours = colours;
        }
        num_colours = this.colours.length -1;

        generatePalettePreview();
    }

    private int[] interpolatePalette(int[] interpolation) {
        int num_of_steps = interpolation[interpolation.length-1];
        int[] colours = new int[num_of_steps + 1];

        int start_red = Color.red(interpolation[1]);
        int start_green = Color.green(interpolation[1]);
        int start_blue = Color.blue((interpolation[1]));
        int end_red = Color.red(interpolation[2]);
        int end_green = Color.green(interpolation[2]);
        int end_blue = Color.blue((interpolation[2]));
        int red_step = (end_red - start_red) / num_of_steps;
        int green_step = (end_green - start_green) / num_of_steps;
        int blue_step = (end_blue - start_blue) / num_of_steps;

        colours[0] = interpolation[0];
        colours[1] = interpolation[1];
        colours[num_of_steps] = num_of_steps;

        for (int i = 2; i <= num_of_steps; ++ i ) {
            colours[i] = Color.rgb(
                    Math.min(255, Math.max(0, start_red + (red_step * (i-1)))),
                    Math.min(255, Math.max(0, start_green + (green_step * (i-1)))),
                    Math.min(255, Math.max(0, start_blue + (blue_step * (i-1))))
            );
        }

        return colours;
    }

    private void generatePalettePreview() {
        /* TODO: this is suitable for PaletteMode.REPEATED not necessarily for other Palette Modes
         */

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
