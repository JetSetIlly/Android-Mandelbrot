package jetsetilly.mandelbrot;

import android.content.Context;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;

public class MandelbrotActionBar extends Toolbar {
    private final String DBG_TAG = "mandelbrot actionbar";

    private View status_bar;
    private float visible_alpha;
    private float visible_y;

    public MandelbrotActionBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MandelbrotActionBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MandelbrotActionBar(Context context) {
        super(context);
    }

    private int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public void completeSetup(MainActivity context, String title) {
        setLayerType(LAYER_TYPE_HARDWARE, null);
        status_bar = context.getWindow().getDecorView();
        setTitle(title);
        setY(getStatusBarHeight(context));
        visible_alpha = getAlpha();
        visible_y = getY();
        setVisibility(false);
    }

    public void enforceVisibility() {
        if (getVisibility() == INVISIBLE) {
            status_bar.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);
            setX(0f);
            setVisibility(VISIBLE);
        }
    }

    public void setVisibility(boolean hide) {
        if (hide) {
            status_bar.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN);

            if (getVisibility() == View.VISIBLE) {
                animate().setDuration(getResources().getInteger(R.integer.action_bar_hide))
                        .alpha(0.0f)
                        .y(getY() + getHeight()/4)
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
                        .alpha(visible_alpha)
                        .y(visible_y)
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
