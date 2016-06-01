package jetsetilly.mandelbrot;

import android.content.Context;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;

public class MandelbrotActionBar extends Toolbar {
    private final String DBG_TAG = "mandelbrot actionbar";

    private View status_bar;
    private float alpha;

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
        status_bar = context.getWindow().getDecorView();
        alpha = getAlpha();
        setTitle(title);
        setY(Tools.getStatusBarHeight(context));
        setVisibility(false);
    }

    public void show_noanim() {
        if (getVisibility() == INVISIBLE) {
            status_bar.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            setX(0f);
            setVisibility(VISIBLE);
        }
    }

    public void setVisibility(boolean hide) {
        if (hide) {
            status_bar.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);

            if (getVisibility() == View.VISIBLE) {
                animate().setDuration(getResources().getInteger(R.integer.action_bar_hide))
                        .x(getWidth()/4)
                        .alpha(0.0f)
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                setVisibility(INVISIBLE);
                            }
                        }).start();
            }
        } else {
            status_bar.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

            if (getVisibility() == View.INVISIBLE) {
                animate().setDuration(getResources().getInteger(R.integer.action_bar_show))
                        .x(0)
                        .alpha(alpha)
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .withStartAction(new Runnable() {
                            @Override
                            public void run() {
                                setVisibility(VISIBLE);
                            }
                        }).start();
            }
        }
    }

    public boolean inActionBar(float y_coordinate) {
        return y_coordinate <= getHeight();
    }
}
