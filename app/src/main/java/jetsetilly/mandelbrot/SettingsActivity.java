package jetsetilly.mandelbrot;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.SeekBar;
import android.widget.TextView;


public class SettingsActivity extends Activity implements SeekBar.OnSeekBarChangeListener {
    private final String DBG_TAG = "settings activity";

    private SettingsMandelbrot mandelbrot_settings = SettingsMandelbrot.getInstance();
    private boolean dirty_settings;

    private final int BAILOUT_SCALE = 10;
    private final double BAILOUT_MAX = 32.0;
    private int iteration_min;
    private int iteration_max;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SeekBar iterations =  (SeekBar) findViewById(R.id.seek_iterations);
        SeekBar bailout =  (SeekBar) findViewById(R.id.seek_bailout);

        iterations.setOnSeekBarChangeListener(this);
        bailout.setOnSeekBarChangeListener(this);

        iteration_min = (int) (mandelbrot_settings.max_iterations * 0.25);
        iteration_max = (int) (mandelbrot_settings.max_iterations * 1.5);

        iterations.setMax(iteration_max);
        iterations.setProgress(mandelbrot_settings.max_iterations - iteration_min);

        bailout.setMax((int) BAILOUT_MAX * BAILOUT_SCALE);
        bailout.setProgress((int) (mandelbrot_settings.bailout_value * BAILOUT_SCALE));

        dirty_settings = false;
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

        dirty_settings = true;
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
                break;

            case R.id.seek_bailout:
                TextView bailout = (TextView) findViewById(R.id.bailout);
                mandelbrot_settings.bailout_value = Double.parseDouble(bailout.getText().toString());
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
        overridePendingTransition(R.animator.push_up_fade_in, R.animator.push_up_fade_out);
    }
}
