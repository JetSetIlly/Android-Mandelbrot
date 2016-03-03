package jetsetilly.mandelbrot.Palette;

import android.content.Context;
import android.graphics.Color;

import java.util.Arrays;

import jetsetilly.mandelbrot.Tools;

public class PaletteDefinition {
    private static final String DBG_TAG = "palette definition";

    public String name;
    public int[] colours;

    private PaletteSwatch swatch;

    public PaletteDefinition(String name, int[] colours) {
        this.name = name;
        this.colours = colours;

        // swatch will be generated later once we have a Context
        swatch = null;
    }

    public PaletteSwatch getSwatch(Context context) {
        if (swatch == null) {
            swatch = new PaletteSwatch(context, colours);
        }
        return swatch;
    }

    public int[] getColours(int smoothness) {
        int[] interpolated = new int[2 + (smoothness * (colours.length-2))];

        // first colour (null colour)
        interpolated[0] = colours[0];

        // colours in between
        for (int c = 1; c < colours.length-1; ++ c) {
            int start_red = Color.red(colours[c]);
            int start_green = Color.green(colours[c]);
            int start_blue = Color.blue((colours[c]));
            int end_red = Color.red(colours[c+1]);
            int end_green = Color.green(colours[c+1]);
            int end_blue = Color.blue((colours[c+1]));
            int red_step = (end_red - start_red) / smoothness;
            int green_step = (end_green - start_green) / smoothness;
            int blue_step = (end_blue - start_blue) / smoothness;

            interpolated[1 + (c * smoothness) - smoothness] = colours[c];

            for (int s = 0; s < smoothness; ++ s) {
                interpolated[1 + (c * smoothness - smoothness) + s] = Color.rgb(
                        start_red + (red_step * s),
                        start_green + (green_step * s),
                        start_blue + (blue_step * s)
                );
            }
        }

        // last colour
        interpolated[interpolated.length-1] = colours[colours.length-1];

        return interpolated;
    }
}
