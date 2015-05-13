package jetsetilly.mandelbrot;


import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

// TODO: it seems wasteful to setup the animations each time but setting them up once doesn't seem to work. there must be a way of recycling the animation

public class ProgressView extends ImageView {
    private final double PROGRESS_WAIT = 1000000000; // in nanoseconds
    private final float PROGRESS_DELAY = (float) 0.5;   // the fraction of the total num_passes to wait before showing the progress animation

    private int spin_frame_count = 360;
    private int spin_duration = 1000;
    private int visibility_duration = 300;

    private double start_time = 0.0;

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
        if (isShown()) return;

        if (start_time == 0.0) {
            start_time = System.nanoTime();
        }

        if (!(System.nanoTime() - start_time > PROGRESS_WAIT && pass <= num_passes * PROGRESS_DELAY)) {
            return;
        }

        final TranslateAnimation visibility_on_anim = new TranslateAnimation(getMeasuredWidth(), 0, getMeasuredHeight(), 0);
        visibility_on_anim.setDuration(visibility_duration);

        final Animation spin_anim = new RotateAnimation(0, spin_frame_count, getMeasuredWidth()/2, getMeasuredHeight()/2);
        spin_anim.setRepeatCount(Animation.INFINITE);
        spin_anim.setDuration(spin_duration);

        /*
        spin_anim.setInterpolator(new Interpolator() {
            @Override
            public float getInterpolation(float input) {
                return (float) Math.floor(input * spin_frame_count) / spin_frame_count;
            }
        });
        */

        // listener for visibility_on_anim because so that we can string
        // it together with the spin animation
        visibility_on_anim.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(Animation a) {
                startAnimation(spin_anim);
            }

            public void onAnimationRepeat(Animation a) {
            }

            public void onAnimationStart(Animation a) {
                setVisibility(VISIBLE);
            }
        });

        startAnimation(visibility_on_anim);
    }

    public void unsetBusy() {
        // reset start_time before checking isShown() below
        start_time = 0.0;

        // quick exit if progress is not visible
        if (!isShown()) return;

        Animation spinner;
        long delay = 0;

        final TranslateAnimation visibility_off_anim = new TranslateAnimation(0, getMeasuredWidth(), 0, getMeasuredHeight());
        visibility_off_anim.setDuration(visibility_duration);

        spinner = getAnimation();
        if (spinner != null) {
            // wait for spinner to finish at the key frame
            spinner.setRepeatCount(0);

            // wait until the spinning has finished before starting "off" animation
            delay = spinner.computeDurationHint();
        }

        // wait until spinner has finished
        postDelayed(new Runnable() {
            public void run() {
                startAnimation(visibility_off_anim);

                // inelegant way of handling invisibility
                // it would be nice to have this inside a listener as in
                // the visibility_on_anim above but we can't call clearAnimation()
                // inside the listener and we need to call that for setVisibility(INVISIBLE) to work
                postDelayed(new Runnable() {
                    public void run() {
                        clearAnimation();
                        setVisibility(INVISIBLE);
                    }
                }, visibility_duration);
            }
        }, delay);
    }
}
