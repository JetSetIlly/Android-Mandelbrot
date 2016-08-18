package jetsetilly.mandelbrot.RenderCanvas.ImageView;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import jetsetilly.mandelbrot.Settings.PaletteSettings;

public class Background extends ImageView {
    protected int[] colours = new int[3];
    private PaletteSettings palette_settings = PaletteSettings.getInstance();

    private RenderCanvas_ImageView render_canvas;
    private Bitmap background_bm;

    private int width;
    private int height;

    /* initialisation */
    public Background(RenderCanvas_ImageView render_canvas, Context context) {
        super(context);
        this.render_canvas = render_canvas;
    }

    protected void initialise() {
        width = render_canvas.getWidth();
        height = render_canvas.getHeight();

        // needs to be run in the parent view's post() phase
        setMinimumWidth(width);
        setMinimumHeight(height);

        background_bm = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        setImageBitmap(background_bm);
    }
    /* END OF initialisation */

    protected void mostFrequent(int[] frequencies) {
        // find the most frequent color values
        int max_freq = Integer.MAX_VALUE;
        for (int i = 0; i < colours.length; ++ i) {
            int x = 0;
            for (int j = 1; j < frequencies.length; ++ j) {
                if (frequencies[j] > frequencies[x] && frequencies[j] < max_freq) {
                    x = j;
                }
            }
            colours[i] = palette_settings.colours[x];
            max_freq = frequencies[x];
        }
    }

    protected void setBackground() {
        background_bm.eraseColor(colours[0]);
        invalidate();
    }

    protected void resetBackground() {
        for (int i = 0; i < colours.length; ++ i) {
            colours[i] = palette_settings.colours[i+1];
        }
        setBackground();
    }

    protected Bitmap cloneBackground() {
        return Bitmap.createBitmap(background_bm);
    }
}
