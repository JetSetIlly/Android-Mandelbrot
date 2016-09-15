package jetsetilly.tools;

import java.util.concurrent.Semaphore;

public class SimpleLatch {
    static private final String DBG_TAG = "SimpleLatch";

    private Semaphore latch;

    public SimpleLatch() {
        reset();
    }

    public void reset() {
        latch = new Semaphore(1);
    }

    public boolean tryAcquire() {
        return latch.tryAcquire();
    }

    public void acquire() {
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
