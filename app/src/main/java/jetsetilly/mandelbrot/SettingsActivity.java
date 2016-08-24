package jetsetilly.mandelbrot;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.Switch;

import jetsetilly.mandelbrot.Mandelbrot.Mandelbrot;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;
import jetsetilly.mandelbrot.Settings.GestureSettings;
import jetsetilly.mandelbrot.Settings.SystemSettings;
import jetsetilly.mandelbrot.View.IterationsRateSeekBar;
import jetsetilly.mandelbrot.View.IterationsSeekBar;
import jetsetilly.mandelbrot.View.ReportingSeekBar;

public class SettingsActivity extends AppCompatActivity {
    private final String DBG_TAG = "settings activity";

    public final static String INITIAL_ITERATIONS_VALUE = "INITIAL_ITERATIONS_VALUE";
    public static final Integer ACTIVITY_RESULT_NO_RENDER = 1;
    public static final Integer ACTIVITY_RESULT_RENDER = 2;
    public static final Integer ACTIVITY_RESULTS_REINITIALISE = 3;

    private IterationsSeekBar iterations;
    private IterationsRateSeekBar iterations_rate;
    private ReportingSeekBar bailout;
    private ReportingSeekBar double_tap;
    private ReportingSeekBar num_passes;
    private RadioGroup render_mode;
    private Switch deep_colour;
    private RadioGroup orientation;

    private final MandelbrotSettings mandelbrot_settings = MandelbrotSettings.getInstance();
    private final GestureSettings gesture_settings = GestureSettings.getInstance();
    private final SystemSettings system_settings = SystemSettings.getInstance();

    private int initial_iterations_value;

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
        iterations_rate = (IterationsRateSeekBar) findViewById(R.id.iterations_rate);
        bailout = (ReportingSeekBar) findViewById(R.id.bailout);
        double_tap = (ReportingSeekBar) findViewById(R.id.doubletap);
        num_passes = (ReportingSeekBar) findViewById(R.id.num_passes);
        render_mode = (RadioGroup) findViewById(R.id.rendermode);
        deep_colour = (Switch) findViewById(R.id.deep_colour) ;
        orientation = (RadioGroup) findViewById(R.id.orientation);

        // get the max_iterations as passed by intent
        Intent settings_intent = getIntent();
        initial_iterations_value = settings_intent.getIntExtra(INITIAL_ITERATIONS_VALUE, mandelbrot_settings.max_iterations);

        // if orientation option changes, call apply settings to immediately
        // reflect the change for this activity
        orientation.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                applyOrientation();
            }
        });

        // add/remove additional settings if rendermode changes
        render_mode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // TODO: animate visibility
                switch (checkedId) {
                    case R.id.rendermode_hardware:
                        num_passes.setVisibility(View.GONE);
                        break;
                    default:
                        num_passes.setVisibility(View.VISIBLE);
                        break;
                }
            }
        });

        // set values and apply settings
        setValues();
        applyOrientation();
    }

    private void applyOrientation() {
        switch(orientation.getCheckedRadioButtonId()) {
            case R.id.orientation_portrait:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;

            case R.id.orientation_sensor:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
                break;
        }
    }

    private void setValues() {
        // set values
        iterations.set(initial_iterations_value);
        iterations_rate.reset();
        bailout.set(mandelbrot_settings.bailout_value);
        double_tap.set(gesture_settings.double_tap_scale);
        num_passes.set(mandelbrot_settings.num_passes);

        // set render mode radio button
        if (mandelbrot_settings.render_mode == Mandelbrot.RenderMode.HARDWARE) {
            render_mode.check(R.id.rendermode_hardware);
        } else if (mandelbrot_settings.render_mode == Mandelbrot.RenderMode.SOFTWARE_TOP_DOWN) {
            render_mode.check(R.id.rendermode_topdown);
        } else if (mandelbrot_settings.render_mode == Mandelbrot.RenderMode.SOFTWARE_CENTRE) {
            render_mode.check(R.id.rendermode_centre);
        }

        // deep colour switch
        deep_colour.setChecked(system_settings.deep_colour);

        // set screen orientation radio button
        if (system_settings.allow_screen_rotation) {
            orientation.check(R.id.orientation_sensor);
        } else {
            orientation.check(R.id.orientation_portrait);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case android.R.id.home:
                // return ACTIVITY_RESULT_NO_RENDER by default, return ACTIVITY_RESULT_RENDER under
                // specific circumstances (see below)
                setResult(ACTIVITY_RESULT_NO_RENDER);

                // changes to these settings have no effect on final render image
                switch (render_mode.getCheckedRadioButtonId()) {
                    case R.id.rendermode_hardware:
                        mandelbrot_settings.render_mode = Mandelbrot.RenderMode.HARDWARE;
                        break;
                    case R.id.rendermode_topdown:
                        mandelbrot_settings.render_mode = Mandelbrot.RenderMode.SOFTWARE_TOP_DOWN;
                        break;
                    case R.id.rendermode_centre:
                        mandelbrot_settings.render_mode = Mandelbrot.RenderMode.SOFTWARE_CENTRE;
                        break;
                }

                switch(orientation.getCheckedRadioButtonId()) {
                    case R.id.orientation_portrait:
                        system_settings.allow_screen_rotation = false;
                        break;

                    case R.id.orientation_sensor:
                        system_settings.allow_screen_rotation = true;
                        break;
                }

                gesture_settings.double_tap_scale = double_tap.getFloat();
                mandelbrot_settings.num_passes = num_passes.getInteger();
                mandelbrot_settings.iterations_rate = iterations_rate.getProgress();

                // changes to these settings DO have an effect on final render image
                if (iterations.hasChanged() || bailout.hasChanged()) {
                    mandelbrot_settings.max_iterations = iterations.getInteger();
                    mandelbrot_settings.bailout_value = bailout.getDouble();
                    setResult(ACTIVITY_RESULT_RENDER);
                }

                // changes to deep_colour setting require reinitialisation
                if (system_settings.deep_colour != deep_colour.isChecked()) {
                    system_settings.deep_colour = deep_colour.isChecked();
                    setResult(ACTIVITY_RESULTS_REINITIALISE);
                }

                finish();
                setTransitionAnim();
                return true;

            case R.id.settings_action_reset:
                setValues();
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
        overridePendingTransition(R.anim.slide_from_right, R.anim.slide_from_right_with_fade);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }
}
