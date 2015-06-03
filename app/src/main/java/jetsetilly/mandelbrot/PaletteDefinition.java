package jetsetilly.mandelbrot;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import java.util.Comparator;

public class PaletteDefinition {
    final static int MAX_COLOURS_TO_PREVIEW = 128;

    public String   name;
    public int[]  colours;
    public Bitmap preview_bm;

    static int preview_height = MainActivity.resources.getDimensionPixelSize(R.dimen.palette_activity_preview_height);
    static int preview_width = MAX_COLOURS_TO_PREVIEW * 10;

    public PaletteDefinition(String name, int[] colours) {
        this.name = name;
        this.colours = colours;

        generatePalettePreview();
    }

    private void generatePalettePreview() {
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
    }

    static public float colorLightness(int color) {
        int R = android.graphics.Color.red(color);
        int G = android.graphics.Color.green(color);
        int B = android.graphics.Color.blue(color);

        int M = R;
        if (G > M) M = G;
        if(B > M) M = B;

        int m = R;
        if (G < m) m = G;
        if (B < m) m = B;

        return (M + m)/(float) 510;
    }

    public final int HUE = 0;
    public final int SATURATION = 1;
    public final int VALUE = 2;

    public final int LOWEST_TO_HIGHEST = -1;
    public final int HIGHEST_TO_LOWEST = 1;

    public int rankColor(int rank, final int direction) {
        float[][] lightness = new float[colours.length][2];

        for (int i = 0; i < colours.length; ++ i) {
            lightness[i][0] = colorLightness(colours[i]);
            lightness[i][1] = i;
        }

        java.util.Arrays.sort(lightness, new Comparator<float[]>() {
            @Override
            public int compare(float[] lhs, float[] rhs) {
                if (lhs[0] < rhs[0])
                    return direction;
                else if (lhs[0] > rhs[0])
                    return -direction;
                else
                    return 0;
            }
        });

        return colours[(int) lightness[rank][1]];
    }
}
