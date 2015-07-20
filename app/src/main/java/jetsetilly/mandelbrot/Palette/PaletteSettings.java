package jetsetilly.mandelbrot.Palette;

public class PaletteSettings {
    private final String DBG_TAG = "palette settings";

    /* colours definitions */
    public final int DEF_PALETTE_ID = 0;
    public final int DEF_KEY_COL = 1;
    public final int DEF_NULL_COL = 0;

    /* for simplicity, use palettes as defined in PalettePresets
     TODO: store/retrieve definitions on disk */
    public Definition[] palettes = Presets.presets;

    public int selected_id;
    public Definition selected_palette;

    /* count the frequency at which each colour is used
     used to change the background colour of the RenderCanvas */
    private int colour_cnt[];
    private int colour_cnt_highest;

    public PaletteSettings() {
        super();
        setColours(DEF_PALETTE_ID);
        resetCount();
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
    private static PaletteSettings singleton = new PaletteSettings();
    public static PaletteSettings getInstance() {
        return singleton;
    }
    /* end of singleton pattern */
}
