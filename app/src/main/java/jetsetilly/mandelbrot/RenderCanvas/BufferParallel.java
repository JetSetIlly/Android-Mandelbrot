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

    public BufferParallel(RenderCanvas canvas) {
        this.canvas = canvas;

        queues = new IterationQueue[mandelbrot_settings.max_iterations+1];
        for (int i = 0; i < queues.length; ++i) {
            queues[i] = new IterationQueue();
        }

        bundled_points = new float[queues.length * BUFFER_SIZE];
        bundled_points_len = 0;
    }

    public void flush() {
        for (int i = 0; i < queues.length; ++ i) {
            if (!queues[i].isEmpty()) {
                canvas.drawBufferedPoints(queues[i].points, queues[i].points_len, i);
                queues[i].resetQueue();
            }
        }
    }

    public void pushDraw(float cx, float cy, int iteration) {
        if (queues[iteration].push(cx, cy)) {
            popDraw(iteration);
        }
    }

    public void popDraw(int iteration) {
        /*
        int cycle = canvas.getPaletteSize();

        if (iteration == 0) {
            System.arraycopy(queues[0].points, 0, bundled_points, 0, queues[0].points_len);
            bundled_points_len = queues[0].points_len;
            queues[0].resetQueue();
        } else {
            bundled_points_len = 0;

            // bundle iterations with the same cycle
            // note that we only bundle iterations that come after
            // the iteration that we are working with
            //
            // i = iteration % cycle you would imagine would work but it
            // has weird side effects that frankly, is just baffling to me
            // and i've spent far too long trying to figure it out. baffling
            for (int i = iteration; i < queues.length; i += cycle) {
                if (i != 0) {
                    System.arraycopy(queues[i].points, 0, bundled_points, bundled_points_len, queues[i].points_len);
                    bundled_points_len += queues[i].points_len;
                    queues[i].resetQueue();
                }
            }
        }

        canvas.drawBufferedPoints(bundled_points, bundled_points_len, iteration);
        */

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
