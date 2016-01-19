package com.comag.aku.lifetracker.graphics;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.widget.Toast;

import com.comag.aku.lifetracker.AppHelpers;
import com.comag.aku.lifetracker.Launch;
import com.comag.aku.lifetracker.analytics.AnalyticsApplication;
import com.comag.aku.lifetracker.app_settings.AppPreferences;
import com.comag.aku.lifetracker.services.NotificationService;

/**
 * Created by aku on 01/12/15.
 */
public class LeaveStudy {

    public void show(Activity a) throws Exception {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(a);
        dialogBuilder.setTitle("Are you sure?")
                .setMessage("Leaving a study causes all local data and preferences to be lost.")
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AnalyticsApplication.sendEvent("general", "leave_study", AppPreferences.schema.title, null);
                        AppPreferences.clear();
                        AppHelpers.currentActivity.stopService(new Intent(AppHelpers.currentActivity, NotificationService.class));

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                AppHelpers.currentActivity.startActivity(new Intent(AppHelpers.currentActivity, Launch.class));
                                Toast.makeText(AppHelpers.currentContext, "Cleared settings", Toast.LENGTH_LONG).show();
                            }
                        }, 1000);

                    }
                });
        Dialog dialog = dialogBuilder.create();
        dialog.show();
    }
}
