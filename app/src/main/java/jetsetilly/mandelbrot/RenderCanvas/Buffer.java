package jetsetilly.mandelbrot.RenderCanvas;

import jetsetilly.mandelbrot.Mandelbrot.MandelbrotSettings;

public class Buffer {
    final static public String DBG_TAG = "render canvas buffer";

    private final int BUFFER_SIZE = 4096; // this should be an even number

    private RenderCanvas canvas;
    private MandelbrotSettings mandelbrot_settings;
    private IterationQueue[] queues;

    private float[] bundle;
    private int bundle_ct;

    public Buffer(RenderCanvas canvas, MandelbrotSettings settings) {
        this.canvas = canvas;
        this.mandelbrot_settings = settings;
        restart();
    }

    public void restart() {
        queues = new IterationQueue[mandelbrot_settings.max_iterations];
        for (int i = 0; i < queues.length; ++i) {
            queues[i] = new IterationQueue(i);
        }

        bundle = new float[queues.length * BUFFER_SIZE];
        bundle_ct = 0;
    }

    public void finalise() {
        for (int i = 0; i < queues.length; ++ i) {
            if (!queues[i].isEmpty()) {
                canvas.drawBufferedPoints(queues[i].points, queues[i].points_ct, i);
                queues[i].resetQueue();
            }
        }
    }

    public void pushDraw(float cx, float cy, int iteration) {
        queues[iteration].pushDraw(cx, cy);
    }

    class IterationQueue {
        private int iteration;

        public float[] points;
        public int points_ct;

        public IterationQueue(int iteration)
        {
            this.iteration = iteration;
            points = new float[BUFFER_SIZE];
            points_ct = 0;
        }

        public void pushDraw(float cx, float cy) {
            points[points_ct++] = cx;
            points[points_ct++] = cy;

            if (points_ct >= points.length) {
                popDraw();
            }
        }

        public void popDraw() {
            if (points_ct > 0) {
                canvas.drawBufferedPoints(points, points_ct, iteration);
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
