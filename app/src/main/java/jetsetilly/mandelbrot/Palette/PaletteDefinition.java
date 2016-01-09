package jetsetilly.mandelbrot.Palette;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.R;

public class PaletteDefinition {
    private final String DBG_TAG = "palette definition";

    final static int MAX_COLOURS_TO_PREVIEW = 128;

    public enum PaletteMode {INDEX, INTERPOLATE};

    public String   name;
    public PaletteMode palette_mode;
    public int[]  colours;
    public int num_colours;
    public Bitmap swatch;

    static int preview_height = MainActivity.resources.getDimensionPixelSize(R.dimen.palette_swatch_size);
    static int preview_width = preview_height;

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

        swatch = generatePalettePreview();
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

    private Bitmap generatePalettePreview() {
        int num_stripes = Math.min(MAX_COLOURS_TO_PREVIEW, colours.length-1);
        int stripe_width = preview_width / num_stripes;
        float lft;

        Bitmap bm = Bitmap.createBitmap(preview_width, preview_height, Bitmap.Config.RGB_565);
        Canvas cnv = new Canvas(bm);
        Paint pnt = new Paint();

        // one stripe per colour
        for (int i = 0; i < num_stripes; ++i) {
            lft = (float) i * stripe_width;
            pnt.setColor(colours[i+1]);
            cnv.drawRect(lft, 0, lft + stripe_width, preview_height, pnt);
        }

        return bm;
    }
}
