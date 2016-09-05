package jetsetilly.mandelbrot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;

import jetsetilly.mandelbrot.Palette.PaletteAdapter;
import jetsetilly.mandelbrot.Settings.Settings;

public class PaletteActivity extends AppCompatActivity {
    private final String DBG_TAG = "palette activity";

    // result of activity - received by MainActivity.onActivityResult()
    public static final Integer RESULT_NO_CHANGE = 1;
    public static final Integer RESULT_CHANGE = 2;

    private GridView palette_entries;
    private PaletteAdapter palette_adapter;
    private int palette_smoothness;

    private DialogReceiver dialog_receiver;

    private final Settings settings = Settings.getInstance();

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
        palette_adapter = new PaletteAdapter(this);
        palette_entries.setAdapter(palette_adapter);
        palette_smoothness = settings.palette_smoothness;

        // create new DialogReceiver
        dialog_receiver = new DialogReceiver();

        // apply orientation settings
        applyOrientation();
    }

    private void applyOrientation() {
        if (settings.allow_screen_rotation) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dialog_receiver);
    }

    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(dialog_receiver, new IntentFilter(SmoothnessDialog.RESULT_ID));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.palette_action_smoothness:
                SmoothnessDialog smoothness_dialog = new SmoothnessDialog();
                Bundle args = new Bundle();
                args.putInt(SmoothnessDialog.INIT_PARAMS, palette_smoothness);
                smoothness_dialog.setArguments(args);
                smoothness_dialog.show(getFragmentManager(), null);
                break;

            case android.R.id.home:
                String selected_palette_id = palette_adapter.getSelectedPaletteID();
                if (selected_palette_id != settings.selected_palette_id || palette_smoothness != settings.palette_smoothness) {
                    settings.selected_palette_id = selected_palette_id;
                    settings.palette_smoothness = palette_smoothness;
                    setResult(RESULT_CHANGE);
                } else {
                    setResult(RESULT_NO_CHANGE);
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
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_from_left_wifth_fade);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_palette_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private class DialogReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }

            if (intent.getAction().equals(SmoothnessDialog.RESULT_ID)) {
                switch(intent.getStringExtra(SmoothnessDialog.RESULT_ACTION)) {
                    case SmoothnessDialog.ACTION_POSITIVE:
                        palette_smoothness = intent.getIntExtra(SmoothnessDialog.RESULT_PAYLOAD, palette_smoothness);
                        break;
                }
            }
        }
    }

    /* called whenever an entry has been added - used to fix flaw in Android where animations
    * are no always run (see call in PaletteAdapter()) */
    public void adapter_getView_callback() {
        palette_entries.invalidateViews();
    }
}
