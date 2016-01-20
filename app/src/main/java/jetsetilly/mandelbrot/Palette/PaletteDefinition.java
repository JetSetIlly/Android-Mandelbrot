package jetsetilly.mandelbrot.Palette;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import java.util.Random;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.R;

public class PaletteDefinition {
    private final String DBG_TAG = "palette definition";

    public enum PaletteMode {INDEX, INTERPOLATE}

    public String   name;
    public PaletteMode palette_mode;
    public int[]  colours;
    public int num_colours;

    public Bitmap swatch;
    static final int swatch_size = MainActivity.resources.getDimensionPixelSize(R.dimen.palette_swatch_size);

    /* initialisation - if interpolate == true then colours[] should be of the form

        {null_space_colour, starting_color, ending_colour, number of steps}

       final length of palette will be number of steps, plus one (for the null space colour)
     */

    public PaletteDefinition(String name, PaletteMode palette_mode, int[] colours) {
        init(name, palette_mode, colours, false);
    }

    public PaletteDefinition(String name, PaletteMode palette_mode, int[] colours, boolean interpolate) {
        init(name, palette_mode, colours, interpolate);
    }

    private void init(String name, PaletteMode palette_mode, int[] colours, boolean interpolate) {
        this.name = name;
        this.palette_mode = palette_mode;

        if (interpolate) {
            this.colours = interpolatePalette(colours);
        } else {
            this.colours = colours;
        }
        num_colours = this.colours.length -1;

        // swatch will be generated later once we have a Context
        swatch = null;
    }

    private int[] interpolatePalette(int[] interpolation) {
        int num_of_steps = interpolation[interpolation.length-1];
        int[] colours = new int[num_of_steps + 1];

        int start_red = Color.red(interpolation[1]);
        int start_green = Color.green(interpolation[1]);
        int start_blue = Color.blue((interpolation[1]));
        int end_red = Color.red(interpolation[2]);
        int end_green = Color.green(interpolation[2]);
        int end_blue = Color.blue((interpolation[2]));
        int red_step = (end_red - start_red) / num_of_steps;
        int green_step = (end_green - start_green) / num_of_steps;
        int blue_step = (end_blue - start_blue) / num_of_steps;

        colours[0] = interpolation[0];
        colours[1] = interpolation[1];
        colours[num_of_steps] = num_of_steps;

        for (int i = 2; i <= num_of_steps; ++ i ) {
            colours[i] = Color.rgb(
                    Math.min(255, Math.max(0, start_red + (red_step * (i - 1)))),
                    Math.min(255, Math.max(0, start_green + (green_step * (i - 1)))),
                    Math.min(255, Math.max(0, start_blue + (blue_step * (i - 1))))
            );
        }

        return colours;
    }

    public void generatePalettePreview(Context context) {
        int[] swatch_dimensions = paletteDimensions(colours.length - 1);
        int entry_width = swatch_size / swatch_dimensions[0];
        int entry_height = swatch_size / swatch_dimensions[1];

        Bitmap bm = Bitmap.createBitmap(entry_width * swatch_dimensions[0], entry_height * swatch_dimensions[1], Bitmap.Config.ARGB_8888);
        Canvas cnv = new Canvas(bm);
        Paint pnt = new Paint();

        int lft, top, col_idx;

        col_idx = 1;
        top = 0;
        for (int y = 0; y < swatch_dimensions[1]; y ++ ) {
            lft = 0;
            for (int x = 0; x < swatch_dimensions[0]; x ++ ) {
                pnt.setColor(colours[col_idx]);
                cnv.drawRect(lft, top, lft + entry_width, top + entry_height, pnt);

                lft += entry_width;
                col_idx = Math.min(colours.length, col_idx + 1);
            }
            top += entry_height;
        }

        swatch = getMaskedBitmap(bm, swatch_size, context);
    }

    private static int[] paletteDimensions(int num_colours) {
       	int sx = 0;
    	int si = 0;

	    int z = num_colours - 1;

        while (si == 0) {
            z ++;
            int d = z;
            for (int i = 1; i <= z/2; i ++) {
                int x = z / i;
                float y = (float)z / i;

                if (y - x == 0) {
                    if (Math.abs(i - x) < d) {
                        d = Math.abs(i - x);
                        sx = x;
                        si = i;
                    }
                }
            }
        }

        return (new int[] {si, sx});
    }

    private static Bitmap getMaskedBitmap(Bitmap bmp, int radius, Context context) {
        // scale swatch to correct size
        Bitmap scaled_bitmap;
        if(bmp.getWidth() != radius || bmp.getHeight() != radius) {
            scaled_bitmap = Bitmap.createScaledBitmap(bmp, radius, radius, false);
        } else {
            scaled_bitmap = bmp;
        }

        // decide which mask we're using
        int mask_resource_id = context.getResources().getIdentifier(
                "splotch" + new Random().nextInt(5),
                "drawable", context.getPackageName()
        );

        // load in mask
        Bitmap mask = Bitmap.createBitmap(BitmapFactory.decodeResource(context.getResources(), mask_resource_id));

        // rotate
        Matrix matrix = new Matrix();
        matrix.postRotate(new Random().nextInt(8)*45, mask.getWidth()/2, mask.getHeight()/2);
        mask = Bitmap.createBitmap(mask, 0, 0, mask.getWidth(), mask.getHeight(), matrix, false);

        // scale
        mask = Bitmap.createScaledBitmap(mask, radius, radius, false);

        // apply the mask to the swatch
        Rect rect = new Rect(0, 0, radius, radius);
        Paint paint = new Paint();
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        Canvas canvas = new Canvas(mask);
        canvas.drawBitmap(scaled_bitmap, rect, rect, paint);

        return mask;
    }

    private static Bitmap getCircularSwatch(Bitmap bmp, int radius) {
        Bitmap scaled_bitmap;
        if(bmp.getWidth() != radius || bmp.getHeight() != radius)
            scaled_bitmap = Bitmap.createScaledBitmap(bmp, radius, radius, false);
        else
            scaled_bitmap = bmp;

        Bitmap circular_bitmap = Bitmap.createBitmap(scaled_bitmap.getWidth(),
                scaled_bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(circular_bitmap);

        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, scaled_bitmap.getWidth(), scaled_bitmap.getHeight());

        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(Color.parseColor("#BAB399"));
        canvas.drawCircle(scaled_bitmap.getWidth() / 2+0.7f, scaled_bitmap.getHeight() / 2+0.7f,
                scaled_bitmap.getWidth() / 2+0.1f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(scaled_bitmap, rect, rect, paint);

        return circular_bitmap;
    }

}
