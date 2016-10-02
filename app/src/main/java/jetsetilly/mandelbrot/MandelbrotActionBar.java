package jetsetilly.mandelbrot;

import android.content.Context;
import android.graphics.RectF;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;

public class MandelbrotActionBar extends Toolbar {
    private final String DBG_TAG = "mandelbrot actionbar";

    private View status_bar;
    private RectF hotspot;

    // values of properties take when actionbar is visible
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

    public void initialise(MainActivity context, String title) {
        status_bar = context.getWindow().getDecorView();
        setTitle(title);

        /* get status bar height and position action bar just underneath it */
        {
            int status_bar_height = 0;
            int status_bar_id = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (status_bar_id > 0) {
                status_bar_height = context.getResources().getDimensionPixelSize(status_bar_id);
            }
            setY(status_bar_height);
        }

        visible_alpha = getAlpha();
        visible_y = getY();
        setVisibility(false);

        post(new Runnable() {
            @Override
            public void run() {
                hotspot = new RectF(0.0f, 0.0f, getWidth(), getHeight());
            }
        });
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

    public boolean hotspot(float x, float y) {
        return hotspot.contains(x, y);
    }
}
