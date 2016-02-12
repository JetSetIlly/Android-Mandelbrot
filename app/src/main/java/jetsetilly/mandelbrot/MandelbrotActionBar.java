package jetsetilly.mandelbrot;

import android.content.Context;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;

public class MandelbrotActionBar extends Toolbar {
    private final String DBG_TAG = "myactionbar";

    private MainActivity context;
    private View status_bar;

    public MandelbrotActionBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MandelbrotActionBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MandelbrotActionBar(Context context) {
        super(context);
    }

    public void completeSetup(MainActivity context, String title) {
        this.context = context;
        status_bar = context.getWindow().getDecorView();

        setTitle(title);
        setY(Tools.getStatusBarHeight(context));
        hide(false);
    }

    public void hide(boolean hide) {
        hide(hide, true);
    }

    public void hide(boolean hide, boolean animate) {
        if (hide) {
            status_bar.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);

            if (getVisibility() == View.VISIBLE && animate) {
                animate().setDuration(getResources().getInteger(R.integer.action_bar_hide))
                        .translationXBy(getWidth())
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                setVisibility(INVISIBLE);
                            }
                        }).start();
            } else {
                setVisibility(View.INVISIBLE);
            }
        } else {
            status_bar.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

            if (getVisibility() == View.INVISIBLE && animate) {
                animate().setDuration(getResources().getInteger(R.integer.action_bar_show))
                        .translationXBy(-getWidth())
                        .withStartAction(new Runnable() {
                            @Override
                            public void run() {
                                setVisibility(VISIBLE);
                            }
                        }).start();
            } else {
                setVisibility(View.VISIBLE);
            }
        }
    }

    public boolean inActionBar(float y_coordinate) {
        return y_coordinate <= getHeight();
    }
}
