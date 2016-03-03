package jetsetilly.mandelbrot;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.GridView;

import jetsetilly.mandelbrot.Palette.PaletteActivityListAdapter;
import jetsetilly.mandelbrot.Settings.PaletteSettings;
import jetsetilly.mandelbrot.Widgets.ReportingSeekBar;

public class PaletteActivity extends AppCompatActivity {
    private final String DBG_TAG = "palette activity";

    public static final Integer ACTIVITY_RESULT_CHANGE = 1;
    public static final String ACTIVITY_RESULT_PALETTE_ID = "PALETTE_ID";
    public static final String ACTIVITY_RESULT_PALETTE_SMOOTHNESS = "PALETTE_SMOOTHNESS";

    private GridView palette_entries;
    private ReportingSeekBar smoothness;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palette);

        // set up actionbar
        Toolbar action_bar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(action_bar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_arrow_back_24dp);

        palette_entries = (GridView) findViewById(R.id.palette_entries);
        palette_entries.setAdapter(new PaletteActivityListAdapter(this));

        smoothness = (ReportingSeekBar) findViewById(R.id.smoothness);
        smoothness.set(PaletteSettings.getInstance().smoothness);

        smoothness.onSeekBarChange = new Runnable() {
            @Override
            public void run() {
                smoothnessSeekbarVisibility(false);
            }
        };
    }

    private void smoothnessSeekbarVisibility(final boolean visible){
        if ((visible && smoothness.getVisibility() == View.VISIBLE) ||(!visible && smoothness.getVisibility() == View.INVISIBLE))
            return;

        ViewPropertyAnimator anim = smoothness.animate();
        anim.withLayer();
        anim.setDuration(getResources().getInteger(R.integer.palette_smoothness_control_fade));

        if (visible) {
            smoothness.setAlpha(0f);
            smoothness.setVisibility(View.VISIBLE);
            anim.alpha(1f);

        } else {
            anim.alpha(0f);
            anim.withEndAction(new Runnable() {
                @Override
                public void run() {
                    smoothness.setAlpha(1f);
                    smoothness.setVisibility(View.INVISIBLE);
                }
            });
        }

        anim.start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.palette_action_smoothness:
                if (smoothness.getVisibility() == View.VISIBLE)
                    smoothnessSeekbarVisibility(false);
                else
                    smoothnessSeekbarVisibility(true);
                break;

            case android.R.id.home:
                Intent activity_result_intent = new Intent(this, MainActivity.class);
                activity_result_intent.putExtra(ACTIVITY_RESULT_PALETTE_SMOOTHNESS,
                        smoothness.getInteger());
                activity_result_intent.putExtra(ACTIVITY_RESULT_PALETTE_ID,
                    ((PaletteActivityListAdapter) palette_entries.getAdapter()).getSelectedPaletteID());
                setResult(ACTIVITY_RESULT_CHANGE, activity_result_intent);

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
        overridePendingTransition(R.anim.from_left_nofade, R.anim.from_left_fade_out);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_palette_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /* called whenever an entry has been added - used to fix flaw in Android where animations
    * are no always run (see call in PaletteActivityListAdapter()) */
    public void adapter_getView_callback() {
        palette_entries.invalidateViews();
    }
}
