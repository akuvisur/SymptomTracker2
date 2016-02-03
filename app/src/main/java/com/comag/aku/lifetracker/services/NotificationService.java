package com.comag.aku.lifetracker.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
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

    public enum NotificationMode { DUMMY_MODE, LEARNING_MODE }

    private static NotificationMode mode = AppPreferences.getNotificationMode();
    public static NotificationMode getMode() {return mode;}
    public static void setMode(NotificationMode newMode) {
        Log.d("NotificationService", "Setting new mode! " + getModeInt());
        if (!SmartNotificationEngine.isEnabled()) {
            mode = NotificationMode.DUMMY_MODE;
        }
        else mode = newMode;
        AppPreferences.setNotificationMode(newMode.equals(NotificationMode.LEARNING_MODE));
    }

    public static int getModeInt() {
        switch (getMode()) {
            case DUMMY_MODE:
                return 0;
            case LEARNING_MODE:
                return 1;
        }
        return 0;
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
                        new Handler().postDelayed(new Runnable() {
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
        Aware.startSensor(this, Aware_Preferences.STATUS_SCREEN);

        Aware.setSetting(this, Aware_Preferences.STATUS_SCREEN, true);

        //startService(new Intent(this, Aware.class));

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

    // set system wide alarm to restart service if it has been shutdown
    final static int restartAlarmInterval = 20*60*1000;
    final static int resetAlarmTimer = 5*60*1000;
    public void setServiceKeepAlive() {
        final AlarmManager alarmMgr = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        final Intent restartIntent = new Intent(getApplicationContext(), NotificationService.class);
        restartIntent.putExtra("ALARM_RESTART_SERVICE_DIED", true);
        final RestartHandler restartServiceHandler = new RestartHandler(restartIntent, alarmMgr, getApplicationContext());
        restartServiceHandler.sendEmptyMessageDelayed(0, 0);
    }

    private static class RestartHandler extends Handler {
        Intent restartIntent;
        AlarmManager alarmMgr;
        Context applicationContext;
        public RestartHandler(Intent restartIntent, AlarmManager alarmMgr, Context applicationContext) {
            this.restartIntent = restartIntent;
            this.alarmMgr = alarmMgr;
            this.applicationContext = applicationContext;
        }

        @Override
        public void handleMessage(Message msg) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 8);
            PendingIntent pintent = PendingIntent.getService(applicationContext, 0, restartIntent, 0);

            // set restart reminder to 8 in the morning if in night hours (00:00 - 08:00)
            if ((System.currentTimeMillis() - cal.getTimeInMillis()) < 0) {
                alarmMgr.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + restartAlarmInterval, pintent);
                sendEmptyMessageDelayed(0, cal.getTimeInMillis() - System.currentTimeMillis());
            }
            else {
                alarmMgr.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + restartAlarmInterval, pintent);
                sendEmptyMessageDelayed(0, resetAlarmTimer);
            }
        }
    }

    private static boolean IS_RUNNING = false;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if ((intent != null) && (intent.getBooleanExtra("ALARM_RESTART_SERVICE_DIED", false)))
        {
            Log.d(LOG, "onStartCommand after ALARM_RESTART_SERVICE_DIED");
            if (IS_RUNNING)
            {
                Log.d(LOG, "Service already running - return immediately...");
                setServiceKeepAlive();
                return START_STICKY;
            }
        }

        // if app not set up, no need for the service to run
        if (!AppPreferences.appIsSetUp()) return START_NOT_STICKY;

        context = this;

        // otherwise load the prefs etc..
        if (!AppPreferences.hasLoaded()) AppPreferences.load();

        switch (mode) {
            case DUMMY_MODE:
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        emitDummyMode();
                        emitNotification();
                    }
                }, AppPreferences.userSettings.getPopupInterval());
                break;
            case LEARNING_MODE:
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        emitLearningMode();
                        emitNotification();
                    }
                }, AppPreferences.userSettings.getPopupInterval());
                break;
        }

        setServiceKeepAlive();

        IS_RUNNING = true;

        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        IS_RUNNING = false;
        Intent restartService = new Intent(getApplicationContext(),
                NotificationService.class);
        restartService.setPackage(getPackageName());
        PendingIntent restartServicePI = PendingIntent.getService(
                getApplicationContext(), 1, restartService,
                PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() +1000, restartServicePI);
    }

    private void postOnUnlock() {
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
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    emitDummyMode();
                }
            }, AppPreferences.userSettings.getPopupInterval());
            return;
        }
        try {
            if ((Math.random() * 100) < NotificationPreferences.getCurrentPreference()) {
                if (!mainRunning() && !AppHelpers.showingPopup && screenStatus == 1 && mode.equals(NotificationMode.DUMMY_MODE)) {
                    // thread this to prevent main thread crashing
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            new InputPopup().show();
                        }
                    }, 100);
                    // generate ML predictors
                    SmartNotificationEngine.emitNow();

                    AppHelpers.showingPopup = true;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            emitDummyMode();
                        }
                    }, AppPreferences.userSettings.getPopupInterval());
                } else if (mode.equals(NotificationMode.DUMMY_MODE)) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            emitDummyMode();
                        }
                    }, AppPreferences.userSettings.getPopupInterval());
                }
            }
            //
            else if (mode.equals(NotificationMode.DUMMY_MODE)) {
                SyncronizationController.storeNotificationResponse("no_popup_simple:" + NotificationPreferences.getCurrentPreference(), "not_shown", UserContextService.getUserContextString());
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        emitDummyMode();
                    }
                }, AppPreferences.userSettings.getPopupInterval());
            }
            else if (mode.equals(NotificationMode.LEARNING_MODE)) {
                SyncronizationController.storeNotificationResponse("no_popup_simple:" + + NotificationPreferences.getCurrentPreference(), "not_shown", UserContextService.getUserContextString());
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        emitLearningMode();
                    }
                }, AppPreferences.userSettings.getPopupInterval());
            }
        }
        catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            AnalyticsApplication.sendEvent("crash", "emitDummyMode()", sw.toString(), null);
        }

    }

    private void emitLearningMode() {
        if ((System.currentTimeMillis() - lastNotificationTime) < 5*AppHelpers.MINUTE_IN_MILLISECONDS) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    emitDummyMode();
                }
            }, AppPreferences.userSettings.getPopupInterval());
            return;
        }
        try {
            if (SmartNotificationEngine.emitNow() && !mainRunning() && !AppHelpers.showingPopup
                    && screenStatus == 1 && mode.equals(NotificationMode.LEARNING_MODE)) {
                new InputPopup().show();
                AppHelpers.showingPopup = true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        emitLearningMode();
                    }
                }, AppPreferences.userSettings.getPopupInterval());
            } else if (mode.equals(NotificationMode.LEARNING_MODE)) {
                SyncronizationController.storeNotificationResponse("no_popup_ml", "not_shown", UserContextService.getUserContextString());
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        emitLearningMode();
                    }
                }, AppPreferences.userSettings.getPopupInterval());
            } else if (mode.equals(NotificationMode.DUMMY_MODE)) {
                SyncronizationController.storeNotificationResponse("no_popup_ml", "not_shown", UserContextService.getUserContextString());
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        emitDummyMode();
                    }
                }, AppPreferences.userSettings.getPopupInterval());
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
        IS_RUNNING = false;

        Aware.stopSensor(this, Aware_Preferences.STATUS_SCREEN);

        try {
            if (s != null) unregisterReceiver(s);
            /*
            Intent aware = new Intent(this, Aware.class);
            stopService(aware);
            */
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
        /*
        Log.d("mainact", MainActivity.class.getCanonicalName());
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> runningTaskInfo = manager.getRunningTasks(1);
        Log.d("foreground", UserContextService.foreground_app);
        for (int i = 0; i < runningTaskInfo.size(); i++) {
            Log.d("running", runningTaskInfo.get(i).topActivity.getClassName());
            if (runningTaskInfo.get(i).topActivity.getClassName().equals(MainActivity.class.getCanonicalName())) return true;
        }
        return false;*/
    }

}

