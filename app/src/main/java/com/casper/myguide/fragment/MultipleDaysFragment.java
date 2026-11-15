package com.casper.myguide.fragment;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.casper.myguide.R;
import com.casper.myguide.data.ThisApplication;
import com.casper.myguide.databinding.FragmentMultipleDaysBinding;
import com.casper.myguide.weathermodel.CityInfo;
import com.casper.myguide.weathermodel.daysweather.ListItem;
import com.casper.myguide.weathermodel.daysweather.MultipleDaysWeatherResponse;
import com.casper.myguide.weathermodel.db.MultipleDaysWeather;
import com.casper.myguide.service.ApiService;
import com.casper.myguide.weatherutils.ApiClient;
import com.casper.myguide.weatherutils.AppUtil;
import com.casper.myguide.weatherutils.Constants;
import com.casper.myguide.weatherutils.DbUtil;
import com.github.pwittchen.prefser.library.rx2.Prefser;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;

import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.android.AndroidScheduler;
import io.objectbox.query.Query;
import io.objectbox.reactive.DataObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class MultipleDaysFragment extends DialogFragment {

  private String defaultLang = "en";
  private CompositeDisposable disposable = new CompositeDisposable();
  private FastAdapter<MultipleDaysWeather> mFastAdapter;
  private ItemAdapter<MultipleDaysWeather> mItemAdapter;
  private Activity activity;
  private Box<MultipleDaysWeather> multipleDaysWeatherBox;
  private Prefser prefser;
  private String apiKey;
  private String lat,lng;
  private FragmentMultipleDaysBinding binding;

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    binding = FragmentMultipleDaysBinding.inflate(inflater, container, false);

    View view = binding.getRoot();
    initVariables();
    initSwipeView();
    initRecyclerView();
    showStoredMultipleDaysWeather();
    checkTimePass();

    return view;
  }

  private void initVariables() {
    activity = getActivity();
    prefser = new Prefser(activity);
    lat = prefser.get("lat", String.class, "Latitude");
    lng = prefser.get("lng", String.class, "Longitude");

    BoxStore boxStore = ThisApplication.getBoxStore();
    multipleDaysWeatherBox = boxStore.boxFor(MultipleDaysWeather.class);
    binding.closeButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        dismiss();
        if (getFragmentManager() != null) {
          getFragmentManager().popBackStack();
        }
      }
    });
  }

  private void initSwipeView() {
    binding.swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
        android.R.color.holo_green_light,
        android.R.color.holo_orange_light,
        android.R.color.holo_red_light);
    binding.swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

      @Override
      public void onRefresh() {
        requestWeather();
      }

    });
  }

  private void requestWeather() {
    long lastUpdate = prefser.get(Constants.LAST_STORED_MULTIPLE_DAYS, Long.class, 0L);
    if (AppUtil.isTimePass(lastUpdate)) {
      checkCityInfoExist();
    } else {
      binding.swipeContainer.setRefreshing(false);
    }
  }

  private void initRecyclerView() {
    LinearLayoutManager layoutManager
        = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
    binding.recyclerView.setLayoutManager(layoutManager);
    mItemAdapter = new ItemAdapter<>();
    mFastAdapter = FastAdapter.with(mItemAdapter);
    binding.recyclerView.setItemAnimator(new DefaultItemAnimator());
    binding.recyclerView.setAdapter(mFastAdapter);
  }

  private void showStoredMultipleDaysWeather() {
    Query<MultipleDaysWeather> query = DbUtil.getMultipleDaysWeatherQuery(multipleDaysWeatherBox);
    query.subscribe().on(AndroidScheduler.mainThread())
        .observer(new DataObserver<List<MultipleDaysWeather>>() {
          @Override
          public void onData(@NonNull List<MultipleDaysWeather> data) {
            if (data.size() > 0) {
              final Handler handler = new Handler();
              handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                  data.remove(0);
                  mItemAdapter.clear();
                  mItemAdapter.add(data);
                }
              }, 500);
            }
          }
        });
  }

  private void checkTimePass() {
    apiKey = getResources().getString(R.string.open_weather_maps_app_id);
    if (prefser.contains(Constants.LAST_STORED_MULTIPLE_DAYS)) {
      requestWeather();
    } else {
      checkCityInfoExist();
    }
  }

  private void checkCityInfoExist() {
    CityInfo cityInfo = prefser.get(Constants.CITY_INFO, CityInfo.class, null);
    if (cityInfo != null) {
      if (AppUtil.isNetworkConnected()) {
        requestWeathers(lat,lng);
      } else {
        Toast.makeText(activity, getResources().getString(R.string.no_internet_message), Toast.LENGTH_SHORT).show();
        binding.swipeContainer.setRefreshing(false);
      }
    }
  }

  private void requestWeathers(String lat, String lng) {
    ApiService apiService = ApiClient.getClient().create(ApiService.class);
    disposable.add(
        apiService.getMultipleDaysWeather(
                lat,lng, Constants.UNITS, defaultLang, 16, apiKey)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(new DisposableSingleObserver<MultipleDaysWeatherResponse>() {
              @Override
              public void onSuccess(MultipleDaysWeatherResponse response) {
                handleMultipleDaysResponse(response);
                binding.swipeContainer.setRefreshing(false);
              }

              @Override
              public void onError(Throwable e) {
                binding.swipeContainer.setRefreshing(false);
                Log.e("MainActivity", "onError: " + e.getMessage());
              }
            })
    );
  }

  private void handleMultipleDaysResponse(MultipleDaysWeatherResponse response) {
    multipleDaysWeatherBox.removeAll();
    List<ListItem> listItems = response.getList();
    for (ListItem listItem : listItems) {
      MultipleDaysWeather multipleDaysWeather = new MultipleDaysWeather();
      multipleDaysWeather.setDt(listItem.getDt());
      multipleDaysWeather.setMaxTemp(listItem.getTemp().getMax());
      multipleDaysWeather.setMinTemp(listItem.getTemp().getMin());
      multipleDaysWeather.setWeatherId(listItem.getWeather().get(0).getId());
      multipleDaysWeatherBox.put(multipleDaysWeather);
    }
    prefser.put(Constants.LAST_STORED_MULTIPLE_DAYS, System.currentTimeMillis());
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Dialog dialog = super.onCreateDialog(savedInstanceState);
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    dialog.setCancelable(true);
    WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
    lp.copyFrom(dialog.getWindow().getAttributes());
    lp.width = WindowManager.LayoutParams.MATCH_PARENT;
    lp.height = WindowManager.LayoutParams.MATCH_PARENT;
    dialog.getWindow().setAttributes(lp);
    return dialog;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    disposable.dispose();
  }
}
