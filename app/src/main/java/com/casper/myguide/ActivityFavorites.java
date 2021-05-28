package com.casper.myguide;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.casper.myguide.adapter.AdapterPlaceGrid;
import com.casper.myguide.connection.RestAdapter;
import com.casper.myguide.connection.callbacks.CallbackListPlace;
import com.casper.myguide.data.AppConfig;
import com.casper.myguide.data.Constant;
import com.casper.myguide.data.DatabaseHandler;
import com.casper.myguide.data.SharedPref;
import com.casper.myguide.data.ThisApplication;
import com.casper.myguide.databinding.ActivityFavoritesBinding;
import com.casper.myguide.model.Place;
import com.casper.myguide.utils.Tools;
import com.casper.myguide.widget.SpacingItemDecoration;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class ActivityFavorites extends AppCompatActivity {

    private ActivityFavoritesBinding binding;
    private int favs;
    private DatabaseHandler db;
    private SharedPref sharedPref;
    private ActionBar actionBar;
    private AdapterPlaceGrid adapter;
    private Snackbar snackbar_retry;
    private int count_total = 0;
    private Call<CallbackListPlace> callback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFavoritesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.ftoolbar.toolbar);

        initToolbar();

        db = new DatabaseHandler(this);
        sharedPref = new SharedPref(this);

        // Get intent from ActivityMain
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            favs = extras.getInt("fav");

            // Swipe to refresh
            binding.swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_light, R.color.purple_500, R.color.colorAccent);
            binding.swipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.WHITE);
            binding.swipeRefreshLayout.setEnabled(true);
            binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    ThisApplication.getInstance().setLocation(null);
                    sharedPref.setLastPlacePage(1);
                    sharedPref.setRefreshPlaces(true);
                    if (snackbar_retry != null) snackbar_retry.dismiss();
                    actionRefresh(sharedPref.getLastPlacePage());
                }
            });

            // for system bar in lollipop
            Tools.setSystemBarColor(this);
            Tools.setSystemBarLight(this);

        }

        binding.recycler.setLayoutManager(new StaggeredGridLayoutManager(Tools.getGridSpanCount(ActivityFavorites.this), StaggeredGridLayoutManager.VERTICAL));
        binding.recycler.addItemDecoration(new SpacingItemDecoration(Tools.getGridSpanCount(ActivityFavorites.this), Tools.dpToPx(ActivityFavorites.this, 4), true));

        //set data and list adapter
        adapter = new AdapterPlaceGrid(ActivityFavorites.this, binding.recycler, new ArrayList<Place>());
        binding.recycler.setAdapter(adapter);

        // on item list clicked
        adapter.setOnItemClickListener(new AdapterPlaceGrid.OnItemClickListener() {
            @Override
            public void onItemClick(View v, Place obj) {
                ActivityPlaceDetail.navigate((ActivityFavorites) ActivityFavorites.this, v.findViewById(R.id.lyt_content), obj);
            }
        });

        if (sharedPref.isRefreshPlaces() || db.getPlacesSize() == 0) {
            actionRefresh(sharedPref.getLastPlacePage());
        } else {
            startLoadMoreAdapter();
        }

    }

    private void initToolbar() {
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle("Favorites");
        binding.ftoolbar.toolbar.getNavigationIcon().setColorFilter(getResources().getColor(R.color.grey_80), PorterDuff.Mode.SRC_ATOP);
        binding.ftoolbar.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                finish();
            }
        });
    }

    private void startLoadMoreAdapter() {
        adapter.resetListData();
        List<Place> items = db.getPlacesByPage(favs, Constant.LIMIT_LOADMORE, 0);
        adapter.insertData(items);
        showNoItemView();
        final int item_count = (int) db.getPlacesSize(favs);
        // detect when scroll reach bottom
        adapter.setOnLoadMoreListener(new AdapterPlaceGrid.OnLoadMoreListener() {
            @Override
            public void onLoadMore(final int current_page) {
                if (item_count > adapter.getItemCount() && current_page != 0) {
                    displayDataByPage(current_page);
                } else {
                    adapter.setLoaded();
                }
            }
        });
    }

    private void displayDataByPage(final int next_page) {
        adapter.setLoading();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                List<Place> items = db.getPlacesByPage(favs, Constant.LIMIT_LOADMORE, (next_page * Constant.LIMIT_LOADMORE));
                adapter.insertData(items);
                showNoItemView();
            }
        }, 500);
    }

    // checking some condition before perform refresh data
    private void actionRefresh(int page_no) {
        boolean conn = Tools.cekConnection(ActivityFavorites.this);
        if (conn) {
            if (!onProcess) {
                onRefresh(page_no);
            } else {
                Snackbar.make(binding.getRoot(), R.string.task_running, Snackbar.LENGTH_SHORT).show();
            }
        } else {
            onFailureRetry(page_no, getString(R.string.no_internet));
        }
    }

    private boolean onProcess = false;

    private void onRefresh(final int page_no) {
        onProcess = true;
        showProgress(onProcess);
        callback = RestAdapter.createAPI().getPlacesByPage(page_no, Constant.LIMIT_PLACE_REQUEST, (AppConfig.LAZY_LOAD ? 1 : 0));
        callback.enqueue(new retrofit2.Callback<CallbackListPlace>() {
            @Override
            public void onResponse(Call<CallbackListPlace> call, Response<CallbackListPlace> response) {
                CallbackListPlace resp = response.body();
                if (resp != null) {
                    count_total = resp.count_total;
                    if (page_no == 1) db.refreshTablePlace();
                    db.insertListPlaceAsync(resp.places);  // save result into database
                    sharedPref.setLastPlacePage(page_no + 1);
                    delayNextRequest(page_no);
                } else {
                    onFailureRetry(page_no, getString(R.string.refresh_failed));
                }
            }

            @Override
            public void onFailure(Call<CallbackListPlace> call, Throwable t) {
                if (call != null && !call.isCanceled()) {
                    Log.e("onFailure", t.getMessage());
                    boolean conn = Tools.cekConnection(ActivityFavorites.this);
                    if (conn) {
                        onFailureRetry(page_no, getString(R.string.refresh_failed));
                    } else {
                        onFailureRetry(page_no, getString(R.string.no_internet));
                    }
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        adapter.notifyDataSetChanged();
        super.onResume();
    }

    private void showProgress(boolean show) {
        if (show) {
            binding.swipeRefreshLayout.setRefreshing(true);
            binding.recycler.setVisibility(View.GONE);
            binding.lytNotFound.getRoot().setVisibility(View.GONE);
        } else {
            binding.swipeRefreshLayout.setRefreshing(false);
            binding.recycler.setVisibility(View.VISIBLE);
        }
    }

    private void showNoItemView() {
        if (adapter.getItemCount() == 0) {
            binding.lytNotFound.getRoot().setVisibility(View.VISIBLE);
        } else {
            binding.lytNotFound.getRoot().setVisibility(View.GONE);
        }
    }

    private void onFailureRetry(final int page_no, String msg) {
        onProcess = false;
        showProgress(onProcess);
        showNoItemView();
        startLoadMoreAdapter();
        try {
            snackbar_retry = Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_INDEFINITE);
            snackbar_retry.setAction(R.string.RETRY, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    actionRefresh(page_no);
                }
            });
            snackbar_retry.show();
        } catch (Exception e) {}
    }

    private void delayNextRequest(final int page_no) {
        if (count_total == 0) {
            onFailureRetry(page_no, getString(R.string.refresh_failed));
            return;
        }
        if ((page_no * Constant.LIMIT_PLACE_REQUEST) > count_total) { // when all data loaded
            onProcess = false;
            showProgress(onProcess);
            startLoadMoreAdapter();
            sharedPref.setRefreshPlaces(false);
            Snackbar.make(binding.getRoot(), R.string.load_success, Snackbar.LENGTH_LONG).show();
            return;
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                onRefresh(page_no + 1);
            }
        }, 300);
    }

    }