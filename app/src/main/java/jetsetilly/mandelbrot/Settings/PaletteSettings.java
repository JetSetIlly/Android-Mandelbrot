package jetsetilly.mandelbrot.Settings;

import android.content.Context;
import android.content.SharedPreferences;

import jetsetilly.mandelbrot.Palette.PaletteDefinition;
import jetsetilly.mandelbrot.Palette.Presets;

// TODO: store/retrieve definitions on disk
// TODO: make palette saving more robust (order of list might change)

public class PaletteSettings {
    private final int DEF_PALETTE_ID = 0;
    public final int DEF_SMOOTHNESS = 4;

    // for simplicity, use palettes as defined in PalettePresets
    public final PaletteDefinition[] palettes = Presets.presets;

    public int selected_id;
    public int smoothness = 4;
    public int[] colours;

    public void save(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor prefs_editor = prefs.edit();
        prefs_editor.putInt("palette_id", selected_id);
        prefs_editor.putInt("smoothness", smoothness);
        prefs_editor.apply();
    }

    public void restore(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        setColours(prefs.getInt("palette_id", DEF_PALETTE_ID));
        smoothness = (prefs.getInt("smoothness", DEF_SMOOTHNESS));
    }

    /* helper functions */
    public void setColours() {
        setColours(selected_id);
    }
    public void setColours(int id) {
        selected_id = id;
        colours = palettes[selected_id].getColours(smoothness);
    }

    public int numColors() {
        return colours.length;
    }
    /* end of helper functions */

    /* singleton pattern */
    private static final PaletteSettings singleton = new PaletteSettings();
    public static PaletteSettings getInstance() {
        return singleton;
    }
    /* end of singleton pattern */
}
