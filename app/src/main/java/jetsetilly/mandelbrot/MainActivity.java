package jetsetilly.mandelbrot;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
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

import jetsetilly.mandelbrot.Settings.MandelbrotCoordinates;
import jetsetilly.mandelbrot.RenderCanvas.RenderCanvas;
import jetsetilly.mandelbrot.Settings.Settings;
import jetsetilly.tools.SimpleAsyncTask;


public class MainActivity extends AppCompatActivity {
    private final String DBG_TAG = "main activity";

    // used to identify activities in onActivityResult(). set when calling startActivityForResult()
    private static final int ACTIVITY_ID_PALETTE = 1;
    private static final int ACTIVITY_ID_SETTINGS = 2;

    // used to segregate calls to onRequestPermissionsResult(). set by requestPermissions() when
    // doing something that requires user approval
    private static final int REQUEST_PERMISSIONS_SAVE_IMAGE = 1;

    private RenderCanvas render_canvas;
    private DialogReceiver dialog_receiver;

    /*** static declarations ***/
    // declaring these as static so that they are globally accessible
    // if this seems strange then take a look at this (straight from the horses mouth):
    // https://groups.google.com/d/msg/android-developers/I1swY6FlbPI/gGkY8mt8_IQJ
    static public ProgressView progress;
    static public MandelbrotActionBar action_bar;
    static public RenderScript render_script;
    /*** END OF static declarations ***/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // restore settings
        MandelbrotCoordinates.getInstance().restore(this, false);
        Settings.getInstance().restore(this, false);

        // enforce thread policy
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());

        // basic layout
        setContentView(R.layout.activity_main);

        // set up actionbar
        action_bar = (MandelbrotActionBar) findViewById(R.id.toolbar);
        action_bar.completeSetup(this, getResources().getString(R.string.app_name));
        setSupportActionBar(action_bar);

        // render script instance -- alive for the entire lifespan of the app
        {
            final Context context = this;
            new SimpleAsyncTask("Load Renderscript", new Runnable() {
                @Override
                public void run() {
                    render_script = RenderScript.create(context, RenderScript.ContextType.NORMAL);
                }
            });
        }

        // progress view
        progress = (ProgressView) findViewById(R.id.progressView);

        // create new DialogReceiver
        dialog_receiver = new DialogReceiver(this);

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
        MandelbrotCoordinates.getInstance().save(this);
        Settings.getInstance().save(this);
    }

    protected void onResume() {
        action_bar.enforceVisibility();
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(dialog_receiver, new IntentFilter(IterationsDialog.RESULT_ID));
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
                startActivityForResult(palette_intent, ACTIVITY_ID_PALETTE);
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
                MandelbrotCoordinates.getInstance().restoreDefaults(this);
                render_canvas.resetCanvas();
                return true;

            case R.id.action_redraw:
                render_canvas.resetCanvas();
                return true;

            case R.id.action_toggle_info_pane:
                final View info_pane = findViewById(R.id.infoPane);
                if (info_pane.getVisibility() == View.INVISIBLE) {
                    info_pane.animate().setDuration(getResources().getInteger(R.integer.info_pane_fade)).alpha(1f)
                            .withStartAction(new Runnable() {
                                @Override
                                public void run() {
                                    info_pane.setVisibility(View.VISIBLE);
                                }
                            }
                    ).start();
                } else {
                    info_pane.animate().setDuration(getResources().getInteger(R.integer.info_pane_fade)).alpha(0f)
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
            case ACTIVITY_ID_PALETTE:
                if (result_code == PaletteActivity.RESULT_CHANGE) {
                    Settings.getInstance().save(this);
                    render_canvas.stopRender();

                    // wait for transition from palette activity to this activity to complete
                    // we do this simply by waiting an equivalent amount of time as the transition
                    Handler h = new Handler();
                    h.postDelayed(new Runnable() {
                              @Override
                              public void run() {
                            render_canvas.startRender();
                    }
                    }, getResources().getInteger(R.integer.activity_transition_duration));
                }
                break;

            case ACTIVITY_ID_SETTINGS:
                // changes to Settings occurred in SettingsActivity
                // -- now we save and apply those settings

                Settings.getInstance().save(this);
                applyOrientation();

                if (result_code == SettingsActivity.RESULT_REINITIALISE) {
                    render_canvas.initialise(this);
                    render_canvas.startRender();
                } else if (result_code == SettingsActivity.RESULT_RENDER) {
                    render_canvas.startRender();
                }

                break;
        }
    }

    private void applyOrientation() {
        if (Settings.getInstance().allow_screen_rotation) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private class DialogReceiver extends BroadcastReceiver {
        private MainActivity main_activity;

        public DialogReceiver(MainActivity main_activity) {
            this.main_activity = main_activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }

            if (intent.getAction().equals(IterationsDialog.RESULT_ID)) {
                MandelbrotCoordinates mandelbrot = MandelbrotCoordinates.getInstance().getInstance();
                switch(intent.getStringExtra(IterationsDialog.RESULT_ACTION)) {
                    case IterationsDialog.ACTION_POSITIVE:
                        int max_iterations = intent.getIntExtra(IterationsDialog.RESULT_PAYLOAD, mandelbrot.max_iterations);
                        if (max_iterations != mandelbrot.max_iterations) {
                            mandelbrot.max_iterations = max_iterations;
                            render_canvas.startRender();
                        }
                        break;

                    case IterationsDialog.ACTION_NEUTRAL:
                        Intent settings_intent = new Intent(main_activity, SettingsActivity.class);
                        settings_intent.putExtra(SettingsActivity.SETUP_INITIAL_ITERATIONS_VAL, intent.getIntExtra(IterationsDialog.RESULT_PAYLOAD, mandelbrot.max_iterations));
                        startActivityForResult(settings_intent, ACTIVITY_ID_SETTINGS);
                        main_activity.overridePendingTransition(R.anim.slide_from_left, R.anim.slide_from_left_wifth_fade);
                        break;
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSIONS_SAVE_IMAGE:
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
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS_SAVE_IMAGE);
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
