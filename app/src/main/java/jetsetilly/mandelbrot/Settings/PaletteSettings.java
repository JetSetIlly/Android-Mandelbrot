package jetsetilly.mandelbrot.Settings;

import android.content.Context;
import android.content.SharedPreferences;

import jetsetilly.mandelbrot.Palette.PaletteDefinition;
import jetsetilly.mandelbrot.Palette.Presets;

public class PaletteSettings {
    private final String DBG_TAG = "palette settings";

    /* colours definitions */
    public final int DEF_PALETTE_ID = 0;
    public final int DEF_KEY_COL = 1;   // base color - used to colourise tabs in ColoursActivity

    /* for simplicity, use palettes as defined in PalettePresets
     TODO: store/retrieve definitions on disk */
    public final PaletteDefinition[] palettes = Presets.presets;

    public int selected_id;
    public PaletteDefinition selected_palette;

    /* count the frequency at which each colour is used
     used to change the background colour of the RenderCanvas */
    private int colour_cnt[];
    private int colour_cnt_highest;

    public PaletteSettings() {
        setColours(DEF_PALETTE_ID);
        resetCount();
    }

    /* TODO:
    saving palette_id is not very robust - if the position in the preset list
    changes then the saved palette_id will be wrong. we should save the palette
    name and search for it in the list.
     */

    public void save(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor prefs_editor = prefs.edit();
        prefs_editor.putInt("palette_id", selected_id);
        prefs_editor.apply();
    }

    public void restore(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        setColours(prefs.getInt("palette_id", DEF_PALETTE_ID));
    }

    /* helper functions */
    public void setColours(int id) {
        selected_id = id;
        selected_palette = palettes[selected_id];
        resetCount();
    }

    public int numColors() {
        return numColors(selected_id);
    }

    public int numColors(int id) {
        return palettes[id].colours.length;
    }
    /* end of helper functions */

    /* colour counting */
    public void resetCount() {
        colour_cnt = new int[selected_palette.colours.length];

        // colour_cnt_highest is used to fill the screen
        // when starting rendering. set it to a random number to begin with
        colour_cnt_highest = 1; // new Random().nextInt(colours.length-1)+1;
    }

    public void updateCount(int palette_entry)
    {
        colour_cnt[palette_entry] ++;

        // we don't want to consider colours[0] for the colour_cnt_highest
        // it's the zero space color it's not really a color
        if (palette_entry == 0) return;

        if (colour_cnt[palette_entry] > colour_cnt[colour_cnt_highest]) {
            colour_cnt_highest = palette_entry;
        }
    }

    public int mostFrequentColor() {
        return selected_palette.colours[colour_cnt_highest];
    }
    /* colour counting */

    /* singleton pattern */
    private static final PaletteSettings singleton = new PaletteSettings();
    public static PaletteSettings getInstance() {
        return singleton;
    }
    /* end of singleton pattern */
}
