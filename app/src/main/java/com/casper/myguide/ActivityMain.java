package com.casper.myguide;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.casper.myguide.data.DatabaseHandler;
import com.casper.myguide.databinding.ActivityFavoritesBinding;
import com.casper.myguide.databinding.ActivityMainBinding;
import com.casper.myguide.fragment.FragmentCategory;
import com.casper.myguide.utils.Tools;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class ActivityMain extends AppCompatActivity {

    public ActionBar actionBar;
    private int cat[];
    private BottomSheetBehavior mBehavior;
    private BottomSheetDialog mBottomSheetDialog;
    private NavigationView navigationView;
    private ActivityMainBinding binding;
    private DatabaseHandler db;
    static ActivityMain activityMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.mainToolbar.toolbar);

        activityMain = this;

        initToolbar();
        initComponents();
        db = new DatabaseHandler(this);
        cat = getResources().getIntArray(R.array.id_category);

        // for system bar in lollipop
        Tools.setSystemBarColor(this);
        Tools.setSystemBarLight(this);
    }

    private void initToolbar() {
        actionBar = getSupportActionBar();
        Tools.changeOverflowMenuIconColor(binding.mainToolbar.toolbar, getResources().getColor(R.color.grey_80));
        //work around for custom nav menu//
        actionBar.setDisplayHomeAsUpEnabled(true);
        binding.mainToolbar.toolbar.setNavigationIcon(R.drawable.ic_notes);
        binding.mainToolbar.toolbar.getNavigationIcon().setColorFilter(getResources().getColor(R.color.grey_80), PorterDuff.Mode.SRC_ATOP);
        binding.mainToolbar.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                showBottomSheetDialog();
            }
        });

        mBehavior = BottomSheetBehavior.from(binding.bottomSheet);

    }

    private void initComponents() {
        setupViewPager(binding.viewPager);
        binding.tabLayout.setupWithViewPager(binding.viewPager);
    }

    private void showBottomSheetDialog() {
        if (mBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
            mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

        final View view = getLayoutInflater().inflate(R.layout.menu_list, null);

        navigationView = (NavigationView) view.findViewById(R.id.navigationView);
        updateFavCounter(navigationView, R.id.nav_fav, db.getFavoritesSize());
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                mBottomSheetDialog.dismiss();
                return onItemSelected(item.getItemId());
            }
        });

        mBottomSheetDialog = new BottomSheetDialog(this);
        mBottomSheetDialog.setContentView(view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBottomSheetDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        mBottomSheetDialog.show();
        mBottomSheetDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mBottomSheetDialog = null;
            }
        });
    }

    private void updateFavCounter(NavigationView nav, @IdRes int itemId, int count) {
        TextView view = (TextView) nav.getMenu().findItem(itemId).getActionView().findViewById(R.id.textCount);
        view.setText(String.valueOf(count));
    }


    public boolean onItemSelected(int id) {
        // Handle navigation view item clicks here.
        Bundle bundle = new Bundle();
        switch (id) {
            //sub menu
            case R.id.nav_fav:
                Intent f = new Intent(this, ActivityFavorites.class);
                bundle.putInt("fav", -2);
                f.putExtras(bundle);
                startActivity(f);
                break;
            // favorites
            case R.id.nav_news:
                Intent i = new Intent(this, ActivityNewsInfo.class);
                startActivity(i);
                break;
            // news info
            case R.id.nav_setting:
                Intent s = new Intent(getApplicationContext(), ActivitySetting.class);
                startActivity(s);
                break;

            case R.id.nav_about:
                Tools.aboutAction(ActivityMain.this);
                break;
            default:
                break;
        }
     return true;
    }

    private void setupViewPager(ViewPager viewPager) {
        SectionsPagerAdapter adapter = new SectionsPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(FragmentCategory.newInstance(), "All Places");
        adapter.addFragment(FragmentCategory.newInstance(), "Featured Places");
        adapter.addFragment(FragmentCategory.newInstance(), "Tourist Destination");
        adapter.addFragment(FragmentCategory.newInstance(), "Food & Drink");
        adapter.addFragment(FragmentCategory.newInstance(), "Hotels");
        adapter.addFragment(FragmentCategory.newInstance(), "Entertainment");
        adapter.addFragment(FragmentCategory.newInstance(), "Sport");
        adapter.addFragment(FragmentCategory.newInstance(), "Shopping");
        adapter.addFragment(FragmentCategory.newInstance(), "Transportation");
        adapter.addFragment(FragmentCategory.newInstance(), "Religion");
        adapter.addFragment(FragmentCategory.newInstance(), "Public Services");
        adapter.addFragment(FragmentCategory.newInstance(), "Money");
        viewPager.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
            doExitApp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_main, menu);
        Tools.changeMenuIconColor(menu, getResources().getColor(R.color.grey_80));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String urlCovid = "https://covid19.info.gov.pg";
        String urlFlight = "https://www.airniugini.com.pg/mobile/book-flights";
        int id = item.getItemId();
        if (id == R.id.action_search) {
            Intent i = new Intent(getApplicationContext(), ActivitySearch.class);
            startActivity(i);
        } else if (id == R.id.action_flight){
            Tools.openInAppBrowser(ActivityMain.this, urlFlight, true);
        } else if (id == R.id.action_alert){
        Tools.openInAppBrowser(ActivityMain.this, urlCovid, true);
    }
        return super.onOptionsItemSelected(item);
    }

    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public SectionsPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            switch(position){
                case 0:
                    FragmentCategory all = new FragmentCategory();
                    bundle.putInt(FragmentCategory.TAG_CATEGORY, -1);
                    all.setArguments(bundle);
                    return all;
                case 1:
                    FragmentCategory featured = new FragmentCategory();
                    bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[10]);
                    featured.setArguments(bundle);
                    return featured;
                case 2:
                    FragmentCategory tour = new FragmentCategory();
                    bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[0]);
                    tour.setArguments(bundle);
                    return tour;
                case 3:
                    FragmentCategory food = new FragmentCategory();
                    bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[1]);
                    food.setArguments(bundle);
                    return food;
                case 4:
                    FragmentCategory hotels = new FragmentCategory();
                    bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[2]);
                    hotels.setArguments(bundle);
                    return hotels;
                case 5:
                    FragmentCategory ent = new FragmentCategory();
                    bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[3]);
                    ent.setArguments(bundle);
                    return ent;
                case 6:
                    FragmentCategory sports = new FragmentCategory();
                    bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[4]);
                    sports.setArguments(bundle);
                    return sports;
                case 7:
                    FragmentCategory shop = new FragmentCategory();
                    bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[5]);
                    shop.setArguments(bundle);
                    return shop;
                case 8:
                    FragmentCategory transport = new FragmentCategory();
                    bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[6]);
                    transport.setArguments(bundle);
                    return transport;
                case 9:
                    FragmentCategory religion = new FragmentCategory();
                    bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[7]);
                    religion.setArguments(bundle);
                    return religion;
                case 10:
                    FragmentCategory publics = new FragmentCategory();
                    bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[8]);
                    publics.setArguments(bundle);
                    return publics;
                case 11:
                    FragmentCategory money = new FragmentCategory();
                    bundle.putInt(FragmentCategory.TAG_CATEGORY, cat[9]);
                    money.setArguments(bundle);
                    return money;
            }
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            cat = getResources().getIntArray(R.array.id_category);

            return mFragmentTitleList.get(position);
        }
    }

    private long exitTime = 0;

    public void doExitApp() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(this, R.string.press_again_exit_app, Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    static boolean active = false;

    @Override
    public void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        active = false;
    }

    public static ActivityMain getInstance() {
        return activityMain;
    }

}
