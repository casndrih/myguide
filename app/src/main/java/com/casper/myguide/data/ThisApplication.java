package com.casper.myguide.data;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;

import android.text.TextUtils;
import android.util.Log;

import com.casper.myguide.BuildConfig;
import com.casper.myguide.weathermodel.db.MyObjectBox;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import com.casper.myguide.R;
import com.casper.myguide.connection.API;
import com.casper.myguide.connection.RestAdapter;
import com.casper.myguide.connection.callbacks.CallbackDevice;
import com.casper.myguide.model.DeviceInfo;
import com.casper.myguide.utils.Tools;

import io.github.inflationx.calligraphy3.CalligraphyConfig;
import io.github.inflationx.calligraphy3.CalligraphyInterceptor;
import io.github.inflationx.viewpump.ViewPump;
import io.objectbox.BoxStore;
import io.objectbox.android.AndroidObjectBrowser;

import retrofit2.Call;
import retrofit2.Response;

public class ThisApplication extends Application {

    private static BoxStore boxStore;
    private Call<CallbackDevice> callback = null;
    private static ThisApplication mInstance;
    private FirebaseAnalytics firebaseAnalytics;
    private Location location = null;
    private SharedPref sharedPref;
    private int fcm_count = 0;
    private final int FCM_MAX_COUNT = 5;

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

        // initialize firebase
        FirebaseApp firebaseApp = FirebaseApp.initializeApp(this);

        // obtain regId & registering device to server
        obtainFirebaseToken(firebaseApp);

        // activate analytics tracker
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


    private void obtainFirebaseToken(final FirebaseApp firebaseApp) {
        if (!Tools.cekConnection(this)) return;
        fcm_count++;

        Task<InstanceIdResult> resultTask = FirebaseInstanceId.getInstance().getInstanceId();
        resultTask.addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                String regId = instanceIdResult.getToken();
                if (!TextUtils.isEmpty(regId)) sendRegistrationToServer(regId);
            }
        });

        resultTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(Constant.LOG_TAG, "Failed obtain fcmID : " + e.getMessage());
                if (fcm_count > FCM_MAX_COUNT) return;
                obtainFirebaseToken(firebaseApp);
            }
        });
    }

    /**
     * --------------------------------------------------------------------------------------------
     * For Firebase Cloud Messaging
     */
    private void sendRegistrationToServer(String token) {
        if (Tools.cekConnection(this) && !TextUtils.isEmpty(token)) {
            API api = RestAdapter.createAPI();
            DeviceInfo deviceInfo = Tools.getDeviceInfo(this);
            deviceInfo.setRegid(token);

            callback = api.registerDevice(deviceInfo);
            callback.enqueue(new retrofit2.Callback<CallbackDevice>() {
                @Override
                public void onResponse(Call<CallbackDevice> call, Response<CallbackDevice> response) {
                    CallbackDevice resp = response.body();
                }

                @Override
                public void onFailure(Call<CallbackDevice> call, Throwable t) {
                }
            });
        }
    }


    /**
     * --------------------------------------------------------------------------------------------
     * For Google Analytics
     */
    public synchronized FirebaseAnalytics getFirebaseAnalytics() {
        if (firebaseAnalytics == null && AppConfig.ENABLE_ANALYTICS) {
            // Obtain the Firebase Analytics.
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

    /**
     * ---------------------------------------- End of analytics ---------------------------------
     */

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}
