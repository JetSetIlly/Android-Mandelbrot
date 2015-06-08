package jetsetilly.mandelbrot;

import android.content.Context;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public class MyActionBar extends Toolbar {
    private final String DBG_TAG = "myactionbar";

    private MainActivity context;
    private View status_bar;

    public MyActionBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MyActionBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyActionBar(Context context) {
        super(context);
    }

    public void completeSetup(MainActivity context) {
        this.context = context;
        status_bar = context.getWindow().getDecorView();

        setTitle(R.string.app_name);
        setY(Tools.getStatusBarHeight(context));
        hide(false);
    }

    public void hide(boolean hide) {
        if (hide) {
            status_bar.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);

            if (getVisibility() != View.INVISIBLE) {
                Animation anim = AnimationUtils.loadAnimation(context, R.anim.action_bar_hide);

                // set invisibility on animation end
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                startAnimation(anim);
            }
        } else {
            status_bar.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

            if (getVisibility() != View.VISIBLE) {
                Animation anim = AnimationUtils.loadAnimation(context, R.anim.action_bar_show);

                // set visibility on animation start
                anim.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

                startAnimation(anim);
            }
        }
    }

    public boolean inActionBar(float y_coordinate) {
        return y_coordinate <= getHeight();
    }
}
