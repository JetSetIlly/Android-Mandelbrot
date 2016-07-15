package jetsetilly.mandelbrot.RenderCanvas.SurfaceView;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;

import jetsetilly.mandelbrot.MainActivity;
import jetsetilly.mandelbrot.RenderCanvas.Base.RenderCanvas_Base;

public class RenderCanvas_SurfaceView extends RenderCanvas_Base{
    public RenderCanvas_SurfaceView(Context context) {
        super(context);
    }

    public RenderCanvas_SurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /* GestureHandler implementation */
    @Override
    public void animatedZoom(int offset_x, int offset_y) {

    }

    @Override
    public void pinchZoom(float amount) {

    }

    @Override
    public void zoomCorrection(boolean force) {

    }
    /* END OF GestureHandler implementation */

    /* MandelbrotCanvas implementation */
    @Override
    public void startDraw(long canvas_id) {

    }

    @Override
    public void plotIterations(long canvas_id, int[] iterations, boolean complete_plot) {

    }

    @Override
    public void plotIteration(long canvas_id, int dx, int dy, int iteration) {

    }

    @Override
    public void update(long canvas_id) {

    }

    @Override
    public void endDraw(long canvas_id) {

    }

    @Override
    public void cancelDraw(long canvas_id) {

    }

    @Override
    public int getCanvasWidth() {
        return 0;
    }

    @Override
    public int getCanvasHeight() {
        return 0;
    }

    @Override
    public boolean isCompleteRender() {
        return false;
    }
    /* END OF MandelbrotCanvas implementation */

    /* RenderCanvas implementation */
    @Override
    public void initialise(MainActivity main_activity) {

    }

    @Override
    public void startRender() {

    }

    @Override
    public void stopRender() {

    }

    @Override
    public void resetCanvas(MainActivity main_activity) {

    }

    @Override
    public void reRender() {

    }

    @Override
    public Bitmap getVisibleImage(boolean bilinear_filter) {
        return null;
    }
    /* END OF RenderCanvas implementation */
}
