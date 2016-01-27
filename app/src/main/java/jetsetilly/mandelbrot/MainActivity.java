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
import android.widget.Toast;
import android.widget.ImageView;

import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

import jetsetilly.mandelbrot.Settings.GestureSettings;
import jetsetilly.mandelbrot.Settings.PaletteSettings;
import jetsetilly.mandelbrot.RenderCanvas.RenderCanvas;
import jetsetilly.mandelbrot.Settings.MandelbrotSettings;


public class MainActivity extends AppCompatActivity {
    private final String DBG_TAG = "main activity";

    // allow other classes to access resources (used in PaletteDefinition)
    // not sure if there is a more elegant way to do this - this seems heavy handed
    static public Resources resources;

    // declaring these as static so that it is globally accessible
    // if this seems strange then take a look at this (straight from the horses mouth):
    //
    // https://groups.google.com/d/msg/android-developers/I1swY6FlbPI/gGkY8mt8_IQJ
    static public RenderCanvas render_canvas;
    static public ProgressView progress;

    public MandelbrotActionBar action_bar;
    public ImageView background_view;

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
                render_canvas.startCanvas();
            }
        });

        // get reference to background view
        background_view = (ImageView) findViewById(R.id.background);
    }

    @Override
    protected void onPause() {
        // TODO: universal setting to allow background rendering
        super.onPause();
    }

    protected void onResume() {
        super.onResume();
        action_bar.hide(false);
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
                startActivity(palette_intent);
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
                render_canvas.startCanvas();
                return true;

            case R.id.action_redraw:
                render_canvas.startCanvas();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean saveImage() {
        long curr_time = System.currentTimeMillis();

        String title = String.format("%s_%s.jpeg",
                this.getString(R.string.app_name),
                new SimpleDateFormat("ssmmhhddmmyyyy", Locale.ENGLISH).format(curr_time));

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, title);
        values.put(MediaStore.Images.Media.DESCRIPTION, this.getString(R.string.app_name));
        values.put(MediaStore.Images.Media.DATE_ADDED, curr_time);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        ContentResolver cr = this.getContentResolver();
        Uri url = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        // TODO: album in pictures folder

        try {
            url = cr.insert(url, values);

            OutputStream o = cr.openOutputStream(url);
            render_canvas.getDisplayedBitmap().compress(Bitmap.CompressFormat.JPEG, 100, o);
        } catch (Exception e) {
            if (url != null) {
                cr.delete(url, null, null);
            }

            return false;
        }

        return true;
    }
}
