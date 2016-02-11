package jetsetilly.mandelbrot;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.GridView;

import jetsetilly.mandelbrot.Palette.PaletteActivityListAdapter;
import jetsetilly.mandelbrot.Settings.PaletteSettings;

public class PaletteActivity extends AppCompatActivity {
    private final String DBG_TAG = "palette activity";

    public static final String ACTIVITY_RESULT_PALETTE_ID = "PALETTE_ID";
    public static final Integer ACTIVITY_RESULT_NO_CHANGE = 1;
    public static final Integer ACTIVITY_RESULT_CHANGE = 2;

    private GridView palette_entries;

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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case android.R.id.home:
                // get selected palette id
                int palette_id = ((PaletteActivityListAdapter) palette_entries.getAdapter()).getSelectedPaletteID();

                // check to see if palette has changed
                if (PaletteSettings.getInstance().selected_id != palette_id) {
                    // build palette change intent
                    Intent activity_result_intent = new Intent(this, MainActivity.class);
                    activity_result_intent.putExtra(ACTIVITY_RESULT_PALETTE_ID, palette_id);

                    // set result
                    setResult(ACTIVITY_RESULT_CHANGE, activity_result_intent);
                } else {
                    setResult(ACTIVITY_RESULT_NO_CHANGE);
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
        overridePendingTransition(R.anim.from_left_nofade, R.anim.from_left_fade_out);
    }

    /* called whenever an entry has been added - used to fix flaw in Android where animations
    * are no always run (see call in PaletteActivityListAdapter()) */
    public void adapter_getView_callback() {
        palette_entries.invalidateViews();
    }
}
