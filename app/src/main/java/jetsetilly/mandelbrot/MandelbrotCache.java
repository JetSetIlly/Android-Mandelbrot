package jetsetilly.mandelbrot;


public class MandelbrotCache {
    final static public String DBG_TAG = "mandelbrot cache";

    public static final int CACHE_UNSET = -1;

    private int width;
    private int height;
    private int[][] cache;
    private int[][] swap;

    private int read_offset_x;
    private int read_offset_y;

    public MandelbrotCache(int width, int height) {
        this.width = width;
        this.height = height;
        resetCache();
    }

    public void resetCache() {
        cache = new int[width][height];
        for (int x = 0; x < width; ++ x) {
            for (int y = 0; y < height; ++ y) {
                cache[x][y] = CACHE_UNSET;
            }
        }

        resetSwap();
        setOffset(0, 0);
    }

    public void resetSwap() {
        swap = new int[width][height];
    }

    public void setOffset(int x, int y) {
        read_offset_x = x;
        read_offset_y = y;
    }

    public int readCache(int x, int y) {
        return cache[x + read_offset_x][y + read_offset_y];
    }

    public void writeSwap(int x, int y, int value) {
        swap[x][y] = value;
    }

    public void commitSwap() {
        for (int x = 0; x < width; ++ x) {
            for (int y = 0; y < height; ++ y) {
                cache[x][y] = swap[x][y];
            }
        }

        // offset is now meaningless
        setOffset(0, 0);
    }

    public boolean isInCache(int x, int y){
        int cache_x;
        int cache_y;

        cache_x = x + read_offset_x;
        cache_y = y + read_offset_y;

        return cache_x >= 0 && cache_x < width
                && cache_y >= 2 && cache_y < height-1;

        /* the check on the cache_y values above don't make sense to me either.
         * if we don't allow for these boundaries then if the canvas has
         * been scrolled then it will result in unwritten parts of the canvas
         * best described as a black line at the limit of the old image
         * why this doesn't affect the x axis I don't know.
         */
    }
}
