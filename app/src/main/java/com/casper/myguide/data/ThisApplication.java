package com.casper.myguide.data;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import com.casper.myguide.BuildConfig;
import com.casper.myguide.R;
import com.casper.myguide.connection.API;
import com.casper.myguide.connection.RestAdapter;
import com.casper.myguide.model.DeviceInfo;
import com.casper.myguide.weathermodel.db.MyObjectBox;
import com.casper.myguide.utils.Tools;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessaging;

import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;
import io.objectbox.BoxStore;
import io.objectbox.android.AndroidObjectBrowser;
import retrofit2.Call;
import retrofit2.Response;

public class ThisApplication extends Application {

    private static BoxStore boxStore;
    private static ThisApplication mInstance;
    private FirebaseAnalytics firebaseAnalytics;
    private Location location = null;
    private SharedPref sharedPref;

    public static BoxStore getBoxStore() {
        return boxStore;
    }

    @Override
    public void onCreate() {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
        super.onCreate();
        Log.d(Constant.LOG_TAG, "onCreate : ThisApplication");
        mInstance = this;
        sharedPref = new SharedPref(this);

        FirebaseApp.initializeApp(this);
        obtainFirebaseToken();

        getFirebaseAnalytics();

        ViewPump.init(ViewPump.builder()
                .addInterceptor(new CalligraphyInterceptor(
                        new CalligraphyConfig.Builder()
                                .setDefaultFontPath("fonts/Vazir.ttf")
                                .setFontAttrId(R.attr.fontPath)
                                .build()))
                .build());
        createBoxStore();
    }

    private void createBoxStore() {
        boxStore = MyObjectBox.builder().androidContext(ThisApplication.this).build();
        if (BuildConfig.DEBUG) {
            new AndroidObjectBrowser(boxStore).start(this);
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public static synchronized ThisApplication getInstance() {
        return mInstance;
    }

    private void obtainFirebaseToken() {
        if (!Tools.cekConnection(this)) return;

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String token) {
                        if (!TextUtils.isEmpty(token)) sendRegistrationToServer(token);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Firebase Messaging retries token refresh internally; avoid recursive retries.
                        Log.e(Constant.LOG_TAG, "Failed to obtain FCM token: " + e.getMessage());
                    }
                });
    }

    private void sendRegistrationToServer(String token) {
        if (!Tools.cekConnection(this) || TextUtils.isEmpty(token)) return;

        API api = RestAdapter.createAPI();
        DeviceInfo deviceInfo = Tools.getDeviceInfo(this);
        deviceInfo.setRegid(token);

        Call<com.casper.myguide.connection.callbacks.CallbackDevice> callback = api.registerDevice(deviceInfo);
        callback.enqueue(new retrofit2.Callback<com.casper.myguide.connection.callbacks.CallbackDevice>() {
            @Override
            public void onResponse(Call<com.casper.myguide.connection.callbacks.CallbackDevice> call,
                                   Response<com.casper.myguide.connection.callbacks.CallbackDevice> response) {
                if (!response.isSuccessful()) {
                    Log.w(Constant.LOG_TAG, "FCM registration returned HTTP " + response.code());
                }
            }

            @Override
            public void onFailure(Call<com.casper.myguide.connection.callbacks.CallbackDevice> call, Throwable t) {
                Log.e(Constant.LOG_TAG, "FCM registration failed", t);
            }
        });
    }

    public synchronized FirebaseAnalytics getFirebaseAnalytics() {
        if (firebaseAnalytics == null && AppConfig.ENABLE_ANALYTICS) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        }
        return firebaseAnalytics;
    }

    public void trackScreenView(String event) {
        if (firebaseAnalytics == null || !AppConfig.ENABLE_ANALYTICS) return;
        Bundle params = new Bundle();
        event = event.replaceAll("[^A-Za-z0-9_]", "");
        params.putString("event", event);
        firebaseAnalytics.logEvent(event, params);
    }

    public void trackEvent(String category, String action, String label) {
        if (firebaseAnalytics == null || !AppConfig.ENABLE_ANALYTICS) return;
        Bundle params = new Bundle();
        category = category.replaceAll("[^A-Za-z0-9_]", "");
        action = action.replaceAll("[^A-Za-z0-9_]", "");
        label = label.replaceAll("[^A-Za-z0-9_]", "");
        params.putString("category", category);
        params.putString("action", action);
        params.putString("label", label);
        firebaseAnalytics.logEvent("EVENT", params);
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
