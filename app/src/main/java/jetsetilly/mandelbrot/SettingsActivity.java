package jetsetilly.mandelbrot;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ListView;


public class SettingsActivity extends Activity {
    private final String DBG_TAG = "settings activity";

    public boolean dirty_settings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SettingsAdapter settings_adapter = new SettingsAdapter(this);

        // add palette adapter to this list view
        ListView lv;
        lv = (ListView) findViewById(R.id.settings_listview);
        lv.setAdapter(settings_adapter);

        dirty_settings = false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case android.R.id.home:
                if (dirty_settings) {
                    MainActivity.render_canvas.startRender();
                }

                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
