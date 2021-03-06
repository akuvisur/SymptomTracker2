package com.comag.aku.lifetracker.services;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.Battery;
import com.aware.Communication;
import com.aware.Network;
import com.aware.Proximity;
import com.aware.Rotation;
import com.aware.plugin.google.activity_recognition.Plugin;
import com.aware.providers.Applications_Provider;
import com.aware.providers.Battery_Provider;
import com.comag.aku.lifetracker.analytics.AnalyticsApplication;
import com.comag.aku.lifetracker.app_settings.AppPreferences;
import com.gc.android.market.api.MarketSession;
import com.gc.android.market.api.model.Market;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by aku on 11/12/15.
 */
public class UserContextService extends IntentService {
    private final static String LOG = "UserContextService";

    static String input_source = "app";
    public static void setInputSource(String source) {
        if (source != null) input_source = source;
    }
    public static String getInputSource() {return input_source;}

    public static final List<String> required = Arrays.asList(
            "timestamp",
            "hour",
            "minute",
            "day_of_week",
            "battery_level",
            "battery_charging",
            //"foreground_app",
            "foreground_package",
            //"foreground_app_category",
            "proximity",
            "last_call",
            "internet_available",
            "wifi_available",
            "network_type",
            "last_action",
            "activity"
            );

    static int postureBuffer = 0;
    static Tuple<Long, Integer> hour;
    static Tuple<Long, Integer> minute;
    static Tuple<Long, Integer> day_of_week;
    // too high frequency monitoring
    //static Integer device_posture;
    static Tuple<Long, Integer> battery_level;
    static Tuple<Long, Integer> battery_charging;
    static Tuple<Long, String> foreground_app;
    static Tuple<Long, String> foreground_package;
    static Tuple<Long, String> foreground_app_category;
    static Tuple<Long, Integer> proximity;
    static Tuple<Long, Long> last_call;
    static Tuple<Long, Integer> internet_available = new Tuple(System.currentTimeMillis(), 0);
    static Tuple<Long, Integer> wifi_available = new Tuple(System.currentTimeMillis(), 0);
    static Tuple<Long, Integer> network_type;
    static Tuple<Long, Long> last_action;
    static Tuple<Long, Integer> activity;

    // change each variable to have timestamp,object structure to prevent too old values to be set to context
    public static class Tuple<Long, Y> {
        public final Long time;
        public final Y value;
        public Tuple(Long time, Y y) {
            this.time = time;
            this.value = y;
        }
    }

    // activity
    // indoor/outdoor
    // location type / foursquare/google places

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
    */
    public UserContextService() {
        super("UserContextService");
    }

    public static void setLastAction() {
        last_action = new Tuple(System.currentTimeMillis(),System.currentTimeMillis());
    }

