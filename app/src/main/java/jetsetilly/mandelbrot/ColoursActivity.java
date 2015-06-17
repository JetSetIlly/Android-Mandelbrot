package jetsetilly.mandelbrot;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import jetsetilly.mandelbrot.Google.SlidingTabLayout;
import jetsetilly.mandelbrot.Palette.Adapter;
import jetsetilly.mandelbrot.Palette.Settings;


public class ColoursActivity extends AppCompatActivity {
    private final String DBG_TAG = "colours activity";

    private Settings palette_settings = Settings.getInstance();

    private SlidingTabLayout tabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_colours);

        ViewPager pager = (ViewPager) findViewById(R.id.colours_pager);
        pager.setAdapter(new PalettePager(this));

        tabs = (SlidingTabLayout) findViewById(R.id.colours_tabs);
        tabs.setViewPager(pager);

        setAccentColor();
    }

    private void setAccentColor() {
        //tabs.setSelectedIndicatorColors(palette_settings.selected_palette.base_color);
        tabs.setSelectedIndicatorColors(palette_settings.selected_palette.colours[palette_settings.DEF_KEY_COL]);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case android.R.id.home:
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
        overridePendingTransition(R.anim.from_left_nofade, R.anim.from_left_fade_out);
    }

    class PalettePager extends PagerAdapter {
        private final String DBG_TAG = "palette pager adapter";

        private final ThreadLocal<ColoursActivity> context = new ThreadLocal<>();

        public PalettePager(ColoursActivity context) {
            this.context.set(context);
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return o == view;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Resources r = getResources();

            switch (position) {
                case 0:
                    return r.getString(R.string.colours_all_palettes);

                case 1:
                    return r.getString(R.string.colours_favourite_palettes);

                case 2:
                    return r.getString(R.string.colours_mixer);
            }

            return "*error*";
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            // Inflate a new layout from our resources
            View view = context.get().getLayoutInflater().inflate(R.layout.activity_colours_page, container, false);
            // Add the newly created View to the ViewPager
            container.addView(view);

            // TODO: implement other pages
            if (position != 0) return view;

            final Adapter palette_adapter = new Adapter(context.get());

            // add colours adapter to this list view
            final ListView lv = (ListView) view.findViewById(R.id.palettes_list);
            lv.setAdapter(palette_adapter);

            // react to selections in list view
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    palette_adapter.setPaletteCard(view);
                    palette_settings.setColours(position);
                    setAccentColor();
                    MainActivity.render_canvas.startRender();
                }
            });

            // scroll list to the correct location
            lv.post(new Runnable() {
                public void run() {
                    lv.setSelection(palette_settings.selected_id);
                }
            });

            return view;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }
}
