package com.comag.aku.lifetracker.services;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.Log;

import com.aware.Aware;
import com.aware.Screen;
import com.comag.aku.lifetracker.AppHelpers;
import com.comag.aku.lifetracker.R;
import com.comag.aku.lifetracker.analytics.AnalyticsApplication;
import com.comag.aku.lifetracker.app_settings.AppPreferences;
import com.comag.aku.lifetracker.data_syncronization.Plugin;
import com.comag.aku.lifetracker.data_syncronization.SyncronizationController;
import com.comag.aku.lifetracker.model.ApiManager;
import com.comag.aku.lifetracker.model.NoSQLStorage;
import com.comag.aku.lifetracker.services.smart_notifications.SmartNotificationEngine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;

/**
 * Created by aku on 25/11/15.
 */
public class NotificationService extends IntentService {

    private static final String LOG = "NotificationService";


    public static int NOTIFICATION_DELAY_MS = 20 * AppHelpers.MINUTE_IN_MILLISECONDS;

    public static long lastNotificationTime = System.currentTimeMillis();

    public enum NotificationMode { DUMMY_MODE, LEARNING_MODE, UNDEFINED }

    private static Handler handler = new Handler();

    //private static NotificationMode mode = AppPreferences.getNotificationMode();
    private static NotificationMode mode = NotificationMode.LEARNING_MODE;
    public static NotificationMode getMode() {return mode;}
    public static void setMode(NotificationMode newMode) {
        mode = NotificationMode.LEARNING_MODE;
        /*
        AppPreferences.setNotificationMode(true);
        mode = AppPreferences.getNotificationMode();

        Log.d("NotificationService", "Setting new mode! " + getModeInt());
        if (!SmartNotificationEngine.isEnabled()) {
            mode = NotificationMode.DUMMY_MODE;
        }
        else mode = newMode;
        AppPreferences.setNotificationMode(newMode.equals(NotificationMode.LEARNING_MODE));
        */
    }

    public static int getModeInt() {
        AppPreferences.setNotificationMode(true);
        return 1;
        /*
        mode = AppPreferences.getNotificationMode();
        switch (getMode()) {
            case DUMMY_MODE:
                return 0;
            case LEARNING_MODE:
                return 1;
        }
        return -1;
        */
    }

    private static Context context;
    public static Context getContext() {return context;}

    private int screenStatus = 1;

    private long lastSymptomSync = System.currentTimeMillis();
    // listen to screen unlock/lock changes
    private class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case "ACTION_AWARE_SCREEN_LOCKED":
                    screenStatus = 0;
                    AppHelpers.showingPopup = false;
                    // prevent duplicate popups
                    InputPopup.hidePopup();

