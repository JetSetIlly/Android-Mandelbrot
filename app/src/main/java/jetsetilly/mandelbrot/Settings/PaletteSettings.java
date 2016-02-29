package jetsetilly.mandelbrot.Settings;

import android.content.Context;
import android.content.SharedPreferences;

import jetsetilly.mandelbrot.Palette.PaletteDefinition;
import jetsetilly.mandelbrot.Palette.Presets;

public class PaletteSettings {
    private final String DBG_TAG = "palette settings";

    // colours definitions
    public final int DEF_PALETTE_ID = 0;

    // for simplicity, use palettes as defined in PalettePresets
    // TODO: store/retrieve definitions on disk
    public final PaletteDefinition[] palettes = Presets.presets;

    public int selected_id;
    public PaletteDefinition selected_palette;

    // TODO: make palette saving more robust (order of list might change)

    public void save(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor prefs_editor = prefs.edit();
        prefs_editor.putInt("palette_id", selected_id);
        prefs_editor.apply();
    }

    public void restore(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        setColours(prefs.getInt("palette_id", DEF_PALETTE_ID));

        // create swatches
        for (PaletteDefinition swatch : palettes) {
            swatch.generatePalettePreview();
        }
    }

    /* helper functions */
    public void setColours(int id) {
        selected_id = id;
        selected_palette = palettes[selected_id];
    }

    public int numColors() {
        return numColors(selected_id);
    }

    public int numColors(int id) {
        return palettes[id].colours.length;
    }
    /* end of helper functions */

    /* singleton pattern */
    private static final PaletteSettings singleton = new PaletteSettings();
    public static PaletteSettings getInstance() {
        return singleton;
    }
    /* end of singleton pattern */
}
