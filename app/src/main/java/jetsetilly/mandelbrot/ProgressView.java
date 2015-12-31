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

    private AtomicInteger busy_ct = new AtomicInteger(0);
    private AtomicBoolean busy_sustain = new AtomicBoolean(false);

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
        if (getVisibility() == VISIBLE) {
            // progress view is already visible so set busy_sustain to true. busy_sustain
            // is checked during the postDelayed() runnable launched in the unregister() method.
            busy_sustain.set(true);
        }

        start_time = System.nanoTime();
    }

    public void register() {
        busy_ct.incrementAndGet();
    }

    public void kick(int pass, int num_passes, boolean show_immediately) {
        // quick exit if progress is already visible
        if (getVisibility() == VISIBLE) return;

        // KLUDGE
        // end sustain -- we don't the variable getting stuck at true - this may happen if it is
        // set in between the postDelayed() check and the resetting back to false that occurs as a result
        // of the check
        busy_sustain.set(false);

        // if show_immediately is not set to true
        // make sure a suitable amount of time has passed before showing progress view
        if (!show_immediately) {
            if (!(System.nanoTime() - start_time > PROGRESS_WAIT && pass <= num_passes * PROGRESS_DELAY)) {
                return;
            }
        }

        Animation show_anim = AnimationUtils.loadAnimation(getContext(), R.anim.progress_show);

        // set visibility on animation start
        // and begin spin on animation end
        show_anim.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(Animation a) {
                startAnimation(getSpinAnimation());
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

        // set count to zero in case decrement took it below zero
        // shouldn't happen really
        busy_ct.set(0);

        // quick exit if progress is not visible
        if (getVisibility() == INVISIBLE) {
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
                if (busy_sustain.get()) {
                    // busy_sustain is set so kick the spin animation to continue the progress view
                    startAnimation(getSpinAnimation());
                    busy_sustain.set(false);
                    return;
                }

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

    private Animation getSpinAnimation() {
        // building spin animation by hand because of android bug in how repeat count is set
        // in animation xml files
        Animation spin_anim = new RotateAnimation(0, SPIN_FRAME_COUNT, getMeasuredWidth()/2, getMeasuredHeight()/2);
        spin_anim.setRepeatCount(Animation.INFINITE);
        spin_anim.setDuration(SPIN_DURATION);
        return spin_anim;
    }
}
