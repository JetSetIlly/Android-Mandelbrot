package jetsetilly.tools;

import java.util.concurrent.Semaphore;

public class SimpleLatch {
    static private final String DBG_TAG = "SimpleLatch";

    private Semaphore latch;
    private String latch_tag;
    private String acquire_tag;

    public SimpleLatch() {
        latch_tag = null;
        reset();
    }

    public SimpleLatch(String tag) {
        latch_tag = tag;
        reset();
    }

    public void reset() {
        latch = new Semaphore(1);
    }

    public boolean tryAcquire() {
       return tryAcquire(null);
    }

    public boolean tryAcquire(String tag) {
        if (latch.tryAcquire()) {
            acquire_tag = tag;
            return true;
        }

        if (latch_tag != null || acquire_tag != null) {
            LogTools.printDebug(DBG_TAG, "latch (" + latch_tag + ") already acquired: " + acquire_tag);
        }

        return false;
    }

    public void acquire() {
        acquire(null);
    }

    public void acquire(String tag) {
        acquire_tag = tag;
        try {
            latch.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean isLatched() {
        return latch.availablePermits() < 1;
    }

    public void release() {
        if (isLatched()) {
            latch.release();
            acquire_tag = null;
        }
    }

    public void monitor() {
        try {
            latch.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        latch.release();
    }
}
