package jetsetilly.mandelbrot;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.RadioGroup;

import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;
import jetsetilly.mandelbrot.Settings.GestureSettings;
import jetsetilly.mandelbrot.Widgets.IterationsSeekBar;
import jetsetilly.mandelbrot.Widgets.ReportingSeekBar;

public class SettingsActivity extends AppCompatActivity {
    private final String DBG_TAG = "settings activity";

    public final static String ITERATIONS_VALUE_INTENT = "ITERATIONS_VALUE";

    private IterationsSeekBar iterations;
    private ReportingSeekBar bailout;
    private ReportingSeekBar double_tap;
    private ReportingSeekBar num_passes;
    private RadioGroup render_mode;
    private int reference_render_mode;

    private final MandelbrotSettings mandelbrot_settings = MandelbrotSettings.getInstance();
    private final GestureSettings gesture_settings = GestureSettings.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // set up actionbar
        Toolbar action_bar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(action_bar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_24dp);

        iterations = (IterationsSeekBar) findViewById(R.id.iterations);
        bailout = (ReportingSeekBar) findViewById(R.id.bailout);
        double_tap = (ReportingSeekBar) findViewById(R.id.doubletap);
        num_passes = (ReportingSeekBar) findViewById(R.id.num_passes);
        render_mode = (RadioGroup) findViewById(R.id.rendermode);

        // set values
        bailout.set(mandelbrot_settings.bailout_value);
        double_tap.set(gesture_settings.double_tap_scale);
        num_passes.set(mandelbrot_settings.num_passes);

        /* get the values that wer set on the previous screen
        they've not been committed yet so we've passed them by intent */
        Intent settings_intent = getIntent();
        int iterations_value = settings_intent.getIntExtra(ITERATIONS_VALUE_INTENT, -1);
        iterations.set(iterations_value);

        // set render mode radio button
        if (mandelbrot_settings.render_mode == Mandelbrot.RenderMode.TOP_DOWN) {
            render_mode.check(R.id.rendermode_topdown);
        } else if (mandelbrot_settings.render_mode == Mandelbrot.RenderMode.CENTRE) {
            render_mode.check(R.id.rendermode_centre);
        }

        // render mode selected at start of activity -- if this changes then
        // we'll stop the current rendering process
        reference_render_mode = render_mode.getCheckedRadioButtonId();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case android.R.id.home:
                int new_render_mode = render_mode.getCheckedRadioButtonId();

                // check for new render mode
                if (new_render_mode != reference_render_mode) {
                    MainActivity.render_canvas.stopRender();

                    switch (new_render_mode) {
                        case R.id.rendermode_topdown:
                            mandelbrot_settings.render_mode = Mandelbrot.RenderMode.TOP_DOWN;
                            break;
                        case R.id.rendermode_centre:
                            mandelbrot_settings.render_mode = Mandelbrot.RenderMode.CENTRE;
                            break;
                    }
                }

                // change mandelbrot parameters and re-render as appropriate
                if (iterations.fixate() || bailout.hasChanged()) {
                    mandelbrot_settings.bailout_value = bailout.getDouble();
                    MainActivity.render_canvas.startRender();
                }

                // changes to these settings have no effect on rendering
                gesture_settings.double_tap_scale = double_tap.getFloat();
                mandelbrot_settings.num_passes = num_passes.getInteger();

                // save settings
                MandelbrotSettings.getInstance().save(this);
                GestureSettings.getInstance().save(this);

                finish();
                setTransitionAnim();
                return true;

            case R.id.settings_action_reset:
                iterations.reset();
                bailout.reset();
                double_tap.reset();
                num_passes.reset();
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
