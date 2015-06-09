package jetsetilly.mandelbrot;


import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

// TODO: it seems wasteful to setup the animations each time but setting them up once doesn't seem to work. there must be a way of recycling the animation

public class ProgressView extends ImageView {
    private final String DBG_TAG = "progress view";

    private final double PROGRESS_WAIT = 1000000000; // in nanoseconds
    private final float PROGRESS_DELAY = (float) 0.5;   // the fraction of the total num_passes to wait before showing the progress animation
    private final int SPIN_FRAME_COUNT = 360;
    private final int SPIN_DURATION = 1000;
    private double start_time = 0.0;

    // set_busy keeps track of where in the setBusy/unsetBusy sequence we are
    // this is more reliable that checking for visibility because it is possible
    // that a second call to unsetBusy will sneak in between the visibility check
    // and the point at which we make the view invisible
    // in other words: setting the set_busy flag is near enough atomic
    // TODO: implement a proper atomic latch (semaphore)
    private boolean set_busy = false;

    public ProgressView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgressView(Context context) {
        super(context);
    }

    public void setBusy(int pass, int num_passes) {
        // quick exit if progress is already visible
        if (set_busy) return;
        set_busy = true;

        // make sure a suitable amount of time has passed before showing progress view
        if (start_time == 0.0) {
            start_time = System.nanoTime();
        }
        if (!(System.nanoTime() - start_time > PROGRESS_WAIT && pass <= num_passes * PROGRESS_DELAY)) {
            return;
        }

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

    public void unsetBusy() {
        // reset start_time before checking isShown() below
        start_time = 0.0;

        // quick exit if progress is not visible
        if (!set_busy) return;
        set_busy = false;

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
