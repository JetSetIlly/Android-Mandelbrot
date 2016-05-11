package jetsetilly.mandelbrot.RenderCanvas;

import jetsetilly.mandelbrot.Settings.PaletteSettings;

public class ColourCache {
    // count the frequency at which each colour is used
    // used to change the background colour of the RenderCanvas
    private int colour_cnt[];
    private int colour_cnt_highest;

    PaletteSettings palette_settings = PaletteSettings.getInstance();

    public ColourCache() {
        reset();
    }

    public void reset() {
        // reset colour count
        colour_cnt = new int[palette_settings.numColors()];
        colour_cnt_highest = 1;
    }

    public void colourCountUpdate(int palette_entry)
    {
        // we don't want to consider colours[0] for the colour_cnt_highest
        // it's the zero space color it's not really a color
        if (palette_entry == 0) return;

        colour_cnt[palette_entry] ++;

        if (colour_cnt[palette_entry] >= colour_cnt[colour_cnt_highest]) {
            colour_cnt_highest = palette_entry;
        }
    }

    public int mostFrequentColor() {
        return palette_settings.colours[colour_cnt_highest];
    }
}