                    // if 12 hours since last sync
                    if ((System.currentTimeMillis() - lastSymptomSync) > 60 * 12 * AppHelpers.MINUTE_IN_MILLISECONDS) {
                        // send threaded query to dashboard to get symptom info +
                        // check if notitication_mode has changed
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                lastSymptomSync = System.currentTimeMillis();
                                ApiManager.getSymptomsForSchema();
                            }
                        }, 1000);
                    }

                    break;
                case "ACTION_AWARE_SCREEN_UNLOCKED":
                    if (!AppHelpers.showingPopup) postOnUnlock();
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

        //Apply settings
        Aware.startScreen(this);

        s = new ScreenReceiver();
        IntentFilter i = new IntentFilter();
        i.addAction(Screen.ACTION_AWARE_SCREEN_UNLOCKED);
        i.addAction(Screen.ACTION_AWARE_SCREEN_LOCKED);
        registerReceiver(s, i);

        NoSQLStorage.serviceLoad();

        // launch data storage and sync service
        Intent plugin = new Intent(this, Plugin.class);
        startService(plugin);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // if app not set up, no need for the service to run
        if (!AppPreferences.appIsSetUp()) return START_NOT_STICKY;

        context = this;

        // otherwise load the prefs etc..
        if (!AppPreferences.hasLoaded()) AppPreferences.load();

        return START_STICKY;
    }

    private void postOnUnlock() {
        Log.d("PostOnUnlock", getModeInt() + "");
        if (mode == NotificationMode.UNDEFINED) {
            mode = AppPreferences.getNotificationMode();
        }
        // for debug
        //SmartNotificationEngine.generatePastContext();
        try {
            if (mode == NotificationMode.DUMMY_MODE) {
                if ((Math.random() * 100) < NotificationPreferences.getCurrentPreference()) {
                    new InputPopup().show();
                    SmartNotificationEngine.emitNow();
                }
                else {
                    SmartNotificationEngine.emitNow();
                    SyncronizationController.storeNotificationResponse("no_popup_simple:" + + NotificationPreferences.getCurrentPreference(), "not_shown", UserContextService.getUserContextString());
                }
            }
            else if (mode == NotificationMode.LEARNING_MODE) {
                if (SmartNotificationEngine.emitNow()) {
                    new InputPopup().show();
                }
                else {
                    SyncronizationController.storeNotificationResponse("no_popup_ml", "not_shown", UserContextService.getUserContextString());
                }
            }
        }
        catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            AnalyticsApplication.sendEvent("crash", "postOnUnlock()", sw.toString(), null);
        }
    }

    private void emitDummyMode() {
        if ((System.currentTimeMillis() - lastNotificationTime) < 5*AppHelpers.MINUTE_IN_MILLISECONDS) {
            return;
        }
        if (mode == NotificationMode.UNDEFINED) {
            mode = AppPreferences.getNotificationMode();
        }
        try {
            if ((Math.random() * 100) < NotificationPreferences.getCurrentPreference()) {
                if (!mainRunning() && !AppHelpers.showingPopup && screenStatus == 1 && mode.equals(NotificationMode.DUMMY_MODE)) {
                    // thread this to prevent main thread crashing
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            new InputPopup().show();
                        }
                    }, 100);
                    // generate ML predictors
                    SmartNotificationEngine.emitNow();

                    AppHelpers.showingPopup = true;
                    handler.postDelayed(emitDummy, AppPreferences.userSettings.getPopupInterval());
                } else if (mode.equals(NotificationMode.DUMMY_MODE)) {
                    handler.postDelayed(emitDummy, AppPreferences.userSettings.getPopupInterval());
                }
            }

            else if (mode.equals(NotificationMode.DUMMY_MODE)) {
                SyncronizationController.storeNotificationResponse("no_popup_simple:" + NotificationPreferences.getCurrentPreference(), "not_shown", UserContextService.getUserContextString());
                handler.postDelayed(emitDummy, AppPreferences.userSettings.getPopupInterval());
            }
            else if (mode.equals(NotificationMode.LEARNING_MODE)) {
                SyncronizationController.storeNotificationResponse("no_popup_simple:" + + NotificationPreferences.getCurrentPreference(), "not_shown", UserContextService.getUserContextString());
                handler.postDelayed(emitLearning, AppPreferences.userSettings.getPopupInterval());
            }
        }
        catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            AnalyticsApplication.sendEvent("crash", "emitDummyMode()", sw.toString(), null);
        }

    }

    private Runnable emitLearning = new Runnable() {
        @Override
        public void run() {
            emitLearningMode();
        }
    };


    private Runnable emitDummy = new Runnable() {
        @Override
        public void run() {
            emitDummyMode();
        }
    };

    private void emitLearningMode() {
        if ((System.currentTimeMillis() - lastNotificationTime) < 5*AppHelpers.MINUTE_IN_MILLISECONDS) {
            handler.postDelayed(emitLearning, AppPreferences.userSettings.getPopupInterval());
            return;
        }
        if (mode == NotificationMode.UNDEFINED) {
            mode = AppPreferences.getNotificationMode();
        }
        try {
            if (SmartNotificationEngine.emitNow() && !mainRunning() && !AppHelpers.showingPopup
                    && screenStatus == 1 && mode.equals(NotificationMode.LEARNING_MODE)) {
                new InputPopup().show();
                AppHelpers.showingPopup = true;
                handler.postDelayed(emitLearning, AppPreferences.userSettings.getPopupInterval());
            } else if (mode.equals(NotificationMode.LEARNING_MODE)) {
                SyncronizationController.storeNotificationResponse("no_popup_ml", "not_shown", UserContextService.getUserContextString());
                handler.postDelayed(emitLearning, AppPreferences.userSettings.getPopupInterval());
            } else if (mode.equals(NotificationMode.DUMMY_MODE)) {
                SyncronizationController.storeNotificationResponse("no_popup_ml", "not_shown", UserContextService.getUserContextString());
                handler.postDelayed(emitDummy, AppPreferences.userSettings.getPopupInterval());
            }
        }
        catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            AnalyticsApplication.sendEvent("crash", "emitLearningMode()", sw.toString(), null);
        }
    }


    @Override
    protected void onHandleIntent(Intent intent) {}

    @Override
    public void onDestroy() {
        Aware.stopScreen(this);

        handler.removeCallbacks(emitLearning);
        handler.removeCallbacks(emitDummy);

        try {
            if (s != null) unregisterReceiver(s);
        }
        catch (Exception e) {}
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
        }, 30 * AppHelpers.MINUTE_IN_MILLISECONDS);
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
        try {
            if (UserContextService.foreground_app != null) return UserContextService.foreground_app.value.equals(getResources().getString(R.string.app_name));
            else {
                return false;
            }
        }
        catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            AnalyticsApplication.sendEvent("crash", "mainRunning()", sw.toString(), null);
            // return true to prevent popups in case of crashes
            return true;
        }

    }

}

