package jetsetilly.mandelbrot.Mandelbrot;

import android.util.Log;

public class Queue {
    final static public String DBG_TAG = "mandelbrot queue";

    private final int BUFFER_SIZE = 4096; // this should be an even number

    private Canvas context;
    private PointQueue[] queues;

    public Queue(Canvas context) {
        this.context = context;
        resetQueues();
    }

    public void resetQueues() {
        queues = new PointQueue[context.getPaletteSize()];
        for (int i = 0; i < queues.length; ++ i) {
            queues[i] = new PointQueue(i);
        }

        Log.d(DBG_TAG, "memory usage: " + BUFFER_SIZE * queues.length * 2 + " bytes");
    }

    public void finaliseDraw() {
        for (int i = 0; i < queues.length; ++ i) {
            queues[i].popDraw();
        }
    }

    public void pushDraw(int cx, int cy, int iteration) {
        if (iteration == 0) {
            queues[0].pushDraw(cx, cy);
        } else {
            // make sure we don't write into queue[queues.length] which is reserved for zero space
            int i = iteration;

            if (iteration >= queues.length) {
                i = (iteration % (queues.length - 1)) + 1;
            }

            queues[i].pushDraw(cx, cy);
        }
    }

    class PointQueue {
        private int iteration;
        private float[] points;
        private int points_ct;

        public PointQueue(int iteration)
        {
            this.iteration = iteration;
            points = new float[BUFFER_SIZE];
            points_ct = 0;
        }

        public void pushDraw(int cx, int cy) {
            points[points_ct++] = cx;
            points[points_ct++] = cy;

            // return true if buffer size has been reached
            if (points_ct >= points.length) {
                popDraw();
            }
        }

        public void popDraw() {
            if (points_ct > 0) {
                context.doDraw(points, points_ct, iteration);
                points_ct = 0;
            }
        }
    }
}
