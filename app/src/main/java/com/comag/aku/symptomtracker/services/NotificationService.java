package com.comag.aku.symptomtracker.services;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Screen;
import com.comag.aku.symptomtracker.Launch;
import com.comag.aku.symptomtracker.MainActivity;
import com.comag.aku.symptomtracker.R;
import com.comag.aku.symptomtracker.app_settings.AppPreferences;
import com.comag.aku.symptomtracker.model.NoSQLStorage;

import java.util.Calendar;
import java.util.List;

/**
 * Created by aku on 25/11/15.
 */
public class NotificationService extends IntentService {

    public static int NOTIFICATION_DELAY_MS = 120000;

    public enum NotificationMode { DUMMY_MODE, LEARNING_MODE }

    private static NotificationMode mode = NotificationMode.DUMMY_MODE;
    public static NotificationMode getMode() {return mode;}
    public static void setMode(NotificationMode newMode) {mode = newMode;}

    public static boolean showingPopup = false;

    private static Context context;
    public static Context getContext() {return context;}

    private int screenStatus = 1;

    // listen to screen unlock/lock changes
    private class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case "ACTION_AWARE_SCREEN_LOCKED":
                    screenStatus = 0;
                    showingPopup = false;
                    break;
                case "ACTION_AWARE_SCREEN_UNLOCKED":
                    postOnUnlock();
                    screenStatus = 1;
                    break;
                default:
                    screenStatus = 0;
            }
        }
    }

    public NotificationService() {
        super("SymptomTrackerTimerService");
    }

    ScreenReceiver s;

    @Override
    public void onCreate() {
        Log.d("service", "created");
        context = this;

        Intent aware = new Intent(this, Aware.class);
        startService(aware);
        //Activate Accelerometer
        Aware.setSetting(this, Aware_Preferences.STATUS_SCREEN, true);
        //Apply settings
        Aware.startSensor(this, Aware_Preferences.STATUS_SCREEN);

        s = new ScreenReceiver();
        IntentFilter i = new IntentFilter();
        i.addAction(Screen.ACTION_AWARE_SCREEN_UNLOCKED);
        i.addAction(Screen.ACTION_AWARE_SCREEN_LOCKED);
        registerReceiver(s, i);

        NoSQLStorage.serviceLoad();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // if app not set up, no need for the service to run
        if (!AppPreferences.appIsSetUp()) return START_NOT_STICKY;
        // otherwise load the prefs etc..
        if (!AppPreferences.hasLoaded()) AppPreferences.load();
        switch (mode) {
            case DUMMY_MODE:
                emitDummyMode();
                emitNotification();
                break;
            case LEARNING_MODE:
                break;
        }

        return START_STICKY;
    }

    private void postOnUnlock() {
        if ((Math.random()*100) < NotificationPreferences.getCurrentPreference()) new InputPopup().show();
    }

    private void emitDummyMode() {
        Log.d("emit pop", "delay: " + AppPreferences.userSettings.getPopupInterval());
        if ((Math.random() * 100) < NotificationPreferences.getCurrentPreference()) {
            if (!mainRunning() && !showingPopup && screenStatus == 1 && mode.equals(NotificationMode.DUMMY_MODE)) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("postnew", "was not running - launch!");
                        emitNotification();
                        new InputPopup().show();
                        showingPopup = true;
                        emitDummyMode();
                    }
                }, AppPreferences.userSettings.getPopupInterval());
            } else if (mode.equals(NotificationMode.DUMMY_MODE)) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("postnew", "was running");
                        emitDummyMode();
                    }
                }, AppPreferences.userSettings.getPopupInterval());
            }
        }
        //
        else if (mode.equals(NotificationMode.DUMMY_MODE)) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d("postnew", "user didnt liek!");
                    emitDummyMode();
                }
            }, AppPreferences.userSettings.getPopupInterval());
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {}

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        Aware.stopSensor(this, Aware_Preferences.STATUS_SCREEN);

        if (s != null) unregisterReceiver(s);
        Intent aware = new Intent(this, Aware.class);
        stopService(aware);
    }

    private void emitNotification() {
        Calendar c = Calendar.getInstance();
        if (c.get(Calendar.HOUR_OF_DAY) > AppPreferences.userSettings.getNotificationHour()) {
            new ReminderNotification().show();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                emitNotification();
            }
        }, 1800000);
        /*
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.data_tab_icon)
                        .setContentTitle("My notification")
                        .setContentText("Hello World!");
        // Gets an instance of the NotificationController service
        NotificationController mNotifyMgr =
                (NotificationController) getSystemService(Context.NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(0, mBuilder.build());
        */
    }


    private boolean mainRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);
        for (int i = 0; i < runningTaskInfo.size(); i++) {
            if (runningTaskInfo.get(i).topActivity.getClassName().equals(MainActivity.class.getCanonicalName())) return true;
        }
        return false;
    }

}

