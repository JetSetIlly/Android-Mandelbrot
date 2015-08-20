package jetsetilly.mandelbrot;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import jetsetilly.mandelbrot.Widgets.BailoutSlider;
import jetsetilly.mandelbrot.Widgets.DoubleTapScaleSlider;
import jetsetilly.mandelbrot.Widgets.IterationsSlider;


public class SettingsActivity extends AppCompatActivity {
    private final String DBG_TAG = "settings activity";

    private IterationsSlider iterations;
    private BailoutSlider bailout;
    private DoubleTapScaleSlider double_tap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        iterations = (IterationsSlider) findViewById(R.id.iterations);
        bailout = (BailoutSlider) findViewById(R.id.bailout);
        double_tap = (DoubleTapScaleSlider) findViewById(R.id.doubletap);

        /* get the values that wer set on the previous screen
        they've not been committed yet so we've passed them by intent */
        Intent settings_intent = getIntent();
        int iterations_value = settings_intent.getIntExtra(getString(R.string.settings_intent_iteration_value), -1);
        iterations.set(iterations_value);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case android.R.id.home:
                if (iterations.fixate() || bailout.fixate() || double_tap.fixate()) {
                    MainActivity.render_canvas.startRender();
                }

                finish();
                setTransitionAnim();
                return true;

            case R.id.settings_action_reset:
                iterations.reset();
                bailout.reset();
                double_tap.reset();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setTransitionAnim();
    }

    /* sets animation for going back to main activity*/
    private void setTransitionAnim() {
        overridePendingTransition(R.anim.from_right_nofade, R.anim.from_right_fade_out);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }
}
