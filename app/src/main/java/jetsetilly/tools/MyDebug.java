package jetsetilly.tools;

public class MyDebug {
    private final static String DBG_TAG = "MyDebug";

    static private String tag;
    static private long start_time;
    static private final long NANO_PER_FRAME = 16000000;

    static public void start(String new_tag) {
        tag = new_tag;
        start_time = System.nanoTime();
    }

    static public void end() {
        long runtime = System.nanoTime() - start_time;
        String output = String.format("runtime [%s]: %dns (%.2f frames)", tag, runtime, runtime/(double)NANO_PER_FRAME);
        LogTools.printDebug(DBG_TAG, output);
    }
}
