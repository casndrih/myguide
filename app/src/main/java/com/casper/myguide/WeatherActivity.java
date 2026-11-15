package com.casper.myguide;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.casper.myguide.data.ThisApplication;
import com.casper.myguide.databinding.ActivityWeatherBinding;
import com.casper.myguide.fragment.MultipleDaysFragment;
import com.casper.myguide.service.ApiService;
import com.casper.myguide.utils.Tools;
import com.casper.myguide.weathermodel.CityInfo;
import com.casper.myguide.weathermodel.currentweather.CurrentWeatherResponse;
import com.casper.myguide.weathermodel.daysweather.ListItem;
import com.casper.myguide.weathermodel.daysweather.MultipleDaysWeatherResponse;
import com.casper.myguide.weathermodel.db.CurrentWeather;
import com.casper.myguide.weathermodel.db.FiveDayWeather;
import com.casper.myguide.weathermodel.db.ItemHourlyDB;
import com.casper.myguide.weathermodel.db.PlaceLatlng;
import com.casper.myguide.weathermodel.fivedayweather.FiveDayResponse;
import com.casper.myguide.weathermodel.fivedayweather.ItemHourly;
import com.casper.myguide.weatherutils.ApiClient;
import com.casper.myguide.weatherutils.AppUtil;
import com.casper.myguide.weatherutils.Constants;
import com.casper.myguide.weatherutils.DbUtil;
import com.casper.myguide.weatherutils.SnackbarUtil;
import com.casper.myguide.weatherutils.TextViewFactory;
import com.github.pwittchen.prefser.library.rx2.Prefser;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.IAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.listeners.OnClickListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.android.AndroidScheduler;
import io.objectbox.query.Query;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataSubscriptionList;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import retrofit2.HttpException;

public class WeatherActivity extends AppCompatActivity {

    private FastAdapter<FiveDayWeather> mFastAdapter;
    private ItemAdapter<FiveDayWeather> mItemAdapter;
    private CompositeDisposable disposable = new CompositeDisposable();
    private String defaultLang = "en";
    private List<FiveDayWeather> fiveDayWeathers;
    private ApiService apiService;
    private FiveDayWeather todayFiveDayWeather;
    private Prefser prefser;
    private ActivityWeatherBinding binding;
    private Box<CurrentWeather> currentWeatherBox;
    private Box<FiveDayWeather> fiveDayWeatherBox;
    private Box<ItemHourlyDB> itemHourlyDBBox;
    private DataSubscriptionList subscriptions = new DataSubscriptionList();
    private boolean isLoad = false;
    private CityInfo cityInfo;
    private String apiKey;
    private Typeface typeface;
    private int[] colors;
    private int[] colorsAlpha;

