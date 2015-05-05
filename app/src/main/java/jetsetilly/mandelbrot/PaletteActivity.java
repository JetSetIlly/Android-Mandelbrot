package jetsetilly.mandelbrot;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;


public class PaletteActivity extends Activity {
    private final String DBG_TAG = "palette activity";

    private PaletteAdapter palette_adapter;
    private PaletteDefinitions palette_settings = PaletteDefinitions.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_palette);

        palette_adapter = new PaletteAdapter(this);

        // add palette adapter to this list view
        ListView lv;
        lv = (ListView) findViewById(R.id.palette_listview);
        lv.setAdapter(palette_adapter);

        // react to selections in list view
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                palette_settings.setPalette(position);
                MainActivity.render_canvas.startRender(RenderCanvas.RenderMode.CHANGE_PALETTE);
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
