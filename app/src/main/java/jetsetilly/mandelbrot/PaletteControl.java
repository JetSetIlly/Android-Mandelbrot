package jetsetilly.mandelbrot;

public class PaletteControl {
    /* colours definitions */
    public final int DEF_PALETTE_ID = 0;

    public final int HUE = 0;
    public final int SATURATION = 1;
    public final int VALUE = 2;

    public final int LOWEST_TO_HIGHEST = -1;
    public final int HIGHEST_TO_LOWEST = 1;

    /* first colour is the color used for zero space. it is not used for any other
    iteration. see MandelbrotQueue() for details.
     */
    public PaletteDefinition[] palettes = {
            new PaletteDefinition("Candied Stripes",
                new int[]{0xFF000000,
                        0xFF213877, 0xFF0a5bff, 0xFF2d98c2,
                        0xFF99ccff, 0xFF55dddd, 0xFF33eeff,
                        0xFF00ddee, 0xFF67f391, 0xFF38f070,
                        0xFF00b783, 0xFF0c8260, 0xFF660099,
                        0xFF771199, 0xFFd045e7, 0xFFcc66ee,
                        0xFFec84ef, 0xFFf0adf4, 0xFFf76df7,
                        0xFFff51c5, 0xFFff0fcf
                }),

            new PaletteDefinition("Sunnydale",
                new int[]{       0xFF000000,
                        0xFF660000,    0xFFffaa00,    0xFFfff070,
                        0xFF2d322c,    0xFF221100,    0xFF441100,
                        0xFF0a2805,    0xFF115522,    0xFF115f00,
                        0xFF228811,    0xFF667722,    0xFF505962,
                        0xFF333343,    0xFF2b2177,    0xFF330066,
                        0xFF140f37,    0xFF110022,    0xFF000011
                }),

            new PaletteDefinition("Juniper",
                new int[] {       0xFF000000,
                        0xFF3F4642, 0xFF869BA5, 0xFF09A0FF, 0xFFCDCFDE, 0xFF87CEE8,
                        0xFF2D5643, 0xFF56AE51, 0xFF54A1AD, 0xFFF6D2C6, 0xFF8FCAAE,
                        0xFF00005E, 0xFF0018F5, 0xFF00D5E9, 0xFF0075FF, 0xFF00FFFF,
                        0xFF5A5652, 0xFF1660D1, 0xFF8094B3, 0xFF88ACD5, 0xFFB5BFCF,
                        0xFF2F1A36, 0xFFA9619D, 0xFFE69ED6, 0xFFEED6EF, 0xFFE676A9,
                        0xFFAA5F6A, 0xFFE67596, 0xFFF7F5F6, 0xFFE5CDD2, 0xFFE3B2A9,
                        0xFF724045, 0xFFA95A6F, 0xFFBE6C88, 0xFFB98894, 0xFFD69FAE
                }),

            new PaletteDefinition("Original",
                new int[] {       0xFF000000,
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

            new PaletteDefinition("Green",
                new int[] {       0xFF000000,
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

            new PaletteDefinition("Purple",
                new int[] {       0xFF000000,
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

            new PaletteDefinition("Blue",
                new int[] {       0xFF000000,
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

            new PaletteDefinition("Monochrome",
                new int[] {0xFF000000, 0xFFDDDDDD, 0xFFAAAAAA})
    };
    /* end of colours definitions */

    public int selected_id;
    public int[] colours; // this is what RenderCanvas uses

    /* count the frequency at which each colour is used
     used to change the background colour of the RenderCanvas */
    private int colour_cnt[];
    private int colour_cnt_highest;

    public PaletteControl() {
        super();
        setColours(DEF_PALETTE_ID);
        resetCount();
    }

    /* helper functions */
    public void setColours(int id) {
        selected_id = id;
        colours = palettes[selected_id].colours;
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
        colour_cnt = new int[colours.length];

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
        return colours[colour_cnt_highest];
    }
    /* colour counting */

    /* singleton pattern */
    private static PaletteControl singleton = new PaletteControl();
    public static PaletteControl getInstance() {
        return singleton;
    }
    /* end of singleton pattern */
}