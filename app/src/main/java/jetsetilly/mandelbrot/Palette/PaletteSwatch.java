package jetsetilly.mandelbrot.Palette;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;

import jetsetilly.mandelbrot.R;

public class PaletteSwatch {
    public Bitmap bitmap;

    public PaletteSwatch(Context context, int[] colours) {
        int swatch_size = context.getResources().getDimensionPixelSize(R.dimen.palette_swatch_size);
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

        bitmap = getCircularSwatch(bm, swatch_size);
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

    private static Bitmap getCircularSwatch(Bitmap bmp, int radius) {
        // scale swatch to correct size
        Bitmap scaled_bitmap;
        if(bmp.getWidth() != radius || bmp.getHeight() != radius)
            scaled_bitmap = Bitmap.createScaledBitmap(bmp, radius, radius, false);
        else
            scaled_bitmap = bmp;

        // create circular mask
        Bitmap circular_bitmap = Bitmap.createBitmap(scaled_bitmap.getWidth(), scaled_bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        /*
        // load in mask
        Bitmap mask = Bitmap.createBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.palette_splatter));
        Matrix matrix = new Matrix();
        matrix.postScale(((float) radius)/mask.getWidth(), ((float) radius)/mask.getHeight());
        matrix.postRotate(new Random().nextInt(8)*45, mask.getWidth()/2, mask.getHeight()/2);
        mask = Bitmap.createBitmap(mask, 0, 0, mask.getWidth(), mask.getHeight(), matrix, true);
        */

        Canvas canvas = new Canvas(circular_bitmap);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(scaled_bitmap.getWidth() / 2+0.7f, scaled_bitmap.getHeight() / 2+0.7f,
                scaled_bitmap.getWidth() / 2+0.1f, paint);

        // apply mask to the swatch
        Rect rect = new Rect(0, 0, scaled_bitmap.getWidth(), scaled_bitmap.getHeight());
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(scaled_bitmap, rect, rect, paint);

        return circular_bitmap;
    }
}
