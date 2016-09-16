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

    private final double KICK_WAIT = 1000; // in milliseconds
    private final long END_DELAY = 1000; // in milliseconds
    private final long SESSION_PERSIST = 1000; // in milliseconds

    private long start_time = 0;
    private long end_time = 0;
    private AnimatorSet running_anim;
    private AnimatorSet on_anim;

    private AnimatorSet throb_anim;
    private Animator throb_anim_rotation;
    private long throb_start_time; // start (in milliseconds) of throb anim reset on repeat
    private boolean throb_anim_ending;

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

    private long timeToThrobEnd() {
        // the time in milliseconds until the start of the next throb anim cycle
        return throb_start_time+throb_anim_rotation.getDuration()-System.currentTimeMillis();
    }

    private void init() {
        setLayerType(LAYER_TYPE_HARDWARE, null);
        setActivityVisibility(0f);
        on_anim = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.progress_on);
        off_anim = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.progress_off);
        throb_anim = (AnimatorSet) AnimatorInflater.loadAnimator(getContext(), R.animator.progress_throb);
        throb_anim_rotation = throb_anim.getChildAnimations().get(0);
        throb_anim_ending = false;
        on_anim.setTarget(this);
        off_anim.setTarget(this);
        throb_anim.setTarget(this);

        // link animations together
        on_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                running_anim = on_anim;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                throb_anim.start();
                // throb runs forever
            }
        });

        throb_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                running_anim = throb_anim;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                throb_anim_ending = false;
                off_anim.start();
            }
        });

        throb_anim_rotation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                throb_start_time = System.currentTimeMillis();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                throb_start_time = System.currentTimeMillis();
            }
        });

        off_anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                running_anim = off_anim;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                running_anim = null;
                // off anim doesn't lead to another animation
            }
        });
    }

    public void startSession() {
        // something is already happening
        if (running_anim != null) return;

        long now = System.currentTimeMillis();

        // allow sessions to persist from one call to the next if only
        // a short amount of time has passed
        if (now - end_time < SESSION_PERSIST) return;

        // register start time
        start_time = now;
    }

    public void kick() {
        // wait until enough time has passed
        if (System.currentTimeMillis() - start_time < KICK_WAIT) return;

        // do nothing if any of the following conditions are met
        if (running_anim == on_anim) return;
        if (running_anim == throb_anim) {
            // allow throb animation to continue
            throb_anim_ending = false;
            return;
        }

        // run on-animation
        if (running_anim == off_anim) {
            // progressView is about to disappear. wait until it has done so before
            // bringing it back into view
            postOnAnimationDelayed(new Runnable() {
                @Override
                public void run() {
                    on_anim.start();
                }
            }, off_anim.getDuration());
            // getDuration() will not be completely accurate if off_anim is already running
            // but we expect the off time to be so short that it won't be noticeable
            // -- see timeToThrobEnd() for more complete solution
        } else {
            // run without delay
            on_anim.start();
        }
    }

    public void endSession() {
        // run off-animation if necessary after a short delay
        // the delay should give enough time for a new render event
        // to start without the progress view bobbing out of view

        end_time = System.currentTimeMillis();

        postOnAnimationDelayed(new Runnable() {
            @Override
            public void run() {
                if (end_time < start_time) return; // do nothing if a new session has started
                if (running_anim == off_anim || running_anim == no_anim || throb_anim_ending) return;

                if (running_anim == on_anim) {
                    running_anim.end();
                } else if (running_anim == throb_anim) {
                    throb_anim_ending = true;
                    postOnAnimationDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (running_anim == throb_anim && throb_anim_ending) {
                                throb_anim.end();
                                throb_anim_ending = false;
                            }
                        }
                    }, timeToThrobEnd());
                }
            }
        }, END_DELAY); ;
    }

    // animator property
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
