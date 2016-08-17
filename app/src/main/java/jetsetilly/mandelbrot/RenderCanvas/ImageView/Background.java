package jetsetilly.mandelbrot.RenderCanvas.ImageView;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;

import jetsetilly.mandelbrot.Settings.PaletteSettings;

public class Background extends ImageView {
    protected int[] colours = new int[3];
    private PaletteSettings palette_settings = PaletteSettings.getInstance();

    /* initialisation */
    public Background(Context context) {
        super(context);
    }

    public Background(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Background(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    /* END OF initialisation */

    protected void mostFrequent(int[] frequencies) {
        // find the most frequent color values
        int max_freq = Integer.MAX_VALUE;
        for (int i = 0; i < colours.length; ++ i) {
            int x = 0;
            for (int j = 0; j < frequencies.length; ++ j) {
                if (frequencies[j] > frequencies[x] && frequencies[j] < max_freq) {
                    x = j;
                }
            }
            colours[i] = palette_settings.colours[x];
            max_freq = frequencies[x];
        }
    }

    protected void setBackground() {
        setBackgroundColor(colours[0]);
    }

    protected void resetBackground() {
        for (int i = 0; i < colours.length; ++ i) {
            colours[i] = palette_settings.colours[i+1];
        }
        setBackground();
    }

    protected Bitmap cloneBackground() {
        Bitmap cloned_bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        cloned_bitmap.eraseColor(colours[0]);
        return cloned_bitmap;
    }
}
