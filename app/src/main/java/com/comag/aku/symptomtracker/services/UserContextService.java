package com.comag.aku.symptomtracker.services;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.os.Handler;
import android.util.Log;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Battery;
import com.aware.Communication;
import com.aware.Network;
import com.aware.Proximity;
import com.aware.Rotation;
import com.aware.plugin.google.activity_recognition.Plugin;
import com.aware.providers.Applications_Provider;
import com.aware.providers.Battery_Provider;
import com.comag.aku.symptomtracker.AppHelpers;
import com.comag.aku.symptomtracker.app_settings.AppPreferences;
import com.comag.aku.symptomtracker.data_syncronization.SyncProvider;
import com.comag.aku.symptomtracker.services.smart_notifications.SmartNotificationEngine;
import com.gc.android.market.api.MarketSession;
import com.gc.android.market.api.model.Market;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by aku on 11/12/15.
 */
public class UserContextService extends IntentService {

    public static final List<String> required = Arrays.asList(
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
    private static String username = "stracker2015@gmail.com";
    private static String password = "eGzFA9XmKHNvGfT2";

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
                    //Log.d("Application", "changed");
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
                case Plugin.ACTION_AWARE_GOOGLE_ACTIVITY_RECOGNITION:
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
    }

    private void getForegroundApp() {
        Log.d("Application", "getForegroundApp()");
        try {
            Cursor app_data = getContentResolver().query(Applications_Provider.Applications_Foreground.CONTENT_URI, new String[]{"package_name"}, null, null, "timestamp DESC LIMIT 1");
            app_data.moveToFirst();
            foreground_app = new Tuple(System.currentTimeMillis(), app_data.getString(app_data.getColumnIndex("package_name")));
            foreground_package = new Tuple(System.currentTimeMillis(), app_data.getString(app_data.getColumnIndex("package_name")));
            //Log.d("Application", "fore, package: " + foreground_app + foreground_package);
            getAppCategory(foreground_app.value, foreground_package.value);
            app_data.close();
        }
        catch (CursorIndexOutOfBoundsException e) {
            Log.d("Application", "Failed:" + e.getMessage());
        }
    }

