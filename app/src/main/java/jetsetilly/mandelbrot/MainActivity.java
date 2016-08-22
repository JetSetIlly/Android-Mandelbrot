package jetsetilly.mandelbrot;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v8.renderscript.RenderScript;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

import jetsetilly.mandelbrot.RenderCanvas.RenderCanvas;
import jetsetilly.mandelbrot.Settings.GestureSettings;
import jetsetilly.mandelbrot.Settings.PaletteSettings;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;
import jetsetilly.mandelbrot.Settings.SystemSettings;
import jetsetilly.tools.SimpleAsyncTask;


public class MainActivity extends AppCompatActivity {
    private final String DBG_TAG = "main activity";

    // IDs for other activities. used in calls to startActivityForResult()
    // and onActivityResult() implementation
    private static final int PALETTE_ACTIVITY_ID = 1;
    private static final int SETTINGS_ACTIVITY_ID = 2;

    private static final int PERMISSIONS_SAVE_IMAGE = 1;

    // allow other classes to access resources (principally PaletteDefinition)
    // not sure if there is a more elegant way to do this - this seems heavy handed
    static public Resources resources;
    
    // reference to render canvas
    static private RenderCanvas render_canvas;

    // declaring these as static so that it is globally accessible
    // if this seems strange then take a look at this (straight from the horses mouth):
    //
    // https://groups.google.com/d/msg/android-developers/I1swY6FlbPI/gGkY8mt8_IQJ
    static public ProgressView progress;
    static public MandelbrotActionBar action_bar;

    static public RenderScript render_script;

    // handler for dialogs
    private DialogReceiver dialog_receiver;

    // reference to this object so that we can reference it inside the dialog receiver
    private AppCompatActivity this_activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // enforce thread policy
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());

        super.onCreate(savedInstanceState);

        // maintain un-shadow-able reference to this
        this_activity = this;

        // resources
        resources = getResources();

        setContentView(R.layout.activity_main);

        // set up actionbar
        action_bar = (MandelbrotActionBar) findViewById(R.id.toolbar);
        action_bar.completeSetup(this, getResources().getString(R.string.app_name));
        setSupportActionBar(action_bar);

        // restore settings
        final Context context = this;
        new SimpleAsyncTask(new Runnable() {
            @Override
            public void run() {
                MandelbrotSettings.getInstance().restore(context);
                PaletteSettings.getInstance().restore(context);
                GestureSettings.getInstance().restore(context);
                SystemSettings.getInstance().restore(context);
            }
        });

        // render script instance -- alive for the entire lifespan of the app
        render_script = RenderScript.create(context, RenderScript.ContextType.NORMAL);

        // progress view
        progress = (ProgressView) findViewById(R.id.progressView);

        // create new DialogReceiver
        dialog_receiver = new DialogReceiver();

        // set render running as soon as possible
        render_canvas = (RenderCanvas) findViewById(R.id.fractalView);
        render_canvas.initialise(this);

        // apply any relevant settings
        applyOrientation();
    }

    @Override
    protected void onPause() {
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
                overridePendingTransition(R.anim.slide_from_right, R.anim.slide_from_right_with_fade);
                return true;

            case R.id.action_settings:
                IterationsDialog iterations_dialog = new IterationsDialog();
                iterations_dialog.show(getFragmentManager(), null);
                return true;

            case R.id.action_save:
                trySaveImage();
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
                final View info_pane = findViewById(R.id.infoPane);
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

                    PaletteSettings palette_settings = PaletteSettings.getInstance();

                    int num_steps = intent.getIntExtra(PaletteActivity.ACTIVITY_RESULT_PALETTE_SMOOTHNESS, palette_settings.smoothness);
                    int palette_id = intent.getIntExtra(PaletteActivity.ACTIVITY_RESULT_PALETTE_ID, palette_settings.selected_id);

                    // stop/start render if any palette setting has changed
                    if (palette_id != palette_settings.selected_id || num_steps != palette_settings.smoothness ) {
                        render_canvas.stopRender();

                        palette_settings.smoothness = num_steps;
                        palette_settings.setColours(palette_id);
                        palette_settings.save(this);

                        // wait for transition from palette activity to this activity to complete
                        // we do this simply by waiting an equivalent amount of time as the transition
                        Handler h = new Handler();
                        h.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                render_canvas.reRender();
                            }
                        }, getResources().getInteger(R.integer.activity_transition_duration));
                    }
                }
                break;

            case SETTINGS_ACTIVITY_ID:
                if (result_code == SettingsActivity.ACTIVITY_RESULT_RENDER || result_code == SettingsActivity.ACTIVITY_RESULT_NO_RENDER) {
                    // note that settings have been changed in the settings activity
                    // save settings and restart render
                    MandelbrotSettings.getInstance().save(this);
                    GestureSettings.getInstance().save(this);
                    SystemSettings.getInstance().save(this);
                    applyOrientation();
                }

                if (result_code == SettingsActivity.ACTIVITY_RESULTS_REINITIALISE) {
                    render_canvas.initialise(this);
                    render_canvas.startRender();
                }

                if (result_code == SettingsActivity.ACTIVITY_RESULT_RENDER) {
                    render_canvas.startRender();
                }

                break;
        }
    }

    private void applyOrientation() {
        if (SystemSettings.getInstance().allow_screen_rotation) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
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
                        this_activity.overridePendingTransition(R.anim.slide_from_left, R.anim.slide_from_left_wifth_fade);
                        break;
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_SAVE_IMAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    saveImage();
                } else {
                    Toast.makeText(this, R.string.action_save_no_permission, Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }

    private void trySaveImage() {
        int permission_check = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission_check != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_SAVE_IMAGE);
        } else {
            saveImage();
        }
    }

    private void saveImage() {
        long curr_time = System.currentTimeMillis();

        String title = String.format("%s_%s.jpeg", getString(R.string.app_name), new SimpleDateFormat("yyyymmdd_hhmmss", Locale.ENGLISH).format(curr_time));

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, title);
        values.put(MediaStore.Images.Media.DESCRIPTION, getString(R.string.app_name));
        values.put(MediaStore.Images.Media.DATE_ADDED, curr_time);
        values.put(MediaStore.Images.Media.DATE_TAKEN, curr_time);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        ContentResolver cr = this.getContentResolver();
        Uri url = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        // TODO: album in pictures folder

        try {
            url = cr.insert(url, values);
            assert url != null;
            OutputStream output_stream = cr.openOutputStream(url);
            render_canvas.getVisibleImage(false).compress(Bitmap.CompressFormat.JPEG, 100, output_stream);
        } catch (Exception e) {
            Toast.makeText(this, R.string.action_save_fail, Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, R.string.action_save_success, Toast.LENGTH_SHORT).show();
    }
}
