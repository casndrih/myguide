package com.casper.myguide.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.Nullable;

import com.casper.myguide.databinding.FragmentCategoryBinding;
import com.google.android.material.snackbar.Snackbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;

import com.casper.myguide.ActivityMain;
import com.casper.myguide.ActivityPlaceDetail;
import com.casper.myguide.R;
import com.casper.myguide.adapter.AdapterPlaceGrid;
import com.casper.myguide.connection.RestAdapter;
import com.casper.myguide.connection.callbacks.CallbackListPlace;
import com.casper.myguide.data.AppConfig;
import com.casper.myguide.data.Constant;
import com.casper.myguide.data.DatabaseHandler;
import com.casper.myguide.data.SharedPref;
import com.casper.myguide.data.ThisApplication;
import com.casper.myguide.model.Place;
import com.casper.myguide.utils.Tools;
import com.casper.myguide.widget.SpacingItemDecoration;
import retrofit2.Call;
import retrofit2.Response;

public class FragmentCategory extends Fragment {

    public static String TAG_CATEGORY = "key.TAG_CATEGORY";
    private int count_total = 0;
    private int category_id;
    private Snackbar snackbar_retry;
    private FragmentCategoryBinding binding;
    private DatabaseHandler db;
    private SharedPref sharedPref;
    private AdapterPlaceGrid adapter;
    private Call<CallbackListPlace> callback;

    public FragmentCategory() {
    }

    public static FragmentCategory newInstance() {
        FragmentCategory fragment = new FragmentCategory();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCategoryBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        db = new DatabaseHandler(getActivity());
        sharedPref = new SharedPref(getActivity());
        category_id = getArguments().getInt(TAG_CATEGORY);

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

       binding.recycler.setLayoutManager(new StaggeredGridLayoutManager(Tools.getGridSpanCount(getActivity()), StaggeredGridLayoutManager.VERTICAL));
        binding.recycler.addItemDecoration(new SpacingItemDecoration(Tools.getGridSpanCount(getActivity()), Tools.dpToPx(getActivity(), 4), true));

        //set data and list adapter
        adapter = new AdapterPlaceGrid(getActivity(), binding.recycler, new ArrayList<Place>());
        binding.recycler.setAdapter(adapter);

        // on item list clicked
        adapter.setOnItemClickListener(new AdapterPlaceGrid.OnItemClickListener() {
            @Override
            public void onItemClick(View v, Place obj) {
                ActivityPlaceDetail.navigate((ActivityMain) getActivity(), v.findViewById(R.id.lyt_content), obj);
            }
        });

        if (sharedPref.isRefreshPlaces() || db.getPlacesSize() == 0) {
            actionRefresh(sharedPref.getLastPlacePage());
        } else {
            startLoadMoreAdapter();
        }
        return view;
    }

    @Override
    public void onDestroyView() {
        if (snackbar_retry != null) snackbar_retry.dismiss();
        if (callback != null && callback.isExecuted()) {
            callback.cancel();
        }
        super.onDestroyView();
    }

    @Override
    public void onResume() {
        adapter.notifyDataSetChanged();
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void startLoadMoreAdapter() {
        adapter.resetListData();
        List<Place> items = db.getPlacesByPage(category_id, Constant.LIMIT_LOADMORE, 0);
        adapter.insertData(items);
        showNoItemView();
        final int item_count = (int) db.getPlacesSize(category_id);
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
                List<Place> items = db.getPlacesByPage(category_id, Constant.LIMIT_LOADMORE, (next_page * Constant.LIMIT_LOADMORE));
                adapter.insertData(items);
                showNoItemView();
            }
        }, 500);
    }

    // checking some condition before perform refresh data
    private void actionRefresh(int page_no) {
        boolean conn = Tools.cekConnection(getActivity());
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
                    boolean conn = Tools.cekConnection(getActivity());
                    if (conn) {
                        onFailureRetry(page_no, getString(R.string.refresh_failed));
                    } else {
                        onFailureRetry(page_no, getString(R.string.no_internet));
                    }
                }
            }
        });
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
