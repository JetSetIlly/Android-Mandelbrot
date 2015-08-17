package jetsetilly.mandelbrot.Mandelbrot;

import android.util.Log;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Buffer {
    final static public String DBG_TAG = "mandelbrot queue";

    private final int BUFFER_SIZE = 4096; // this should be an even number

    private MandelbrotCanvas canvas;
    private MandelbrotSettings mandelbrot_settings;
    private IterationQueue[] queues;

    private float[] bundle;
    private int bundle_ct;

    public Buffer(MandelbrotCanvas canvas, MandelbrotSettings settings) {
        this.canvas = canvas;
        this.mandelbrot_settings = settings;
        restart();
    }

    public void restart() {
        queues = new IterationQueue[mandelbrot_settings.max_iterations];
        for (int i = 0; i < queues.length; ++i) {
            queues[i] = new IterationQueue(i, this);
        }

        bundle = new float[queues.length * BUFFER_SIZE];
        bundle_ct = 0;
    }

    public void finalise() {
        for (int i = 0; i < queues.length; ++ i) {
            if (!queues[i].isEmpty()) {
                canvas.doDraw(queues[i].points, queues[i].points_ct, i);
                queues[i].resetQueue();
            }
        }
    }

    public void pushDraw(int cx, int cy, int iteration) {
        queues[iteration].pushDraw(cx, cy);
    }

    public void bundlePoints(int iteration, int cycle) {
        /* call with cycle == -1 if you don't want any bundling to occur */

        if (iteration == 0 || cycle == -1) {
            System.arraycopy(queues[iteration].points, 0, bundle, 0, queues[iteration].points_ct);
            bundle_ct = queues[iteration].points_ct;

            queues[iteration].resetQueue();

        } else {
            bundle_ct = 0;

            for (int i = iteration % cycle == 0 ? cycle : iteration % cycle; i < queues.length; i += cycle) {
                if (!queues[i].isEmpty()) {
                    System.arraycopy(queues[i].points, 0, bundle, bundle_ct, queues[i].points_ct);
                    bundle_ct += queues[i].points_ct;
                    queues[i].resetQueue();
                }
            }
        }

        canvas.doDraw(bundle, bundle_ct, iteration);
    }

    class IterationQueue {
        private Buffer buffer;
        private int iteration;

        public float[] points;
        public int points_ct;

        public IterationQueue(int iteration, Buffer buffer)
        {
            this.buffer = buffer;
            this.iteration = iteration;
            points = new float[BUFFER_SIZE];
            points_ct = 0;
        }

        public void pushDraw(int cx, int cy) {
            points[points_ct++] = cx;
            points[points_ct++] = cy;

            if (points_ct >= points.length) {
                popDraw();
            }
        }

        public void popDraw() {
            if (points_ct > 0) {
                canvas.notifyDraw(buffer, iteration);
                resetQueue();
            }
        }

        public void resetQueue() {
            points_ct = 0;
        }

        public boolean isEmpty() {
            return points_ct == 0;
        }
    }
}
