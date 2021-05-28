package com.casper.myguide;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Timer;
import java.util.TimerTask;

import com.casper.myguide.data.SharedPref;
import com.casper.myguide.utils.PermissionUtil;
import com.casper.myguide.utils.Tools;
import com.github.pwittchen.prefser.library.rx2.Prefser;

public class ActivitySplash extends AppCompatActivity {

    private SharedPref sharedPref;
    private Prefser prefser;
    private Boolean open;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        sharedPref = new SharedPref(this);

        //Check if first time
        prefser = new Prefser(ActivitySplash.this);
        open = prefser.get("openAlready", Boolean.class, false);
        if (open) {
            //Do nothing, Let them in
        } else {
            Intent i = new Intent(this, ActivityWizard.class);
            startActivity(i);
        }

        // permission checker for android M or higher
        if (Tools.needRequestPermission()) {
            String[] permission = PermissionUtil.getDeniedPermission(this);
            if (permission.length != 0) {
                requestPermissions(permission, 200);
            } else {
                startActivityMainDelay();
            }
        } else {
            startActivityMainDelay();
        }

        // for system bar in lollipop
        Tools.setSystemBarColor(this);
        Tools.setSystemBarLight(this);
    }

    private void startActivityMainDelay() {
        // Show splash screen for 2 seconds
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Intent i = new Intent(ActivitySplash.this, ActivityMain.class);
                startActivity(i);
                finish(); // kill current activity
            }
        };
        new Timer().schedule(task, 2000);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 200) {
            for (String perm : permissions) {
                boolean rationale = shouldShowRequestPermissionRationale(perm);
                sharedPref.setNeverAskAgain(perm, !rationale);
            }
            startActivityMainDelay();
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

}
