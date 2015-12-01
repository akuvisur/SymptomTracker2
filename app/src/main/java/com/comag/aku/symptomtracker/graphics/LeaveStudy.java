package com.comag.aku.symptomtracker.graphics;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.Toast;

import com.comag.aku.symptomtracker.Launch;
import com.comag.aku.symptomtracker.MainActivity;
import com.comag.aku.symptomtracker.Settings;
import com.comag.aku.symptomtracker.app_settings.AppPreferences;

/**
 * Created by aku on 01/12/15.
 */
public class LeaveStudy {
    private Dialog dialog;

    public void show(Activity a) {
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
                        AppPreferences.clear();
                        Settings.currentActivity.startActivity(new Intent(Settings.currentActivity, Launch.class));
                        Toast.makeText(Settings.currentContext, "Cleared settings", Toast.LENGTH_LONG).show();
                    }
                });
        dialog = dialogBuilder.create();
        dialog.show();
    }
}
