package jetsetilly.mandelbrot;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;


public class PaletteActivity extends Activity {
    private final String DBG_TAG = "colours activity";

    private ListView lv;

    private MainActivity context;

    private PaletteAdapter palette_adapter;
    private PaletteControl palette_settings = PaletteControl.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palette);

        palette_adapter = new PaletteAdapter(this);

        // add colours adapter to this list view
        lv = (ListView) findViewById(R.id.palette_listview);
        lv.setAdapter(palette_adapter);

        // react to selections in list view
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                palette_adapter.setPaletteCard(view);
                palette_settings.setColours(position);
                MainActivity.render_canvas.startRender();
            }
        });

        lv.post(new Runnable() {
            public void run() {
                // TODO: scroll to selected entry before listview is shown to prevent ugliness
                lv.smoothScrollToPosition(palette_settings.selected_id);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
