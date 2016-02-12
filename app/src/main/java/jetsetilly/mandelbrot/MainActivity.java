package jetsetilly.mandelbrot;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

import jetsetilly.mandelbrot.Settings.GestureSettings;
import jetsetilly.mandelbrot.Settings.PaletteSettings;
import jetsetilly.mandelbrot.RenderCanvas.RenderCanvas;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        resources = getResources();
        setContentView(R.layout.activity_main);

        // lock orientation to portrait mode
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // set up actionbar
        action_bar = (MandelbrotActionBar) findViewById(R.id.toolbar);
        action_bar.completeSetup(this, getResources().getString(R.string.app_name));
        setSupportActionBar(action_bar);

        // restore settings
        MandelbrotSettings.getInstance().restore(this);
        PaletteSettings.getInstance().restore(this);
        GestureSettings.getInstance().restore(this);

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
    }

    @Override
    protected void onPause() {
        // TODO: universal setting to allow background rendering
        super.onPause();
    }

    protected void onResume() {
        action_bar.show_noanim();
        super.onResume();
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
                if (saveImage()) {
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
        if (intent == null)
            return;

        switch(request_code) {
            case PALETTE_ACTIVITY_ID:
                if (result_code == PaletteActivity.ACTIVITY_RESULT_CHANGE) {
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
                break;
        }
    }

    public boolean saveImage() {
        long curr_time = System.currentTimeMillis();

        String title = String.format("%s_%s.jpeg", this.getString(R.string.app_name), new SimpleDateFormat("yyyymmdd_hhmmss", Locale.ENGLISH).format(curr_time));

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, title);
        values.put(MediaStore.Images.Media.DESCRIPTION, this.getString(R.string.app_name));
        values.put(MediaStore.Images.Media.DATE_ADDED, curr_time);
        values.put(MediaStore.Images.Media.DATE_TAKEN, curr_time);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        ContentResolver cr = this.getContentResolver();
        Uri url = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        // TODO: album in pictures folder

        try {
            url = cr.insert(url, values);
            OutputStream output_stream = cr.openOutputStream(url);
            render_canvas.display_bm.compress(Bitmap.CompressFormat.JPEG, 100, output_stream);
        } catch (Exception e) {
            if (url != null) {
                cr.delete(url, null, null);
            }

            return false;
        }

        return true;
    }
}