    private class ContextReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(Rotation.ACTION_AWARE_ROTATION)) Log.d("ReceivedAction", intent.getAction());
            switch (intent.getAction()) {
                // device_posture
                /*
                case Rotation.ACTION_AWARE_ROTATION:
                    if (intent.hasExtra(Rotation.EXTRA_DATA)) {
                        ContentValues c = intent.getParcelableExtra(Rotation.EXTRA_DATA);
                        device_posture = calculatePosture(c);
                    }
                    break;
                    */
                // network_type

                case Network.ACTION_AWARE_INTERNET_AVAILABLE:
                    internet_available = new Tuple(System.currentTimeMillis(), 1);
                    calculateNetworkType();
                    break;
                case Network.ACTION_AWARE_INTERNET_UNAVAILABLE:
                    internet_available = new Tuple(System.currentTimeMillis(), 0);
                    calculateNetworkType();
                    break;
                case Network.ACTION_AWARE_WIFI_ON:
                    wifi_available = new Tuple(System.currentTimeMillis(), 1);
                    calculateNetworkType();
                    break;
                case Network.ACTION_AWARE_WIFI_OFF:
                    wifi_available = new Tuple(System.currentTimeMillis(), 0);
                    calculateNetworkType();
                    break;
                // foreground_app
                case ApplicationMonitor.NEW_FOREGROUND:
                    foreground_app = new Tuple(System.currentTimeMillis(), intent.getStringExtra("app_name"));
                    foreground_package = new Tuple(System.currentTimeMillis(), intent.getStringExtra("package_name"));
                    //getAppCategory(intent.getStringExtra("app_name"), intent.getStringExtra("package_name"));
                    break;
                case Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND:
                    Log.d("Application", "changed");
                    getForegroundApp();
                    break;
                // time since last call
                case Communication.ACTION_AWARE_CALL_ACCEPTED:
                    last_call = new Tuple(System.currentTimeMillis(), System.currentTimeMillis());
                    break;
                // battery level
                case Battery.ACTION_AWARE_BATTERY_CHANGED:
                    setBatteryLevel();
                    break;
                case Battery.ACTION_AWARE_BATTERY_CHARGING_USB:
                    battery_level = new Tuple(System.currentTimeMillis(), 1);
                    battery_charging = new Tuple(System.currentTimeMillis(), 1);
                    break;
                case Battery.ACTION_AWARE_BATTERY_CHARGING_AC:
                    battery_level = new Tuple(System.currentTimeMillis(), 1);
                    battery_charging = new Tuple(System.currentTimeMillis(), 1);
                    break;
                case Battery.ACTION_AWARE_BATTERY_DISCHARGING:
                    battery_charging = new Tuple(System.currentTimeMillis(), 0);
                    break;
                case Proximity.ACTION_AWARE_PROXIMITY:
                    //Log.d("Proximity", "changed");
                    if (intent.hasExtra(Proximity.EXTRA_DATA)) {
                        ContentValues c = intent.getParcelableExtra(Proximity.EXTRA_DATA);
                        setProximity(c);
                    }
                    break;
                case "ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION":
                    if (intent.getIntExtra("confidence", 0) > 60 && intent.getIntExtra("activity", -1) > -1) {
                        activity = new Tuple(System.currentTimeMillis(), intent.getIntExtra("activity", -1));
                    }
                default:
                    break;
            }
        }

    }

    private static void setActivity(int a) {
        activity = new Tuple(System.currentTimeMillis(), a);
    }

    private void setBatteryLevel() {
        try {
            Cursor battery_data = getContentResolver().query(Battery_Provider.Battery_Data.CONTENT_URI, new String[]{"battery_level"}, null, null, "timestamp DESC LIMIT 1");
            battery_data.moveToFirst();
            battery_level = new Tuple(System.currentTimeMillis(), Integer.valueOf(battery_data.getString(battery_data.getColumnIndex("battery_level"))));
            battery_data.close();
        }
        catch (CursorIndexOutOfBoundsException e) {
            Log.d("setBatteryLevel", "Failed:" + e.getMessage());
        }
        catch (NullPointerException e2) {
            StringWriter sw = new StringWriter();
            e2.printStackTrace(new PrintWriter(sw));
            AnalyticsApplication.sendEvent("crash", "setBatteryLevel()", sw.toString(), null);
        }
    }

    private void getForegroundApp() {
        Log.d("Application", "getForegroundApp()");
        try {
            Cursor app_data = getContentResolver().query(Applications_Provider.Applications_Foreground.CONTENT_URI, new String[]{"package_name"}, null, null, "timestamp DESC LIMIT 1");
            app_data.moveToFirst();
            foreground_app = new Tuple(System.currentTimeMillis(), app_data.getString(app_data.getColumnIndex("package_name")));
            foreground_package = new Tuple(System.currentTimeMillis(), app_data.getString(app_data.getColumnIndex("package_name")));
            //Log.d("Application", "fore, package: " + foreground_app + foreground_package);
            //getAppCategory(foreground_app.value, foreground_package.value);
            app_data.close();
        }
        catch (CursorIndexOutOfBoundsException e) {
            Log.d("Application", "Failed:" + e.getMessage());
        }
        catch (NullPointerException e2) {
            StringWriter sw = new StringWriter();
            e2.printStackTrace(new PrintWriter(sw));
            AnalyticsApplication.sendEvent("crash", "getForegroundApp()", sw.toString(), null);
        }
    }

    private void getAppCategory(final String appName, final String packageName) {
        Log.d("Application", "getAppCategory()");
        MarketSession session = new MarketSession();
        //session.login(username, password);
        session.getContext().setAndroidId("myid");

        Market.AppsRequest req = Market.AppsRequest.newBuilder()
                .setQuery(packageName)
                .setStartIndex(0).setEntriesCount(0)
                .setWithExtendedInfo(true)
                .build();

        session.append(req, new MarketSession.Callback<Market.AppsResponse>() {
            @Override
            public void onResult(Market.ResponseContext responseContext, Market.AppsResponse response) {
                foreground_app_category = new Tuple(System.currentTimeMillis(), response.getApp(0).getExtendedInfo().getCategory());
                Log.d("app category", foreground_app_category.value);
            }
        });
        session.flush();
    }

    private void setProximity(ContentValues c) {
        if (c.getAsDouble("double_proximity") > 0) proximity = new Tuple(System.currentTimeMillis(), 1);
        else proximity = new Tuple(System.currentTimeMillis(), 0);

    }

    private void calculateNetworkType() {
        if ((wifi_available.value < 1) && (internet_available.value < 1)) network_type = new Tuple(System.currentTimeMillis(), 0);
        else if ((wifi_available.value > 0) && (internet_available.value < 1)) network_type = new Tuple(System.currentTimeMillis(), 1);
        else if (wifi_available.value < 1) network_type = new Tuple(System.currentTimeMillis(), 2);
        else network_type = new Tuple(System.currentTimeMillis(), 3);
    }

    // allow only one instance
    static ContextReceiver co;

    @Override
    public void onCreate() {}

    // set system wide alarm to restart service if it has been shutdown
    final static int restartAlarmInterval = 20*60*1000;
    final static int resetAlarmTimer = 5*60*1000;
    public void setServiceKeepAlive() {
        final AlarmManager alarmMgr = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        final Intent restartIntent = new Intent(getApplicationContext(), UserContextService.class);
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
            PendingIntent pintent = PendingIntent.getService(applicationContext, 0, restartIntent, 0);
            // set restart reminder to 8 in the morning if in night hours (00:00 - 08:00)
            if (cal.get(Calendar.HOUR_OF_DAY) < 8) {
                cal.set(Calendar.HOUR_OF_DAY, 8);
                alarmMgr.set(AlarmManager.ELAPSED_REALTIME, cal.getTimeInMillis() - System.currentTimeMillis(), pintent);
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

        if (co != null) unregisterReceiver(co);

        Log.d("ContextService", "Started");

        Aware.startBattery(this);
        Aware.startCommunication(this);
        Aware.startApplications(this);
        Aware.startProximity(this);
        Aware.startNetwork(this);

        Aware.startPlugin(this, "com.aware.plugin.google.activity_recognition");

        co = new ContextReceiver();
        IntentFilter i = new IntentFilter();

        //i.addAction(Rotation.ACTION_AWARE_ROTATION);

        i.addAction(Network.ACTION_AWARE_INTERNET_AVAILABLE);
        i.addAction(Network.ACTION_AWARE_INTERNET_UNAVAILABLE);
        i.addAction(Network.ACTION_AWARE_WIFI_ON);
        i.addAction(Network.ACTION_AWARE_WIFI_OFF);

        //i.addAction(Applications.ACTION_AWARE_APPLICATIONS_HISTORY);
        //i.addAction(Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND);
        i.addAction(ApplicationMonitor.NEW_FOREGROUND);

        i.addAction(Proximity.ACTION_AWARE_PROXIMITY);

        i.addAction(Battery.ACTION_AWARE_BATTERY_CHANGED);
        i.addAction(Battery.ACTION_AWARE_BATTERY_CHARGING_AC);
        i.addAction(Battery.ACTION_AWARE_BATTERY_CHARGING_USB);
        i.addAction(Battery.ACTION_AWARE_BATTERY_DISCHARGING);

        i.addAction(Communication.ACTION_AWARE_USER_IN_CALL);
        i.addAction(Communication.ACTION_AWARE_USER_NOT_IN_CALL);
        i.addAction(Communication.ACTION_AWARE_CALL_ACCEPTED);

        i.addAction(Plugin.ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION);

        registerReceiver(co, i);

        setTimes();

        ApplicationMonitor.isAccessibilityServiceActive(this);

        setServiceKeepAlive();
        IS_RUNNING = true;

        return START_STICKY;
    }

    Calendar cal;
    private void setTimes() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                cal = Calendar.getInstance();
                hour = new Tuple(System.currentTimeMillis(), cal.get(Calendar.HOUR_OF_DAY));
                minute = new Tuple(System.currentTimeMillis(), cal.get(Calendar.MINUTE));
                day_of_week = new Tuple(System.currentTimeMillis(),cal.get(Calendar.DAY_OF_WEEK));
                setTimes();
            }
        }, 60000);
    }

    @Override
    protected void onHandleIntent(Intent intent) {}

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        IS_RUNNING = false;
        Intent restartService = new Intent(getApplicationContext(),
                UserContextService.class);
        restartService.setPackage(getPackageName());
        PendingIntent restartServicePI = PendingIntent.getService(
                getApplicationContext(), 1, restartService,
                PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() +1000, restartServicePI);
    }

    @Override
    public void onDestroy() {
        IS_RUNNING = false;

        Aware.stopApplications(this);
        Aware.stopBattery(this);
        Aware.stopCommunication(this);
        Aware.stopNetwork(this);
        Aware.stopProximity(this);

        unregisterReceiver(co);

        Aware.stopPlugin(this, "com.aware.plugin.google.activity_recognition");
    }

    private static JSONObject userContext;
    private static void generateJson() throws JSONException {
        try {
            Long curTime = System.currentTimeMillis();
            Calendar cal = Calendar.getInstance();
            hour = new Tuple(System.currentTimeMillis(), cal.get(Calendar.HOUR_OF_DAY));
            minute = new Tuple(System.currentTimeMillis(), cal.get(Calendar.MINUTE));
            day_of_week = new Tuple(System.currentTimeMillis(), cal.get(Calendar.DAY_OF_WEEK));
            userContext = new JSONObject();
            userContext.put("timestamp", curTime);
            // no values over 1800000ms (30minutes) long accepted
            if (hour != null && (hour.time - curTime < 1800000)) {
                userContext.put("hour", hour.value);
            } else {
                userContext.put("hour", -1);
            }
            if (minute != null && (minute.time - curTime < 1800000)) {
                userContext.put("minute", minute.value);
            } else {
                userContext.put("minute", -1);
            }
            if (day_of_week != null && (day_of_week.time - curTime < 1800000)) {
                userContext.put("day_of_week", day_of_week.value);
            } else {
                userContext.put("day_of_week", -1);
            }
        /*
        if (device_posture != null) {
            userContext.put("device_posture", device_posture);
        }
        */
            if (battery_level != null && (battery_level.time - curTime < 1800000)) {
                userContext.put("battery_level", battery_level.value);
            } else {
                userContext.put("battery_level", -1);
            }

            if (battery_charging != null && (battery_charging.time - curTime < 1800000)) {
                userContext.put("battery_charging", battery_charging.value);
            } else {
                userContext.put("battery_charging", -1);
            }

        /*
        if (foreground_app != null && (foreground_app.time - curTime < 300000)) {
            userContext.put("foreground_app", foreground_app);
        } else {userContext.put("foreground_app", -1);}
        */
            if (foreground_package != null && (foreground_package.time - curTime < 1800000)) {
                userContext.put("foreground_package", AppPreferences.getApplicationIndex(foreground_package.value));
            } else {
                userContext.put("foreground_package", -1);
            }

        /*
        if (foreground_app_category != null && (foreground_app_category.time - curTime < 300000)) {
            userContext.put("foreground_app_category", foreground_app_category);
        } else {userContext.put("foreground_app_category", -1);}
        */

            if (proximity != null && (proximity.time - curTime < 1800000)) {
                userContext.put("proximity", proximity.value);
            } else {
                userContext.put("proximity", -1);
            }

            if (last_call != null && (last_call.time - curTime < 1800000)) {
                userContext.put("last_call", last_call.value);
            } else {
                userContext.put("last_call", -1);
            }

            if (internet_available != null && (internet_available.time - curTime < 1800000)) {
                userContext.put("internet_available", internet_available.value);
            } else {
                userContext.put("internet_available", -1);
            }

            if (wifi_available != null && (wifi_available.time - curTime < 1800000)) {
                userContext.put("wifi_available", wifi_available.value);
            } else {
                userContext.put("wifi_available", -1);
            }

            if (network_type != null && (network_type.time - curTime < 1800000)) {
                userContext.put("network_type", network_type.value);
            } else {
                userContext.put("network_type", -1);
            }

            if (last_action != null && (last_action.time - curTime < 1800000)) {
                userContext.put("last_action", last_action.value);
            } else {
                userContext.put("last_action", -1);
            }

            if (activity != null && (activity.time - curTime < 1800000)) {
                userContext.put("activity", activity.value);
            } else {
                userContext.put("activity", -1);
            }
        }
        catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            AnalyticsApplication.sendEvent("crash", "getContextJson()", sw.toString(), null);
        }
    }

    public static JSONObject getUserContext() {
        try {
            generateJson();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
        return userContext;
    }

    public static String getUserContextString() {
        try {
            generateJson();
        }
        catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject().toString();
        }
        return userContext.toString();
    }

}