    private void getAppCategory(final String appName, final String packageName) {
        Log.d("Application", "getAppCategory()");
        MarketSession session = new MarketSession();
        session.login(username, password);
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

    private int calculatePosture(ContentValues c) {
        // if tilting positively and in an angle between 20 and 80
        // the device is in a normal hand-held position
        if ((c.getAsDouble("double_values_1") * c.getAsDouble("double_values_2")) > 0 &&
                (0.20 < Math.abs(c.getAsDouble("double_values_1")) && Math.abs(c.getAsDouble("double_values_1")) < 0.40)) {
            postureBuffer = 0;
            return 1;
        }
        // because of the variance in the sensor values we want to verify the
        // position based on a 10-point buffer
        else {
            postureBuffer++;
            if (postureBuffer > 10) {
                return 0;
            }
            else return 1;
        }
    }

    ContextReceiver co;

    @Override
    public void onCreate() {}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent aware = new Intent(this, Aware.class);
        startService(aware);

        Log.d("ContextService", "Started");

        Aware.setSetting(this, Aware_Preferences.STATUS_ROTATION, false);
        Aware.setSetting(this, Aware_Preferences.STATUS_BATTERY, true);
        Aware.setSetting(this, Aware_Preferences.STATUS_CALLS, true);
        Aware.setSetting(this, Aware_Preferences.STATUS_NETWORK_EVENTS, true);
        Aware.setSetting(this, Aware_Preferences.STATUS_APPLICATIONS, true);
        Aware.setSetting(this, Aware_Preferences.STATUS_PROXIMITY, true);
        Aware.setSetting(this, Aware_Preferences.STATUS_BATTERY, true);
        Aware.setSetting(this, "com.aware.plugin.google.activity_recognition", true);

        //Aware.setSetting(this, Aware_Preferences.FREQUENCY_ROTATION, 60000);

        //Aware.startSensor(this, Aware_Preferences.STATUS_ROTATION);
        Aware.startSensor(this, Aware_Preferences.STATUS_BATTERY);
        Aware.startSensor(this, Aware_Preferences.STATUS_CALLS);
        Aware.startSensor(this, Aware_Preferences.STATUS_NETWORK_EVENTS);
        Aware.startSensor(this, Aware_Preferences.STATUS_APPLICATIONS);
        Aware.startSensor(this, Aware_Preferences.STATUS_PROXIMITY);

        Aware.startPlugin(this, "com.aware.plugin.google.activity_recognition");

        // only poll once per second
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

        //ApplicationMonitor.isAccessibilityServiceActive(this);

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
    public void onDestroy() {
        Aware.stopSensor(this, Aware_Preferences.STATUS_ROTATION);
        Aware.stopSensor(this, Aware_Preferences.STATUS_BATTERY);
        Aware.stopSensor(this, Aware_Preferences.STATUS_CALLS);
        Aware.stopSensor(this, Aware_Preferences.STATUS_NETWORK_EVENTS);
        //Aware.setSetting(this, Aware_Preferences.STATUS_APPLICATIONS, false);

        unregisterReceiver(co);

        Aware.stopPlugin(this, "com.aware.plugin.google.activity_recognition");
    }

    private static JSONObject userContext;
    private static void generateJson() throws JSONException {
        Long curTime = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        hour = new Tuple(System.currentTimeMillis(), cal.get(Calendar.HOUR_OF_DAY));
        minute = new Tuple(System.currentTimeMillis(), cal.get(Calendar.MINUTE));
        day_of_week = new Tuple(System.currentTimeMillis(),cal.get(Calendar.DAY_OF_WEEK));
        userContext = new JSONObject();
        // no values over 1800000ms (30minutes) long accepted
        if (hour != null && (hour.time - curTime < 1800000)) {
            userContext.put("hour", hour.value);
        } else {userContext.put("hour", -1);}
        if (minute != null && (minute.time - curTime < 1800000)) {
            userContext.put("minute", minute.value);
        } else {userContext.put("minute", -1);}
        if (day_of_week != null && (day_of_week.time - curTime < 1800000)) {
            userContext.put("day", day_of_week.value);
        } else {userContext.put("day", -1);}
        /*
        if (device_posture != null) {
            userContext.put("device_posture", device_posture);
        }
        */
        if (battery_level != null && (battery_level.time - curTime < 1800000)) {
            userContext.put("battery_level", battery_level.value);
        } else {userContext.put("battery_level", -1);}

        if (battery_charging != null && (battery_charging.time - curTime < 1800000)) {
            userContext.put("battery_charging", battery_charging.value);
        } else {userContext.put("battery_charging", -1);}

        /*
        if (foreground_app != null && (foreground_app.time - curTime < 300000)) {
            userContext.put("foreground_app", foreground_app);
        } else {userContext.put("foreground_app", -1);}
        */
        if (foreground_package != null && (foreground_package.time - curTime < 1800000)) {
            userContext.put("foreground_package", AppPreferences.getApplicationIndex(foreground_package.value));
        } else {userContext.put("foreground_package", -1);}

        /*
        if (foreground_app_category != null && (foreground_app_category.time - curTime < 300000)) {
            userContext.put("foreground_app_category", foreground_app_category);
        } else {userContext.put("foreground_app_category", -1);}
        */

        if (proximity != null && (proximity.time - curTime < 1800000)) {
            userContext.put("proximity", proximity.value);
        } else {userContext.put("proximity", -1);}

        if (last_call != null && (last_call.time - curTime < 1800000)) {
            userContext.put("last_call", last_call.value);
        } else {userContext.put("last_call", -1);}

        if (internet_available != null && (internet_available.time - curTime < 1800000)) {
            userContext.put("internet_available", internet_available.value);
        } else {userContext.put("internet_available", -1);}

        if (wifi_available != null && (wifi_available.time - curTime < 1800000)) {
            userContext.put("wifi_available", wifi_available.value);
        } else {userContext.put("wifi_available", -1);}

        if (network_type != null && (network_type.time - curTime < 1800000)) {
            userContext.put("network_type", network_type.value);
        } else {userContext.put("network_type", -1);}

        if (last_action != null && (last_action.time - curTime < 1800000)) {
            userContext.put("last_action", last_action.value);
        } else {userContext.put("last_action", -1);}

        if (activity != null && (activity.time - curTime < 1800000)) {
            userContext.put("activity", activity.value);
        } else {userContext.put("activity", -1);}

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
