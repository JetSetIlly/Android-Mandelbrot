package jetsetilly.mandelbrot.Palette;

import android.content.Context;
import android.graphics.Color;

public class PaletteDefinition {
    private static final String DBG_TAG = "palette definition";

    public enum PaletteMode {INDEX, INTERPOLATE}
    public String   name;
    public int[] colours;
    public int num_colours;

    private PaletteSwatch swatch;

    public PaletteDefinition(String name, PaletteMode palette_mode, int[] colours) {
        this.name = name;

        if (palette_mode == PaletteMode.INTERPOLATE) {
            this.colours = interpolate(colours);
        } else {
            this.colours = colours;
        }
        num_colours = this.colours.length-1;

        // swatch will be generated later once we have a Context
        swatch = null;
    }

    public PaletteSwatch getSwatch(Context context) {
        if (swatch == null) {
            swatch = new PaletteSwatch(context, colours);
        }
        return swatch;
    }

    private int[] interpolate(int[] discrete) {
        int steps = discrete[discrete.length-1];
        int[] interpolated = new int[steps + 1];

        int start_red = Color.red(discrete[1]);
        int start_green = Color.green(discrete[1]);
        int start_blue = Color.blue((discrete[1]));
        int end_red = Color.red(discrete[2]);
        int end_green = Color.green(discrete[2]);
        int end_blue = Color.blue((discrete[2]));
        int red_step = (end_red - start_red) / steps;
        int green_step = (end_green - start_green) / steps;
        int blue_step = (end_blue - start_blue) / steps;

        interpolated[0] = discrete[0];
        interpolated[1] = discrete[1];
        interpolated[steps] = steps;

        for (int i = 2; i <= steps; ++ i ) {
            interpolated[i] = Color.rgb(
                    Math.min(255, Math.max(0, start_red + (red_step * (i - 1)))),
                    Math.min(255, Math.max(0, start_green + (green_step * (i - 1)))),
                    Math.min(255, Math.max(0, start_blue + (blue_step * (i - 1))))
            );
        }

        return interpolated;
    }
}
