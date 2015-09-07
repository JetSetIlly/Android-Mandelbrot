package jetsetilly.mandelbrot.RenderCanvas;

import android.util.Log;

import jetsetilly.mandelbrot.Mandelbrot.MandelbrotSettings;

public class BufferParallel implements Buffer {
    final static public String DBG_TAG = "buffer parallel";

    private final int BUFFER_SIZE = 4096; // this should be an even number

    private MandelbrotSettings mandelbrot_settings = MandelbrotSettings.getInstance();

    private RenderCanvas canvas;
    private IterationQueue[] queues;

    // bundled_points is used in popDraw(). we don't want to keep reinitialising
    // memory every time popDraw() is called so we're declaring it here
    private float[] bundled_points;
    private int bundled_points_len;

    private long number_of_pushes;

    public BufferParallel(RenderCanvas canvas) {
        this.canvas = canvas;

        queues = new IterationQueue[mandelbrot_settings.max_iterations+1];
        for (int i = 0; i < queues.length; ++i) {
            queues[i] = new IterationQueue();
        }

        bundled_points = new float[queues.length * BUFFER_SIZE];
        bundled_points_len = 0;

        number_of_pushes = 0;
    }

    public void flush() {
        for (int i = 0; i < queues.length; ++ i) {
            if (!queues[i].isEmpty()) {
                canvas.drawBufferedPoints(queues[i].points, queues[i].points_len, i);
                queues[i].resetQueue();
                number_of_pushes = 0;
            }
        }
    }

    public void pushDraw(float cx, float cy, int iteration) {
        if (queues[iteration].push(cx, cy)) {
            popDraw(iteration);
        }
    }

    private void popDraw(int iteration) {
        canvas.drawBufferedPoints(queues[iteration].points, queues[iteration].points_len, iteration);
        queues[iteration].resetQueue();
    }

    class IterationQueue {
        public float[] points;
        public int points_len;

        public IterationQueue()
        {
            points = new float[BUFFER_SIZE];
            points_len = 0;
        }

        public boolean push(float cx, float cy) {
            // returns true when queue is full
            // false otherwise

            points[points_len++] = cx;
            points[points_len++] = cy;

            return points_len >= points.length;
        }

        public void resetQueue() {
            points_len = 0;
        }

        public boolean isEmpty() {
            return points_len == 0;
        }
    }
}
