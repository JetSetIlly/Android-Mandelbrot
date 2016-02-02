package jetsetilly.mandelbrot;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewAnimationUtils;
import android.view.ViewPropertyAnimator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import java.util.concurrent.atomic.AtomicInteger;

public class ProgressView extends ImageView {
    private final String DBG_TAG = "progress view";

    private final double PROGRESS_WAIT = 1000; // in milliseconds
    private final float PROGRESS_DELAY = (float) 0.5;   // the fraction of the total num_passes to wait before showing the progress animation
    private final int SPIN_FRAME_COUNT = 360;
    private final int SPIN_DURATION = 1000;

    private double start_time = 0.0;
    private double kick_time = 0.0;

    private final AtomicInteger busy_ct = new AtomicInteger(0);

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
        start_time = System.currentTimeMillis();
        kick_time = start_time;
    }

    public void register() {
        busy_ct.incrementAndGet();
    }

    public void kick(int pass, int num_passes, boolean show_immediately) {
        kick_time = System.currentTimeMillis();

        // quick exit if progress is already visible
        if (getVisibility() == VISIBLE) return;

        // if show_immediately is not set to true
        // make sure a suitable amount of time has passed before showing progress view
        if (!show_immediately) {
            if (!(System.currentTimeMillis() - start_time > PROGRESS_WAIT && pass <= num_passes * PROGRESS_DELAY)) {
                return;
            }
        }

        startAnimation(buildProgressAnimation());
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

        // wait for spin_anim to finish at the key frame
        Animation spin_anim = getAnimation();
        if (spin_anim == null) return;
        spin_anim.setRepeatCount(0);
    }

    private Animation buildProgressAnimation() {
        Animation show_anim = AnimationUtils.loadAnimation(getContext(), R.anim.progress_show);
        show_anim.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(Animation a) {
                // building spin animation by hand because of android bug in how repeat count is set
                // in animation xml files
                final Animation spin_anim = new RotateAnimation(0, SPIN_FRAME_COUNT, getMeasuredWidth()/2, getMeasuredHeight()/2);
                spin_anim.setRepeatCount(Animation.INFINITE);
                spin_anim.setDuration(SPIN_DURATION);

                spin_anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        // check to see if another MandelbrotThread has registered with ProgressView in the
                        // time it takes for postDelayed() to run. if it has, resume the spin animation
                        // and exit
                        if (busy_ct.get() > 0) {
                            // busy_sustain is set so kick the spin animation to continue the progress view
                            spin_anim.setRepeatCount(Animation.INFINITE);
                            startAnimation(spin_anim);
                            return;
                        }

                        final Animation hide_anim = AnimationUtils.loadAnimation(getContext(), R.anim.progress_hide);

                        hide_anim.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                clearAnimation();
                                setVisibility(INVISIBLE);
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                        });

                        startAnimation(hide_anim);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                        // check that the animation hasn't been spinning too long
                        // this shouldn't be necessary because threads should always unregister
                        // once they are done in all circumstances
                        // if the wtf message is every logged then this clearly isn't happening
                        if (System.currentTimeMillis() - kick_time > PROGRESS_WAIT ) {
                            Tools.printWTF(DBG_TAG, "spinner has been spinning too long without activity - forcing closure");
                            unregister();
                        }
                    }
                });
                startAnimation(spin_anim);
            }

            public void onAnimationRepeat(Animation a) {
            }

            public void onAnimationStart(Animation a) {
                setVisibility(VISIBLE);
            }
        });

        return show_anim;
    }
}
