package jetsetilly.mandelbrot;


import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// TODO: it seems wasteful to setup the animations each time but setting them up once doesn't seem to work. there must be a way of recycling the animation

public class ProgressView extends ImageView {
    private final String DBG_TAG = "progress view";

    private final double PROGRESS_WAIT = 1000000000; // in nanoseconds
    private final float PROGRESS_DELAY = (float) 0.5;   // the fraction of the total num_passes to wait before showing the progress animation
    private final int SPIN_FRAME_COUNT = 360;
    private final int SPIN_DURATION = 1000;
    private double start_time = 0.0;

    private AtomicBoolean set_busy = new AtomicBoolean(false);
    private AtomicInteger busy_ct = new AtomicInteger(0);

    public ProgressView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgressView(Context context) {
        super(context);
    }

    public void startSession() {
        start_time = System.nanoTime();
        set_busy.set(false);
        busy_ct.set(0);
    }

    public void register() {
        busy_ct.incrementAndGet();
    }

    public void kick(int pass, int num_passes, boolean show_immediately) {
        assert busy_ct.get() > 0;

        // quick exit if progress is already visible
        if (set_busy.get()) return;

        // if show_immediately is not set to true
        // make sure a suitable amount of time has passed before showing progress view
        if (!show_immediately) {
            if (!(System.nanoTime() - start_time > PROGRESS_WAIT && pass <= num_passes * PROGRESS_DELAY)) {
                return;
            }
        }

        set_busy.set(true);

        Animation show_anim = AnimationUtils.loadAnimation(getContext(), R.anim.progress_show);

        // building spin animation by hand because of android bug in how repeatcount is set
        // in animation xml files
        final Animation spin_anim = new RotateAnimation(0, SPIN_FRAME_COUNT, getMeasuredWidth()/2, getMeasuredHeight()/2);
        spin_anim.setRepeatCount(Animation.INFINITE);
        spin_anim.setDuration(SPIN_DURATION);

        // set visibility on animation start
        // and begin spin on animation end
        show_anim.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(Animation a) {
                startAnimation(spin_anim);
            }

            public void onAnimationRepeat(Animation a) {
            }

            public void onAnimationStart(Animation a) {
                setVisibility(VISIBLE);
            }
        });

        startAnimation(show_anim);
    }

    public void unregister() {
        // quick exit if this isn't the last thread to unregister
        if ( busy_ct.decrementAndGet() > 0 ) {
            return;
        }
        assert busy_ct.get() == 0;

        // quick exit if progress is not visible
        if (!set_busy.getAndSet(false)) {
            return;
        }

        final Animation hide_anim = AnimationUtils.loadAnimation(getContext(), R.anim.progress_hide);

        long delay;
        Animation spin_anim = getAnimation();
        if (spin_anim != null) {
            // wait for spin_anim to finish at the key frame
            spin_anim.setRepeatCount(0);
            delay = spin_anim.computeDurationHint();
        } else {
            delay = 0;
        }

        // wait until spinner has finished
        postDelayed(new Runnable() {
            public void run() {
                startAnimation(hide_anim);

                // inelegant way of handling invisibility
                // it would be nice to have this inside a listener as in
                // the visibility_on_anim above but we can't call clearAnimation()
                // inside the listener and we need to call that for setVisibility(INVISIBLE) to work
                postDelayed(new Runnable() {
                    public void run() {
                        clearAnimation();
                        setVisibility(INVISIBLE);
                    }
                }, hide_anim.getDuration());
            }
        }, delay);
    }
}
