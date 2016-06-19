package jetsetilly.mandelbrot;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class ProgressView extends ImageView {
    private final String DBG_TAG = "progress view";

    private final double PROGRESS_WAIT = 1000; // in milliseconds
    private final long UNREGISTER_DELAY = 1000; // in milliseconds

    private long start_time = 0;
    private AnimatorSet running_anim;
    private AnimatorSet on_anim;
    private AnimatorSet throb_anim;
    private AnimatorSet off_anim;
    final private AnimatorSet no_anim = null;

    public ProgressView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public ProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProgressView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setActivityVisibility(0f);
        on_anim = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.progress_on);
        off_anim = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.progress_off);
        throb_anim = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.progress_throb);
        on_anim.setTarget(this);
        off_anim.setTarget(this);
        throb_anim.setTarget(this);

        // link animations together
        on_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                running_anim = throb_anim;
                throb_anim.start();
                // throb runs forever
            }
        });

        off_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                running_anim = null;
                // off anim doesn't lead to another animation
            }
        });
    }

    public void startSession() {
        start_time = System.currentTimeMillis();
    }

    public void kick(boolean show_immediately) {
        // if show_immediately is not set to true
        // make sure a suitable amount of time has passed before showing progress view
        if (!show_immediately) {
            if (System.currentTimeMillis() - start_time < PROGRESS_WAIT) {
                return;
            }
        }

        // run on-animation if necessary
        if (running_anim == on_anim || running_anim == throb_anim ) return;
        if (running_anim == off_anim) running_anim.cancel();
        running_anim = on_anim;
        on_anim.start();
    }

    public void unregister() {
        // run off-animation if necessary after a short delay
        // the delay should give enough time for a new render event
        // to start without the progress view bobbing out of view

        final long unregister_time = System.currentTimeMillis();

        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (unregister_time < start_time) return; // do nothing if a new session has started
                if (running_anim == off_anim || running_anim == no_anim) return;
                running_anim.cancel(); // running_anim == on_anim || running_anim == throb_anim
                running_anim = off_anim;
                off_anim.start();
            }
        }, UNREGISTER_DELAY);
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
}
