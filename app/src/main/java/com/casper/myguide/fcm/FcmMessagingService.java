package com.casper.myguide.fcm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.Map;

import com.casper.myguide.ActivityNewsInfoDetails;
import com.casper.myguide.ActivityPlaceDetail;
import com.casper.myguide.ActivitySplash;
import com.casper.myguide.R;
import com.casper.myguide.data.AppConfig;
import com.casper.myguide.data.Constant;
import com.casper.myguide.data.DatabaseHandler;
import com.casper.myguide.data.SharedPref;
import com.casper.myguide.model.FcmNotif;
import com.casper.myguide.model.NewsInfo;
import com.casper.myguide.model.Place;
import com.casper.myguide.utils.Tools;

public class FcmMessagingService extends FirebaseMessagingService {

    private static final int VIBRATION_TIME = 500;
    private SharedPref sharedPref;

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        sharedPref = new SharedPref(this);
        sharedPref.setFcmRegId(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        sharedPref = new SharedPref(this);
        sharedPref.setRefreshPlaces(true);

        if (AppConfig.REFRESH_IMG_NOTIF) {
            Tools.clearImageCacheOnBackground(this);
        }

        // Showing a notification does not require legacy external-storage permission.
        if (!sharedPref.getNotification()) return;

        final FcmNotif notification = new FcmNotif();
        if (!remoteMessage.getData().isEmpty()) {
            Map<String, String> data = remoteMessage.getData();
            notification.title = data.get("title");
            notification.content = data.get("content");
            notification.type = data.get("type");

            String placeJson = data.get("place");
            notification.place = placeJson != null ? new Gson().fromJson(placeJson, Place.class) : null;

            String newsJson = data.get("news");
            notification.news = newsJson != null ? new Gson().fromJson(newsJson, NewsInfo.class) : null;
        } else if (remoteMessage.getNotification() != null) {
            RemoteMessage.Notification remoteNotification = remoteMessage.getNotification();
            notification.title = remoteNotification.getTitle();
            notification.content = remoteNotification.getBody();
        }

        loadRetryImageFromUrl(this, notification, new CallbackImageNotif() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                displayNotificationIntent(notification, bitmap);
            }

            @Override
            public void onFailed(String message) {
                displayNotificationIntent(notification, null);
            }
        });
    }

    private void displayNotificationIntent(FcmNotif notification, Bitmap bitmap) {
        playRingtoneVibrate(this);
        Intent intent = new Intent(this, ActivitySplash.class);

        if (notification.place != null) {
            intent = ActivityPlaceDetail.navigateBase(this, notification.place, true);
        } else if (notification.news != null) {
            new DatabaseHandler(this).refreshTableNewsInfo();
            intent = ActivityNewsInfoDetails.navigateBase(this, notification.news, true);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int pendingIntentFlags = PendingIntent.FLAG_ONE_SHOT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags);

        String channelId = getString(R.string.default_notification_channel_id);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(notification.title)
                .setContentText(notification.content)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        if (bitmap != null) {
            builder.setStyle(new NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap).setSummaryText(notification.content));
        } else {
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(notification.content));
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, channelId, NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void playRingtoneVibrate(Context context) {
        try {
            if (sharedPref.getVibration()) {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) vibrator.vibrate(VIBRATION_TIME);
            }
            RingtoneManager.getRingtone(context, Uri.parse(sharedPref.getRingtone())).play();
        } catch (Exception exception) {
            Log.w(Constant.LOG_TAG, "Unable to play notification sound", exception);
        }
    }

    private void loadRetryImageFromUrl(Context context, FcmNotif notification, CallbackImageNotif callback) {
        String url = "";
        if (notification.place != null) {
            url = Constant.getURLimgPlace(notification.place.image);
        } else if (notification.news != null) {
            url = Constant.getURLimgNews(notification.news.image);
        }

        if (url.isEmpty()) {
            callback.onFailed("");
            return;
        }

        glideLoadImageFromUrl(context, url, callback);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable imageRunnable;

    private void glideLoadImageFromUrl(final Context context, final String url, final CallbackImageNotif callback) {
        imageRunnable = new Runnable() {
            @Override
            public void run() {
                Glide.with(context).asBitmap().load(url).into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap bitmap,
                                                @Nullable Transition<? super Bitmap> transition) {
                        callback.onSuccess(bitmap);
                        mainHandler.removeCallbacks(imageRunnable);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        callback.onFailed("Image load failed");
                        mainHandler.removeCallbacks(imageRunnable);
                    }
                });
            }
        };
        mainHandler.post(imageRunnable);
    }

    public interface CallbackImageNotif {
        void onSuccess(Bitmap bitmap);
        void onFailed(String message);
    }
}
