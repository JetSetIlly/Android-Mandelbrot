package jetsetilly.mandelbrot;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;
import java.util.concurrent.atomic.AtomicInteger;

public class ProgressView extends ImageView {
    private final String DBG_TAG = "progress view";

    private final double PROGRESS_WAIT = 1000; // in milliseconds
    private final int SPIN_FRAME_COUNT = 360;
    private final int SPIN_DURATION = 1000;

    private double start_time = 0.0;
    private double kick_time = 0.0;

    private final AtomicInteger busy_ct = new AtomicInteger(0);

    private AnimatorSet anim;

    public ProgressView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setActivityVisibility(0f);
    }

    public ProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setActivityVisibility(0f);
    }

    public ProgressView(Context context) {
        super(context);
        setActivityVisibility(0f);
    }

    public void startSession() {
        start_time = System.currentTimeMillis();
        kick_time = start_time;
    }

    public void register() {
        busy_ct.incrementAndGet();
    }

    public void kick(boolean show_immediately) {
        kick_time = System.currentTimeMillis();

        // quick exit if progress is already visible
        if (getActivityVisibility() > 0f) return;

        // if show_immediately is not set to true
        // make sure a suitable amount of time has passed before showing progress view
        if (!show_immediately) {
            if (System.currentTimeMillis() - start_time < PROGRESS_WAIT) {
                return;
            }
        }

        anim = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.progress_on);
        anim.setTarget(this);
        anim.start();
    }

    public void unregister() {
        // quick exit if this isn't the last thread to unregister
        if ( busy_ct.decrementAndGet() > 0 ) {
            return;
        }

        // set count to zero in case decrement took it below zero
        // shouldn't happen really
        busy_ct.set(0);

        // do nothing else if there is no animation
        if (anim == null) return;

        // cancel existing animation
        anim.cancel();

        // set up and run off animation
        anim = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.progress_off);
        anim.setTarget(this);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                anim = null;
            }
        });
        anim.start();
    }

    public void setActivityVisibility(final float value) {
        if (value == 0f) {
            setVisibility(INVISIBLE);
            setRotation(0f);
        } else {
            setVisibility(VISIBLE);
        }

        setAlpha(value);
        setTranslationX((1-value) * getWidth());
        setTranslationY((1-value) * getHeight());
    }

    public float getActivityVisibility() {
        return getAlpha();
    }
}
