package jetsetilly.mandelbrot;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;

import jetsetilly.mandelbrot.Mandelbrot.Settings;


public class SettingsActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {
    private final String DBG_TAG = "settings activity";

    private Settings mandelbrot_settings = Settings.getInstance();

    private boolean dirty_settings;
    private int rendered_iterations;
    private double rendered_bailout;

    private final int BAILOUT_SCALE = 10;
    private final double BAILOUT_MAX = 32.0;
    private int iteration_min;
    private int iteration_max;

    private SeekBar iterations;
    private SeekBar bailout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Intent settings_intent = getIntent();
        dirty_settings = settings_intent.getBooleanExtra(getString(R.string.settings_intent_dirty_settings), false);
        rendered_iterations = settings_intent.getIntExtra(getString(R.string.settings_intent_rendered_iterations), -1);
        rendered_bailout = mandelbrot_settings.bailout_value;

        iterations =  (SeekBar) findViewById(R.id.seek_iterations);
        bailout =  (SeekBar) findViewById(R.id.seek_bailout);

        iterations.setOnSeekBarChangeListener(this);
        bailout.setOnSeekBarChangeListener(this);

        iteration_min = (int) (mandelbrot_settings.max_iterations * 0.25);
        iteration_max = (int) (mandelbrot_settings.max_iterations * 1.5);

        iterations.setMax(iteration_max);
        iterations.setProgress(mandelbrot_settings.max_iterations - iteration_min);

        bailout.setMax((int) BAILOUT_MAX * BAILOUT_SCALE);
        bailout.setProgress((int) (mandelbrot_settings.bailout_value * BAILOUT_SCALE));
    }

    @Override
    public void onProgressChanged(SeekBar seek_bar, int progress, boolean fromUser) {
        switch (seek_bar.getId()) {
            case R.id.seek_iterations:
                TextView iterations = (TextView) findViewById(R.id.iterations);
                iterations.setText("" + (seek_bar.getProgress() + iteration_min));
                break;

            case R.id.seek_bailout:
                TextView bailout = (TextView) findViewById(R.id.bailout);
                bailout.setText("" + ((float) seek_bar.getProgress() / BAILOUT_SCALE));

                break;
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seek_bar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seek_bar) {
        switch (seek_bar.getId()) {
            case R.id.seek_iterations:
                TextView iterations = (TextView) findViewById(R.id.iterations);
                mandelbrot_settings.max_iterations = Integer.parseInt(iterations.getText().toString());
                dirty_settings = mandelbrot_settings.max_iterations != rendered_iterations;
                break;

            case R.id.seek_bailout:
                TextView bailout = (TextView) findViewById(R.id.bailout);
                mandelbrot_settings.bailout_value = Double.parseDouble(bailout.getText().toString());
                dirty_settings = mandelbrot_settings.bailout_value != rendered_bailout;
                break;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case android.R.id.home:
                if (dirty_settings) {
                    MainActivity.render_canvas.startRender();
                }

                finish();
                setTransitionAnim();
                return true;

            case R.id.settings_action_reset:
                iterations.setProgress(rendered_iterations - iteration_min);
                bailout.setProgress((int) rendered_bailout * BAILOUT_SCALE);
                dirty_settings = false;
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
