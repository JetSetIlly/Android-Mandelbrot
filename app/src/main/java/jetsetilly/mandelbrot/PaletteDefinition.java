package jetsetilly.mandelbrot;

import java.util.Comparator;

public class PaletteDefinition {
    public String   name;
    public int[]  colours;

    public PaletteDefinition(String name, int[] colours) {
        this.name = name;
        this.colours = colours;
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
