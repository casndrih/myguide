package com.casper.myguide;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;

import com.casper.myguide.databinding.ActivityNewsInfoBinding;
import com.casper.myguide.databinding.ActivityWeatherBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

import com.casper.myguide.adapter.AdapterNewsInfo;
import com.casper.myguide.connection.API;
import com.casper.myguide.connection.RestAdapter;
import com.casper.myguide.connection.callbacks.CallbackListNewsInfo;
import com.casper.myguide.data.Constant;
import com.casper.myguide.data.DatabaseHandler;
import com.casper.myguide.model.NewsInfo;
import com.casper.myguide.utils.Tools;
import com.casper.myguide.widget.SpacingItemDecoration;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityNewsInfo extends AppCompatActivity {

    public ActionBar actionBar;

    private View parent_view;
    private AdapterNewsInfo mAdapter;
    private Call<CallbackListNewsInfo> callbackCall = null;
    private DatabaseHandler db;
    private ActivityNewsInfoBinding binding;
    private int post_total = 0;
    private int failed_page = 0;
    private Snackbar snackbar_retry = null;
    // can be, ONLINE or OFFLINE
    private String MODE = "ONLINE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewsInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.ntoolbar.toolbar);

        parent_view = findViewById(android.R.id.content);
        db = new DatabaseHandler(this);

        initToolbar();
        iniComponent();

        // for system bar in lollipop
        Tools.setSystemBarColor(this);
        Tools.setSystemBarLight(this);

        // Swipe to refresh
        binding.swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_light, R.color.purple_500, R.color.colorAccent);
        binding.swipeRefreshLayout.setProgressBackgroundColorSchemeColor(Color.WHITE);
        binding.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (callbackCall != null && callbackCall.isExecuted()) callbackCall.cancel();
                showFailedView(false, "");
                MODE = "ONLINE";
                post_total = 0;
                requestAction(1);
            }
        });
    }

    private void initToolbar() {
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle("News");
        binding.ntoolbar.toolbar.getNavigationIcon().setColorFilter(getResources().getColor(R.color.grey_80), PorterDuff.Mode.SRC_ATOP);
        binding.ntoolbar.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
               finish();
            }
        });
    }

    public void iniComponent() {
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.addItemDecoration(new SpacingItemDecoration(1, Tools.dpToPx(this, 4), true));


        //set data and list adapter
        mAdapter = new AdapterNewsInfo(this, binding.recyclerView, new ArrayList<NewsInfo>());
        binding.recyclerView.setAdapter(mAdapter);

        // on item list clicked
        mAdapter.setOnItemClickListener(new AdapterNewsInfo.OnItemClickListener() {
            @Override
            public void onItemClick(View v, NewsInfo obj, int position) {
                ActivityNewsInfoDetails.navigate(ActivityNewsInfo.this, obj, false);
            }
        });

        // detect when scroll reach bottom
        mAdapter.setOnLoadMoreListener(new AdapterNewsInfo.OnLoadMoreListener() {
            @Override
            public void onLoadMore(int current_page) {
                if (post_total > mAdapter.getItemCount() && current_page != 0) {
                    int next_page = current_page + 1;
                    requestAction(next_page);
                } else {
                    mAdapter.setLoaded();
                }
            }
        });

        // if already have data news at db, use mode OFFLINE
        if (db.getNewsInfoSize() > 0) {
            MODE = "OFFLINE";
        }
        requestAction(1);
    }

    private void displayApiResult(final List<NewsInfo> items) {
        mAdapter.insertData(items);
        firstProgress(false);
        if (items.size() == 0) {
            showNoItemView(true);
        }
    }

    private void requestListNewsInfo(final int page_no) {
        if (MODE.equals("ONLINE")) {
            API api = RestAdapter.createAPI();
            callbackCall = api.getNewsInfoByPage(page_no, Constant.LIMIT_NEWS_REQUEST);
            callbackCall.enqueue(new Callback<CallbackListNewsInfo>() {
                @Override
                public void onResponse(Call<CallbackListNewsInfo> call, Response<CallbackListNewsInfo> response) {
                    CallbackListNewsInfo resp = response.body();
                    if (resp != null && resp.status.equals("success")) {
                        if (page_no == 1) {
                            mAdapter.resetListData();
                            db.refreshTableNewsInfo();
                        }
                        post_total = resp.count_total;
                        db.insertListNewsInfo(resp.news_infos);
                        displayApiResult(resp.news_infos);
                    } else {
                        onFailRequest(page_no);
                    }
                }

                @Override
                public void onFailure(Call<CallbackListNewsInfo> call, Throwable t) {
                    if (!call.isCanceled()) onFailRequest(page_no);
                }

            });
        } else {
            if (page_no == 1) mAdapter.resetListData();
            int limit = Constant.LIMIT_NEWS_REQUEST;
            int offset = (page_no * limit) - limit;
            post_total = db.getNewsInfoSize();
            List<NewsInfo> items = db.getNewsInfoByPage(limit, offset);
            displayApiResult(items);
        }
    }

    private void onFailRequest(int page_no) {
        failed_page = page_no;
        mAdapter.setLoaded();
        firstProgress(false);
        if (Tools.cekConnection(this)) {
            showFailedView(true, getString(R.string.refresh_failed));
        } else {
            showFailedView(true, getString(R.string.no_internet));
        }
    }

    private void requestAction(final int page_no) {
        showFailedView(false, "");
        showNoItemView(false);
        if (page_no == 1) {
            firstProgress(true);
        } else {
            mAdapter.setLoading();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                requestListNewsInfo(page_no);
            }
        }, MODE.equals("OFFLINE") ? 50 : 1000);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        firstProgress(false);
        if (callbackCall != null && callbackCall.isExecuted()) {
            callbackCall.cancel();
        }
    }

    private void showFailedView(boolean show, String message) {
        if(snackbar_retry == null) {
            snackbar_retry = Snackbar.make(parent_view, "", Snackbar.LENGTH_INDEFINITE);
        }
        snackbar_retry.setText(message);
        snackbar_retry.setAction(R.string.RETRY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestAction(failed_page);
            }
        });
        if (show) {
            snackbar_retry.show();
        } else {
            snackbar_retry.dismiss();
        }
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = (View) findViewById(R.id.lyt_no_item);
        if (show) {
            binding.recyclerView.setVisibility(View.GONE);
            lyt_no_item.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerView.setVisibility(View.VISIBLE);
            lyt_no_item.setVisibility(View.GONE);
        }
    }

    private void firstProgress(boolean show) {
        if (show) {
            binding.swipeRefreshLayout.setRefreshing(true);
        } else {
            binding.swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
