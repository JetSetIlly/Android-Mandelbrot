package jetsetilly.mandelbrot.Widgets;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import java.util.Random;

import jetsetilly.mandelbrot.R;

public class PaletteSwatch extends ImageView {
    private Bitmap circular_bitmap = null;
    

    public PaletteSwatch(Context context) {
        super(context);
    }

    public PaletteSwatch(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PaletteSwatch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO: calling prepareCircularView() for every call to onDraw() is wasteful but I can't figure
        // out where to put the call instead - overriding setImageBitmap() doesn't work because the
        // view will not have a width or a height

        prepareCircularView();
        canvas.drawBitmap(circular_bitmap, 0, 0, null);
    }

    private void prepareCircularView() {
        Drawable drawable = getDrawable();

        if (drawable instanceof BitmapDrawable ) {
            circular_bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            // not supporting anything other than bitmaps
            return;
        }

        circular_bitmap = circular_bitmap.copy(Bitmap.Config.ARGB_8888, true);
        circular_bitmap = getMaskedBitmap(circular_bitmap, getWidth(), this.getContext());
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
        int mask_resource_id = context.getResources().getIdentifier("splotch" + new Random().nextInt(5), "drawable", context.getPackageName());

        // load in mask
        Bitmap raw_mask = Bitmap.createBitmap(BitmapFactory.decodeResource(context.getResources(), mask_resource_id));

        // rotate
        //Matrix mask_matrix = new Matrix();
        //mask_matrix.setRotate(rand.nextInt(360) + 1, raw_mask.getWidth()/2, raw_mask.getHeight()/2);
        //Bitmap mask = Bitmap.createBitmap(raw_mask, 0, 0, raw_mask.getWidth(), raw_mask.getHeight(), mask_matrix, false);

        // do-not rotate
        Bitmap mask = raw_mask;

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

    private static Bitmap getCircularBitmap(Bitmap bmp, int radius) {
        Bitmap scaled_bitmap;
        if(bmp.getWidth() != radius || bmp.getHeight() != radius)
            scaled_bitmap = Bitmap.createScaledBitmap(bmp, radius, radius, false);
        else
            scaled_bitmap = bmp;

        Bitmap circular_bitmap = Bitmap.createBitmap(scaled_bitmap.getWidth(),
                scaled_bitmap.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(circular_bitmap);

        final Rect rect = new Rect(0, 0, scaled_bitmap.getWidth(), scaled_bitmap.getHeight());
        final Paint paint = new Paint();

        /*
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        */

        canvas.drawCircle(scaled_bitmap.getWidth() / 2+0.7f, scaled_bitmap.getHeight() / 2+0.7f,
                scaled_bitmap.getWidth() / 2+0.1f, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(scaled_bitmap, rect, rect, paint);

        return circular_bitmap;
    }
}