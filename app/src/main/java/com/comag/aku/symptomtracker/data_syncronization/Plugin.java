package com.comag.aku.symptomtracker.data_syncronization;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;
import com.comag.aku.symptomtracker.R;

/**
 * Created by aku on 10/12/15.
 */
public class Plugin extends Aware_Plugin {

    @Override
    public void onCreate() {
        super.onCreate();
        TAG = "AWARE::" + getResources().getString(R.string.app_name);
        Aware.setSetting(this, Aware_Preferences.DEBUG_FLAG, "true");

        Aware.startPlugin(this, "com.comag.aku.symptomtracker");
        Log.d("AWARE", "Created and started Symptom Tracker as plugin");

        DATABASE_TABLES = SyncProvider.DATABASE_TABLES;
        TABLES_FIELDS = SyncProvider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ SyncProvider.NotificationEventData.CONTENT_URI, SyncProvider.AdverseEventData.CONTENT_URI};

    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("AWARE", "Running Symptom Tracker as plugin");
        //Check if the user has toggled the debug messages
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //Deactivate any sensors/plugins you activated here
        //e.g., Aware.setSetting(this, Aware_Preferences.STATUS_ACCELEROMETER, false);

        Log.d("AWARE", "Stopping Symptom Tracker plugin service");

        //Stop plugin
        Aware.stopPlugin(this, "com.comag.aku.symptomtracker");
    }
}
