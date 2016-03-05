package jetsetilly.mandelbrot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.GridView;

import jetsetilly.mandelbrot.Palette.PaletteActivityListAdapter;
import jetsetilly.mandelbrot.Settings.PaletteSettings;

public class PaletteActivity extends AppCompatActivity {
    private final String DBG_TAG = "palette activity";

    public static final Integer ACTIVITY_RESULT_CHANGE = 1;
    public static final String ACTIVITY_RESULT_PALETTE_ID = "PALETTE_ID";
    public static final String ACTIVITY_RESULT_PALETTE_SMOOTHNESS = "PALETTE_SMOOTHNESS";

    private GridView palette_entries;
    private int smoothness;

    private DialogReceiver dialog_receiver;

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

        smoothness = PaletteSettings.getInstance().smoothness;

        // create new DialogReceiver
        dialog_receiver = new DialogReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dialog_receiver);
    }

    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(dialog_receiver, new IntentFilter(SmoothnessDialog.SMOOTHNESS_DIALOG_INTENT));
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
                args.putInt(SmoothnessDialog.SET_VALUE, smoothness);
                smoothness_dialog.setArguments(args);
                smoothness_dialog.show(getFragmentManager(), null);
                break;

            case android.R.id.home:
                Intent activity_result_intent = new Intent(this, MainActivity.class);
                activity_result_intent.putExtra(ACTIVITY_RESULT_PALETTE_SMOOTHNESS, smoothness);
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

            if (intent.getAction().equals(SmoothnessDialog.SMOOTHNESS_DIALOG_INTENT)) {
                switch(intent.getStringExtra(SmoothnessDialog.INTENT_ACTION)) {
                    case SmoothnessDialog.ACTION_SET:
                        smoothness = intent.getIntExtra(SmoothnessDialog.SET_VALUE, smoothness);
                        break;
                }
            }
        }
    }

    /* called whenever an entry has been added - used to fix flaw in Android where animations
    * are no always run (see call in PaletteActivityListAdapter()) */
    public void adapter_getView_callback() {
        palette_entries.invalidateViews();
    }
}
