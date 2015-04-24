package jetsetilly.mandelbrot;


import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

// TODO: it seems wasteful to setup the animations each time but setting them up once doesn't seem to work. there must be a way of recycling the animation

public class ProgressView extends ImageView {
    private int spin_frame_count = 24;
    private int spin_duration = 1000;
    private int visibility_duration = 300;

    public ProgressView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ProgressView(Context context) {
        super(context);
    }

    public void setBusy() {
        final TranslateAnimation visibility_on_anim = new TranslateAnimation(getMeasuredWidth(), 0, getMeasuredHeight(), 0);
        visibility_on_anim.setDuration(visibility_duration);

        final Animation spin_anim = AnimationUtils.loadAnimation(getContext(), R.anim.progress_view_anim_spin);
        spin_anim.setDuration(spin_duration);
        spin_anim.setInterpolator(new Interpolator() {
            @Override
            public float getInterpolation(float input) {
                return (float) Math.floor(input * spin_frame_count) / spin_frame_count;
            }
        });

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

        if (!isShown()) {
            startAnimation(visibility_on_anim);
        }
    }

    public void unsetBusy() {
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
