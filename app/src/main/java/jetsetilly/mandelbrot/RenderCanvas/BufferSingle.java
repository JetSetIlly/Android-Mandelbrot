package jetsetilly.mandelbrot.RenderCanvas;

public class BufferSingle implements Buffer {
    final static public String DBG_TAG = "buffer single";

    private RenderCanvas canvas;

    public float[] points;
    public int points_len;
    private int iteration;

    public BufferSingle(RenderCanvas canvas) {
        this.canvas = canvas;

        points = new float[canvas.getCanvasHeight() * canvas.getCanvasWidth() * 2];
        points_len = 0;
        iteration = 0;
    }

    public void flush() {
        canvas.drawBufferedPoints(points, points_len, iteration);
        points_len = 0;
    }

    public void pushDraw(float cx, float cy, int iteration) {
        if (this.iteration != iteration) {
            flush();
            this.iteration = iteration;
        }

        points[points_len++] = cx;
        points[points_len++] = cy;
    }
}
