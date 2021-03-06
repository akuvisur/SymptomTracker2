package com.comag.aku.lifetracker.data_syncronization;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.utils.Aware_Plugin;
import com.comag.aku.lifetracker.R;
import com.comag.aku.lifetracker.services.ApplicationMonitor;
import com.comag.aku.lifetracker.services.NotificationService;
import com.comag.aku.lifetracker.services.UserContextService;

/**
 * Created by aku on 10/12/15.
 */
public class Plugin extends Aware_Plugin {
    public static Uri[] URI;
    private static Context context;
    @Override
    public void onCreate() {
        super.onCreate();
        TAG = "AWARE::" + getResources().getString(R.string.app_name);
        Aware.setSetting(this, Aware_Preferences.DEBUG_FLAG, "true");

        Aware.startPlugin(this, "com.comag.aku.lifetracker");

        DATABASE_TABLES = SyncProvider.DATABASE_TABLES;
        TABLES_FIELDS = SyncProvider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{ SyncProvider.AdverseEventData.CONTENT_URI, SyncProvider.NotificationEventData.CONTENT_URI};
        URI = CONTEXT_URIS;

        context = getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }

    //This function gets called every 5 minutes by AWARE to make sure this plugin is still running.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("AWARE", "Running Symptom Tracker as plugin");
        //Check if the user has toggled the debug messages
        DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

        Aware.setSetting(this, "com.comag.aku.lifetracker", true);

        DATABASE_TABLES = SyncProvider.DATABASE_TABLES;
        TABLES_FIELDS = SyncProvider.TABLES_FIELDS;

        // start the services here so AWARE takes care of them running
        startService(new Intent(this, UserContextService.class));
        startService(new Intent(this, NotificationService.class));
        startService(new Intent(this, ApplicationMonitor.class));

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //Deactivate any sensors/plugins you activated here
        //e.g., Aware.setSetting(this, Aware_Preferences.STATUS_ACCELEROMETER, false);

        Log.d("AWARE", "Stopping Symptom Tracker plugin service");

        Aware.setSetting(this, "com.comag.aku.lifetracker", false);
        //Stop plugin
        Aware.stopPlugin(this, "com.comag.aku.lifetracker");
    }
}
