package jetsetilly.mandelbrot;

public class SettingsMandelbrot {
    public String[][] presentable_settings = {
            {"Max Iterations", "max_iterations"},
            {"Bailout Value", "bailout_value"}
    };

    /* default values -- these are approximately square. Mandelbrot.correctMandelbrotRange() will correct squishiness */
    public double real_left = -2.11;
    public double real_right = 1.16;
    public double imaginary_upper = 2.94;
    public double imaginary_lower = -2.88;

    public int max_iterations = 60;
    public double bailout_value = 4.0;

    /* singleton pattern */
    private static SettingsMandelbrot singleton = new SettingsMandelbrot();
    public static SettingsMandelbrot getInstance() {
        return singleton;
    }
    /* end of singleton pattern */
}
