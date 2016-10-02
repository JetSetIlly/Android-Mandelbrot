package jetsetilly.mandelbrot;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;

public class Throbber extends ImageView {
    private final String DBG_TAG = "throbber";

    private final long THROB_DELAY = 1000;
    private final long SESSION_PERSIST_PERIOD = 2000;
    private long session_start_time = 0;
    private long session_end_time = 0;

    private boolean session_ending;
    private Runnable set_invisible_runnable = new Runnable() {
        @Override
        public void run() {
            if (session_ending) {
                ViewPropertyAnimator anim = animate();
                anim.alpha(0.0f);
                anim.withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        setVisibility(INVISIBLE);
                        setAlpha(1.0f);
                        session_ending = false;
                    }
                });
                anim.start();
            }
        }
    };

    public Throbber(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Throbber(Context context) {
        super(context);
        init();
    }

    private void init() {
        setVisibility(INVISIBLE);
    }

    public void startSession() {
        if (session_ending) {
            removeCallbacks(set_invisible_runnable);
            session_ending = false;
        }

        long now_time = System.currentTimeMillis();
        if (now_time - session_end_time >= SESSION_PERSIST_PERIOD) {
           session_start_time = now_time;
        }
    }

    public void kick() {
        if (getVisibility() == INVISIBLE) {
            if (System.currentTimeMillis() - session_start_time < THROB_DELAY)
                return;
            setVisibility(VISIBLE);
        }
    }

    public void endSession() {
        session_end_time = System.currentTimeMillis();
        if (getVisibility() == VISIBLE) {
            session_ending = true;
            postDelayed(set_invisible_runnable, SESSION_PERSIST_PERIOD);
        }
    }
}
