package jetsetilly.mandelbrot;

import android.app.Activity;
import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;


public class MainActivity extends Activity {
    private final String DBG_TAG = "main activity";

    private View decoration; // window manager (for want of a better phrase)
    private ActionBar action_bar;

    // declaring this as static so that it is globally accessible
    // if this seems strange then take a look at this:
    //
    // https://groups.google.com/d/msg/android-developers/I1swY6FlbPI/gGkY8mt8_IQJ
    //
    // straight from the horses mouth
    static public RenderCanvas render_canvas;
    static public ProgressView progress;

    private SettingsMandelbrot mandelbrot_settings = SettingsMandelbrot.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // action bar control
        action_bar = getActionBar();
        decoration = getWindow().getDecorView();
        hideActionBar(false);

        // lock orientation to portrait mode
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // progress view
        progress = (ProgressView) findViewById(R.id.progressView);

        // set render running as soon as possible
        render_canvas = (RenderCanvas) findViewById(R.id.fractalView);
        render_canvas.post(new Runnable() {
            public void run() {
                render_canvas.kickStartCanvas();
            }
        });
    }

    @Override
    protected void onPause() {
        // TODO: universal setting to allow background rendering

        super.onPause();
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
                return true;

            case R.id.action_settings:
                Intent settings_intent = new Intent(this, SettingsActivity.class);
                startActivity(settings_intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /* action bar control */
    public boolean inActionBar(float y_coordinate) {
        return y_coordinate <= action_bar.getHeight();
    }

    public void hideActionBar(boolean hide)
    {
        /* this synchronises the show/hide animation of the action bar and the status bar */

        if (hide) {
            decoration.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_FULLSCREEN);
            action_bar.hide();
        } else {
            decoration.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            action_bar.show();
        }
    }
    /* end of action bar control */
}
