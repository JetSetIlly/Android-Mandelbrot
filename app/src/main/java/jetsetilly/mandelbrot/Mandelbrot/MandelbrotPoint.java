package jetsetilly.mandelbrot.Mandelbrot;

public class MandelbrotPoint {
    double x;
    double y;
    double A;
    double B;
    double U;
    double V;
    int iteration;

    public MandelbrotPoint(double x, double y) {
        this.x = x;
        this.y = y;
        this.U = (this.A = x) * this.A;
        this.V = (this.B = y) * this.B;
        this.iteration = -1;
    }
}
