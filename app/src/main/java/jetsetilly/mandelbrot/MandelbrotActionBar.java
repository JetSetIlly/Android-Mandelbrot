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
        alpha = getAlpha();
        setTitle(title);
        setY(getStatusBarHeight(context));
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
                        .withLayer()
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