    private double clat, clng;
    private String qlat, qlng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWeatherBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbarLayout.toolbar);

        initValues();
        initRecyclerView();
        setupTextSwitchers();
        showStoredCurrentWeather();
        showStoredFiveDayWeather();
        checkLastUpdate();

        // for system bar in lollipop
        Tools.setSystemBarColor(this);

        // Get latlng from intents
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            clat = extras.getDouble("wlat");
            clng = extras.getDouble("wlng");
            qlat = String.valueOf(clat);
            qlng = String.valueOf(clng);

            //Store latlng in sharedpref
            prefser.put("lat",qlat);
            prefser.put("lng",qlng);
        }

        requestWeather(qlat,qlng, true);
    }

    private void initValues() {
        colors = getResources().getIntArray(R.array.mdcolor_500);
        colorsAlpha = getResources().getIntArray(R.array.mdcolor_500_alpha);
        prefser = new Prefser(this);
        apiService = ApiClient.getClient().create(ApiService.class);
        BoxStore boxStore = ThisApplication.getBoxStore();
        currentWeatherBox = boxStore.boxFor(CurrentWeather.class);
        fiveDayWeatherBox = boxStore.boxFor(FiveDayWeather.class);
        itemHourlyDBBox = boxStore.boxFor(ItemHourlyDB.class);
        binding.swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        binding.swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                cityInfo = prefser.get(Constants.CITY_INFO, CityInfo.class, null);
                if (cityInfo != null) {
                    long lastStored = prefser.get(Constants.LAST_STORED_CURRENT, Long.class, 0L);
                    if (AppUtil.isTimePass(lastStored)) {
                        requestWeather(qlat,qlng, false);
                    } else {
                        binding.swipeContainer.setRefreshing(false);
                    }
                } else {
                    binding.swipeContainer.setRefreshing(false);
                }
            }

        });
        binding.bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        typeface = Typeface.createFromAsset(getAssets(), "fonts/Vazir.ttf");
        binding.nextDaysButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppUtil.showFragment(new MultipleDaysFragment(), getSupportFragmentManager(), true);
            }
        });
        binding.contentMainLayout.todayMaterialCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (todayFiveDayWeather != null) {
                    Intent intent = new Intent(WeatherActivity.this, HourlyActivity.class);
                    intent.putExtra(Constants.FIVE_DAY_WEATHER_ITEM, todayFiveDayWeather);
                    startActivity(intent);
                }
            }
        });
    }

    private void setupTextSwitchers() {
        binding.contentMainLayout.tempTextView.setFactory(new TextViewFactory(WeatherActivity.this, R.style.TempTextView, true, typeface));
        binding.contentMainLayout.tempTextView.setInAnimation(WeatherActivity.this, R.anim.slide_in_right);
        binding.contentMainLayout.tempTextView.setOutAnimation(WeatherActivity.this, R.anim.slide_out_left);
        binding.contentMainLayout.descriptionTextView.setFactory(new TextViewFactory(WeatherActivity.this, R.style.DescriptionTextView, true, typeface));
        binding.contentMainLayout.descriptionTextView.setInAnimation(WeatherActivity.this, R.anim.slide_in_right);
        binding.contentMainLayout.descriptionTextView.setOutAnimation(WeatherActivity.this, R.anim.slide_out_left);
        binding.contentMainLayout.humidityTextView.setFactory(new TextViewFactory(WeatherActivity.this, R.style.HumidityTextView, false, typeface));
        binding.contentMainLayout.humidityTextView.setInAnimation(WeatherActivity.this, R.anim.slide_in_bottom);
        binding.contentMainLayout.humidityTextView.setOutAnimation(WeatherActivity.this, R.anim.slide_out_top);
        binding.contentMainLayout.windTextView.setFactory(new TextViewFactory(WeatherActivity.this, R.style.WindSpeedTextView, false, typeface));
        binding.contentMainLayout.windTextView.setInAnimation(WeatherActivity.this, R.anim.slide_in_bottom);
        binding.contentMainLayout.windTextView.setOutAnimation(WeatherActivity.this, R.anim.slide_out_top);
    }

    private void initRecyclerView() {
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        binding.contentMainLayout.recyclerView.setLayoutManager(layoutManager);
        mItemAdapter = new ItemAdapter<>();
        mFastAdapter = FastAdapter.with(mItemAdapter);
        binding.contentMainLayout.recyclerView.setItemAnimator(new DefaultItemAnimator());
        binding.contentMainLayout.recyclerView.setAdapter(mFastAdapter);
        binding.contentMainLayout.recyclerView.setFocusable(false);
        mFastAdapter.withOnClickListener(new OnClickListener<FiveDayWeather>() {
            @Override
            public boolean onClick(@Nullable View v, @NonNull IAdapter<FiveDayWeather> adapter, @NonNull FiveDayWeather item, int position) {
                Intent intent = new Intent(WeatherActivity.this, HourlyActivity.class);
                intent.putExtra(Constants.FIVE_DAY_WEATHER_ITEM, item);
                startActivity(intent);
                return true;
            }
        });
    }

    private void showStoredCurrentWeather() {
        Query<CurrentWeather> query = DbUtil.getCurrentWeatherQuery(currentWeatherBox);
        query.subscribe(subscriptions).on(AndroidScheduler.mainThread())
                .observer(new DataObserver<List<CurrentWeather>>() {
                    @Override
                    public void onData(@NonNull List<CurrentWeather> data) {
                        if (data.size() > 0) {
                            hideEmptyLayout();
                            CurrentWeather currentWeather = data.get(0);
                            if (isLoad) {
                                binding.contentMainLayout.tempTextView.setText(String.format(Locale.getDefault(), "%.0f°", currentWeather.getTemp()));
                                binding.contentMainLayout.descriptionTextView.setText(AppUtil.getWeatherStatus(currentWeather.getWeatherId(), AppUtil.isRTL(WeatherActivity.this)));
                                binding.contentMainLayout.humidityTextView.setText(String.format(Locale.getDefault(), "%d%%", currentWeather.getHumidity()));
                                binding.contentMainLayout.windTextView.setText(String.format(Locale.getDefault(), getResources().getString(R.string.wind_unit_label), currentWeather.getWindSpeed()));
                            } else {
                                binding.contentMainLayout.tempTextView.setCurrentText(String.format(Locale.getDefault(), "%.0f°", currentWeather.getTemp()));
                                binding.contentMainLayout.descriptionTextView.setCurrentText(AppUtil.getWeatherStatus(currentWeather.getWeatherId(), AppUtil.isRTL(WeatherActivity.this)));
                                binding.contentMainLayout.humidityTextView.setCurrentText(String.format(Locale.getDefault(), "%d%%", currentWeather.getHumidity()));
                                binding.contentMainLayout.windTextView.setCurrentText(String.format(Locale.getDefault(), getResources().getString(R.string.wind_unit_label), currentWeather.getWindSpeed()));
                            }
                            binding.contentMainLayout.animationView.setAnimation(AppUtil.getWeatherAnimation(currentWeather.getWeatherId()));
                            binding.contentMainLayout.animationView.playAnimation();
                        }
                    }
                });
    }

    private void showStoredFiveDayWeather() {
        Query<FiveDayWeather> query = DbUtil.getFiveDayWeatherQuery(fiveDayWeatherBox);
        query.subscribe(subscriptions).on(AndroidScheduler.mainThread())
                .observer(new DataObserver<List<FiveDayWeather>>() {
                    @Override
                    public void onData(@NonNull List<FiveDayWeather> data) {
                        if (data.size() > 0) {
                            todayFiveDayWeather = data.remove(0);
                            mItemAdapter.clear();
                            mItemAdapter.add(data);
                        }
                    }
                });
    }

    private void checkLastUpdate() {
        cityInfo = prefser.get(Constants.CITY_INFO, CityInfo.class, null);
        if (cityInfo != null) {
            binding.toolbarLayout.cityNameTextView.setText(String.format("%s, %s", cityInfo.getName(), cityInfo.getCountry()));
            if (prefser.contains(Constants.LAST_STORED_CURRENT)) {
                long lastStored = prefser.get(Constants.LAST_STORED_CURRENT, Long.class, 0L);
                if (AppUtil.isTimePass(lastStored)) {
                    requestWeather(qlat,qlng, false);
                }
            } else {
                requestWeather(qlat,qlng, false);
            }
        } else {
          //Do nothing
        }

    }


    private void requestWeather(String lat,String lng, boolean isSearch) {
        if (AppUtil.isNetworkConnected()) {
            getCurrentWeather(lat,lng, isSearch);
            getFiveDaysWeather(lat,lng);
        } else {
            SnackbarUtil
                    .with(binding.swipeContainer)
                    .setMessage(getString(R.string.no_internet_message))
                    .setDuration(SnackbarUtil.LENGTH_LONG)
                    .showError();
            binding.swipeContainer.setRefreshing(false);
        }
    }

    private void getCurrentWeather(String lat,String lng, boolean isSearch) {
        apiKey = getResources().getString(R.string.open_weather_maps_app_id);
        disposable.add(
                apiService.getCurrentWeather(
                        lat,lng, Constants.UNITS, defaultLang, apiKey)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableSingleObserver<CurrentWeatherResponse>() {
                            @Override
                            public void onSuccess(CurrentWeatherResponse currentWeatherResponse) {
                                isLoad = true;
                                storeCurrentWeather(currentWeatherResponse);
                                storeCityInfo(currentWeatherResponse);
                                binding.swipeContainer.setRefreshing(false);
                                if (isSearch) {
                                    prefser.remove(Constants.LAST_STORED_MULTIPLE_DAYS);
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                binding.swipeContainer.setRefreshing(false);
                                try {
                                    HttpException error = (HttpException) e;
                                    handleErrorCode(error);
                                } catch (Exception exception) {
                                    e.printStackTrace();
                                }
                            }
                        })

        );
    }

    private void handleErrorCode(HttpException error) {
        if (error.code() == 404) {
            SnackbarUtil
                    .with(binding.swipeContainer)
                    .setMessage(getString(R.string.no_city_found_message))
                    .setDuration(SnackbarUtil.LENGTH_INDEFINITE)
                    .showWarning();

        } else if (error.code() == 401) {
            SnackbarUtil
                    .with(binding.swipeContainer)
                    .setMessage(getString(R.string.invalid_api_key_message))
                    .setDuration(SnackbarUtil.LENGTH_INDEFINITE)
                    .setAction(getString(R.string.ok_label), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish();
                        }
                    })
                    .showError();

        } else {
            SnackbarUtil
                    .with(binding.swipeContainer)
                    .setMessage(getString(R.string.network_exception_message))
                    .setDuration(SnackbarUtil.LENGTH_LONG)
                    .setAction(getResources().getString(R.string.retry_label), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (cityInfo != null) {
                                requestWeather(qlat,qlng, false);
                            } else {

                            }
                        }
                    })
                    .showWarning();
        }
    }

    private void hideEmptyLayout() {
        binding.contentMainLayout.nestedScrollView.setVisibility(View.VISIBLE);
    }

    private void storeCurrentWeather(CurrentWeatherResponse response) {
        CurrentWeather currentWeather = new CurrentWeather();
        currentWeather.setTemp(response.getMain().getTemp());
        currentWeather.setHumidity(response.getMain().getHumidity());
        currentWeather.setDescription(response.getWeather().get(0).getDescription());
        currentWeather.setMain(response.getWeather().get(0).getMain());
        currentWeather.setWeatherId(response.getWeather().get(0).getId());
        currentWeather.setWindDeg(response.getWind().getDeg());
        currentWeather.setWindSpeed(response.getWind().getSpeed());
        currentWeather.setStoreTimestamp(System.currentTimeMillis());
        prefser.put(Constants.LAST_STORED_CURRENT, System.currentTimeMillis());
        if (!currentWeatherBox.isEmpty()) {
            currentWeatherBox.removeAll();
            currentWeatherBox.put(currentWeather);
        } else {
            currentWeatherBox.put(currentWeather);
        }
    }

    private void storeCityInfo(CurrentWeatherResponse response) {
        CityInfo cityInfo = new CityInfo();
        cityInfo.setCountry(response.getSys().getCountry());
        cityInfo.setId(response.getId());
        cityInfo.setName(response.getName());
        prefser.put(Constants.CITY_INFO, cityInfo);
        binding.toolbarLayout.cityNameTextView.setText(String.format("%s, %s", cityInfo.getName(), cityInfo.getCountry()));
    }

    private void getFiveDaysWeather(String lat, String lng) {
        disposable.add(
                apiService.getMultipleDaysWeather(
                        lat, lng, Constants.UNITS, defaultLang, 5, apiKey)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableSingleObserver<MultipleDaysWeatherResponse>() {
                            @Override
                            public void onSuccess(MultipleDaysWeatherResponse response) {
                                handleFiveDayResponse(response, lat, lng);
                            }

                            @Override
                            public void onError(Throwable e) {
                                e.printStackTrace();
                            }
                        })
        );
    }

    private void handleFiveDayResponse(MultipleDaysWeatherResponse response, String lat, String lng) {
        fiveDayWeathers = new ArrayList<>();
        List<ListItem> list = response.getList();
        int day = 0;
        for (ListItem item : list) {
            int color = colors[day];
            int colorAlpha = colorsAlpha[day];
            Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
            Calendar newCalendar = AppUtil.addDays(calendar, day);
            FiveDayWeather fiveDayWeather = new FiveDayWeather();
            fiveDayWeather.setWeatherId(item.getWeather().get(0).getId());
            fiveDayWeather.setDt(item.getDt());
            fiveDayWeather.setMaxTemp(item.getTemp().getMax());
            fiveDayWeather.setMinTemp(item.getTemp().getMin());
            fiveDayWeather.setTemp(item.getTemp().getDay());
            fiveDayWeather.setColor(color);
            fiveDayWeather.setColorAlpha(colorAlpha);
            fiveDayWeather.setTimestampStart(AppUtil.getStartOfDayTimestamp(newCalendar));
            fiveDayWeather.setTimestampEnd(AppUtil.getEndOfDayTimestamp(newCalendar));
            fiveDayWeathers.add(fiveDayWeather);
            day++;
        }
        getFiveDaysHourlyWeather(lat, lng);
    }

    private void getFiveDaysHourlyWeather(String lat, String lng) {
        disposable.add(
                apiService.getFiveDaysWeather(
                        lat, lng, Constants.UNITS, defaultLang, apiKey)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableSingleObserver<FiveDayResponse>() {
                            @Override
                            public void onSuccess(FiveDayResponse response) {
                                handleFiveDayHourlyResponse(response);
                            }

                            @Override
                            public void onError(Throwable e) {
                                e.printStackTrace();
                            }
                        })

        );
    }

    private void handleFiveDayHourlyResponse(FiveDayResponse response) {
        if (!fiveDayWeatherBox.isEmpty()) {
            fiveDayWeatherBox.removeAll();
        }
        if (!itemHourlyDBBox.isEmpty()) {
            itemHourlyDBBox.removeAll();
        }
        for (FiveDayWeather fiveDayWeather : fiveDayWeathers) {
            long fiveDayWeatherId = fiveDayWeatherBox.put(fiveDayWeather);
            ArrayList<ItemHourly> listItemHourlies = new ArrayList<>(response.getList());
            for (ItemHourly itemHourly : listItemHourlies) {
                Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
                calendar.setTimeInMillis(itemHourly.getDt() * 1000L);
                if (calendar.getTimeInMillis()
                        <= fiveDayWeather.getTimestampEnd()
                        && calendar.getTimeInMillis()
                        > fiveDayWeather.getTimestampStart()) {
                    ItemHourlyDB itemHourlyDB = new ItemHourlyDB();
                    itemHourlyDB.setDt(itemHourly.getDt());
                    itemHourlyDB.setFiveDayWeatherId(fiveDayWeatherId);
                    itemHourlyDB.setTemp(itemHourly.getMain().getTemp());
                    itemHourlyDB.setWeatherCode(itemHourly.getWeather().get(0).getId());
                    itemHourlyDBBox.put(itemHourlyDB);
                }
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.dispose();
    }

    @Override
    public void onBackPressed() {
            super.onBackPressed();
    }


}
