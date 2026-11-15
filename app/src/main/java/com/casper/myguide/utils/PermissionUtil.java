package com.casper.myguide.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public abstract class PermissionUtil {

    public static final String STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    public static final String LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;

    public static final String[] PERMISSION_ALL = {
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    public static void goToPermissionSettingScreen(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", activity.getPackageName(), null));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    public static boolean isAllPermissionGranted(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : getRequiredPermissions()) {
                if (ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String[] getRequiredPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= 33) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 28) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return permissions.toArray(new String[0]);
    }

    public static String[] getDeniedPermission(Activity activity) {
        List<String> denied = new ArrayList<>();
        for (String permission : getRequiredPermissions()) {
            if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                denied.add(permission);
            }
        }
        return denied.toArray(new String[0]);
    }

    public static boolean isGranted(Context context, String permission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && Build.VERSION.SDK_INT >= 29) {
            return true;
        }
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isLocationGranted(Context context) {
        return isGranted(context, Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public static boolean isStorageGranted(Context context) {
        // Scoped storage is used on Android 10+; legacy storage permission is not required there.
        return Build.VERSION.SDK_INT >= 29 || isGranted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    public static void showSystemDialogPermission(Fragment fragment, String permission) {
        fragment.requestPermissions(new String[]{permission}, 200);
    }

    public static void showSystemDialogPermission(Activity activity, String permission) {
        activity.requestPermissions(new String[]{permission}, 200);
    }

    public static void showSystemDialogPermission(Activity activity, String permission, int code) {
        activity.requestPermissions(new String[]{permission}, code);
    }
}
