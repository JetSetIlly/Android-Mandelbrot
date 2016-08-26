package jetsetilly.mandelbrot.RenderCanvas.SurfaceView;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.view.TextureView;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.mandelbrot.RenderCanvas.Base.RenderCanvas_Base;
import jetsetilly.mandelbrot.Settings.PaletteSettings;

public class RenderCanvas_SurfaceView extends RenderCanvas_Base {
    private TextureView texture_view;

    private Canvas canvas;
    private Paint paint;

    private int offset_x;
    private int offset_y;
    private double zoom_factor;

    private boolean complete_render;

    private final PaletteSettings palette_settings = PaletteSettings.getInstance();
    private int num_colours;

    public RenderCanvas_SurfaceView(Context context) {
        super(context);
    }

    public RenderCanvas_SurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RenderCanvas_SurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /* GestureHandler implementation */
    @Override
    public void scroll(int x, int y) {
    }

    @Override
    public void autoZoom(int offset_x, int offset_y, boolean zoom_out) {
    }

    @Override
    public void manualZoom(float amount) {
    }

    @Override
    public void endManualZoom() {
    }

    @Override
    public void finishManualGesture() {
    }
    /* END OF GestureHandler implementation */

    /* MandelbrotCanvas implementation */
    @Override
    public void startDraw(long canvas_id) {
        canvas = texture_view.lockCanvas();
        paint = new Paint();
        num_colours = palette_settings.numColors();
    }

    @Override
    public void plotIterations(long canvas_id, int[] iterations, boolean complete_plot) {
        int width = getCanvasWidth();
        for (int i = 0; i < iterations.length; ++ i) {
            int iteration = iterations[i];
            if (iteration != Mandelbrot.NULL_ITERATIONS) {
                plotIteration(canvas_id, i % width, i / width, iterations[i]);
            }
        }
    }

    @Override
    public void plotIteration(long canvas_id, int dx, int dy, int iteration) {
        // figure out which colour to use
        int palette_entry = iteration;
        if (iteration >= num_colours) {
            palette_entry = (iteration % (num_colours - 1)) + 1;
        }

        // put coloured pixel into pixel buffer - ready for flushing
        paint.setColor(palette_settings.colours[palette_entry]);
        canvas.drawPoint(dx, dy, paint);
    }

    @Override
    public void update(long canvas_id) {
        /*
        The content of the Surface is never preserved between unlockCanvas() and lockCanvas(), for
        this reason, every pixel within the Surface area must be written. The only exception to this
        rule is when a dirty rectangle is specified, in which case, non-dirty pixels will be preserved.
        */
    }

    @Override
    public void endDraw(long canvas_id, boolean cancelled) {
        texture_view.unlockCanvasAndPost(canvas);
        canvas = null;
        complete_render = true;
    }

    @Override
    public int getCanvasWidth() {
        return getWidth();
    }

    @Override
    public int getCanvasHeight() {
        return getHeight();
    }

    @Override
    public boolean isCompleteRender() {
        return false;
    }
    /* END OF MandelbrotCanvas implementation */

    /* RenderCanvas implementation */
    @Override
    public void initialise(final MainActivity main_activity) {
        post(new Runnable() {
            @Override
            public void run() {
                setup(main_activity);
            }
        });
        super.initialise(main_activity);
    }

    private void setup(final MainActivity main_activity) {
        this.texture_view = new TextureView(main_activity);
        addView(texture_view);

        texture_view.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                resetCanvas();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            }
        });
    }

    @Override
    public void startRender() {
        mandelbrot.transformMandelbrot(offset_x, offset_y, zoom_factor);
        mandelbrot.startRender();
    }

    @Override
    public void stopRender() {
        if (mandelbrot != null)
            mandelbrot.stopRender();
    }

    @Override
    public void resetCanvas() {
        stopRender();
        super.resetCanvas();
        startRender();
    }

    @Override
    public Bitmap getVisibleImage(boolean bilinear_filter) {
        return null;
    }
    /* END OF RenderCanvas implementation */
}
