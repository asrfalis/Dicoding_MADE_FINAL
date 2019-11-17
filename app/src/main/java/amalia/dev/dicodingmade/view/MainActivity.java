package amalia.dev.dicodingmade.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.material.tabs.TabLayout;

import amalia.dev.dicodingmade.R;
import amalia.dev.dicodingmade.adapter.SectionPagerAdapter;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SectionPagerAdapter sectionPagerAdapter = new SectionPagerAdapter(this,getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.viewpager_main);
        viewPager.setAdapter(sectionPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tablayout_main);
        tabLayout.setupWithViewPager(viewPager);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setElevation(0);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_menu_setting){
            Intent mIntent = new Intent(Settings.ACTION_LOCALE_SETTINGS);
            startActivity(mIntent);
        }
        return super.onOptionsItemSelected(item);
    }
}