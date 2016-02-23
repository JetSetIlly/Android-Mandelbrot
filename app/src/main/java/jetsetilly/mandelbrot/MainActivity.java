package jetsetilly.mandelbrot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import jetsetilly.mandelbrot.Settings.GestureSettings;
import jetsetilly.mandelbrot.Settings.PaletteSettings;
import jetsetilly.mandelbrot.RenderCanvas.RenderCanvas;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;
import jetsetilly.mandelbrot.Settings.SystemSettings;


public class MainActivity extends AppCompatActivity {
    private final String DBG_TAG = "main activity";

    // IDs for other activities. used in calls to startActivityForResult()
    // and onActivityResult() implementation
    private static final int PALETTE_ACTIVITY_ID = 1;
    private static final int SETTINGS_ACTIVITY_ID = 2;

    // allow other classes to access resources (principally PaletteDefinition)
    // not sure if there is a more elegant way to do this - this seems heavy handed
    static public Resources resources;

    // declaring these as static so that it is globally accessible
    // if this seems strange then take a look at this (straight from the horses mouth):
    //
    // https://groups.google.com/d/msg/android-developers/I1swY6FlbPI/gGkY8mt8_IQJ
    static public RenderCanvas render_canvas;
    static public ProgressView progress;

    public MandelbrotActionBar action_bar;

    // handler for dialogs
    private DialogReceiver dialog_receiver;

    private AppCompatActivity this_activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // maintain un-shadow-able reference to this
        this_activity = this;

        // resources
        resources = getResources();

        setContentView(R.layout.activity_main);

        // lock orientation to portrait mode
        //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // set up actionbar
        action_bar = (MandelbrotActionBar) findViewById(R.id.toolbar);
        action_bar.completeSetup(this, getResources().getString(R.string.app_name));
        setSupportActionBar(action_bar);

        // restore settings
        MandelbrotSettings.getInstance().restore(this);
        PaletteSettings.getInstance().restore(this);
        GestureSettings.getInstance().restore(this);
        SystemSettings.getInstance().restore(this);

        // generate swatches for palettes
        PaletteSettings.getInstance().createSwatches(this);

        // progress view
        progress = (ProgressView) findViewById(R.id.progressView);

        // set render running as soon as possible
        render_canvas = (RenderCanvas) findViewById(R.id.fractalView);
        render_canvas.post(new Runnable() {
            public void run() {
                render_canvas.initPostLayout();
            }
        });

        // create new DialogReceiver
        dialog_receiver = new DialogReceiver();

        // apply any relevant settings
        completeSetup();
    }

    @Override
    protected void onPause() {
        // TODO: universal setting to allow background rendering
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dialog_receiver);
    }

    protected void onResume() {
        action_bar.show_noanim();
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(dialog_receiver, new IntentFilter(IterationsDialog.ITERATIONS_DIALOG_INTENT));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_palette:
                Intent palette_intent = new Intent(this, PaletteActivity.class);
                startActivityForResult(palette_intent, PALETTE_ACTIVITY_ID);
                overridePendingTransition(R.anim.from_right_nofade, R.anim.from_right_fade_out);
                return true;

            case R.id.action_settings:
                IterationsDialog iterations_dialog = new IterationsDialog();
                iterations_dialog.show(getFragmentManager(), null);
                return true;

            case R.id.action_save:
                if (render_canvas.saveImage()) {
                    Toast.makeText(this, R.string.action_save_success, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.action_save_fail, Toast.LENGTH_SHORT).show();
                }

                return true;

            case R.id.action_reset:
                render_canvas.stopRender();
                MandelbrotSettings.getInstance().reset();
                render_canvas.resetCanvas();
                return true;

            case R.id.action_redraw:
                render_canvas.resetCanvas();
                return true;

            case R.id.action_toggle_info_pane:
                final View info_pane = findViewById(R.id.info_pane);
                if (info_pane.getVisibility() == View.INVISIBLE) {
                    info_pane.animate().setDuration(resources.getInteger(R.integer.info_pane_fade)).alpha(1f)
                            .withStartAction(new Runnable() {
                                @Override
                                public void run() {
                                    info_pane.setVisibility(View.VISIBLE);
                                }
                            }
                    ).start();
                } else {
                    info_pane.animate().setDuration(resources.getInteger(R.integer.info_pane_fade)).alpha(0f)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    info_pane.setVisibility(View.INVISIBLE);
                                }
                            }
                    ).start();
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int request_code, int result_code, Intent intent) {
        switch(request_code) {
            case PALETTE_ACTIVITY_ID:
                if (result_code == PaletteActivity.ACTIVITY_RESULT_CHANGE) {
                    if (intent == null)
                        return;

                    int palette_id = intent.getIntExtra(PaletteActivity.ACTIVITY_RESULT_PALETTE_ID, -1);
                    if (palette_id >= 0) {
                        render_canvas.stopRender();

                        PaletteSettings palette_settings = PaletteSettings.getInstance();
                        palette_settings.setColours(palette_id);
                        palette_settings.save(this);

                        render_canvas.startRender();
                    }
                }
                break;

            case SETTINGS_ACTIVITY_ID:
                if (result_code == SettingsActivity.ACTIVITY_RESULT_CHANGE) {
                    // note that settings have been changed in the settings activity
                    // save settings and restart render
                    // TODO: return bundled changes in the intent
                    MandelbrotSettings.getInstance().save(this);
                    GestureSettings.getInstance().save(this);
                    SystemSettings.getInstance().save(this);
                    completeSetup();
                    render_canvas.startRender();
                }
                break;
        }
    }

    private void completeSetup() {
        if (SystemSettings.getInstance().allow_screen_rotation) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private class DialogReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }

            if (intent.getAction().equals(IterationsDialog.ITERATIONS_DIALOG_INTENT)) {
                MandelbrotSettings mandelbrot_settings = MandelbrotSettings.getInstance();
                switch(intent.getStringExtra(IterationsDialog.INTENT_ACTION)) {
                    case IterationsDialog.ACTION_SET:
                        int max_iterations = intent.getIntExtra(IterationsDialog.SET_VALUE, mandelbrot_settings.max_iterations);
                        if (max_iterations != mandelbrot_settings.max_iterations) {
                            mandelbrot_settings.max_iterations = max_iterations;
                            MainActivity.render_canvas.startRender();
                        }
                        break;

                    case IterationsDialog.ACTION_MORE:
                        Intent settings_intent = new Intent(this_activity, SettingsActivity.class);
                        settings_intent.putExtra(SettingsActivity.INITIAL_ITERATIONS_VALUE, intent.getIntExtra(IterationsDialog.SET_VALUE, mandelbrot_settings.max_iterations));
                        startActivityForResult(settings_intent, SETTINGS_ACTIVITY_ID);
                        this_activity.overridePendingTransition(R.anim.from_left_nofade, R.anim.from_left_fade_out);
                        break;
                }
            }
        }
    }
}
