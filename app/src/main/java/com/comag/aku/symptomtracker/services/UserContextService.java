package com.comag.aku.symptomtracker.services;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.aware.Applications;
import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Battery;
import com.aware.Communication;
import com.aware.Gyroscope;
import com.aware.Network;
import com.aware.Proximity;
import com.aware.Rotation;
import com.aware.providers.Gyroscope_Provider;
import com.aware.utils.Aware_Sensor;

import java.util.Calendar;

/**
 * Created by aku on 11/12/15.
 */
public class UserContextService extends IntentService {

    static int hour;
    static int minute;
    static int day_of_week;
    static int device_posture;
    static int postureBuffer = 0;
    static int battery_level;
    static int battery_charging;
    static String foreground_app;
    static int proximity;
    static long last_call;
    static boolean internet_available = false;
    static boolean wifi_available = false;
    static int network_type;

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
    */
    public UserContextService() {
        super("UserContextService");
    }

    private class ContextReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                // device_posture
                case Rotation.ACTION_AWARE_ROTATION:
                    if (intent.hasExtra(Rotation.EXTRA_DATA)) {
                        ContentValues c = intent.getParcelableExtra(Rotation.EXTRA_DATA);
                        device_posture = calculatePosture(c);
                    }
                    break;
                // network_type
                case Network.ACTION_AWARE_INTERNET_AVAILABLE:
                    internet_available = true;
                    calculateNetworkType();
                    break;
                case Network.ACTION_AWARE_INTERNET_UNAVAILABLE:
                    internet_available = false;
                    calculateNetworkType();
                    break;
                case Network.ACTION_AWARE_WIFI_ON:
                    wifi_available = true;
                    calculateNetworkType();
                    break;
                case Network.ACTION_AWARE_WIFI_OFF:
                    wifi_available = false;
                    calculateNetworkType();
                    break;
                // foreground_app
                case Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND:
                    Log.d("foregroundapp", "changed");
                    foreground_app = "new";
                    break;
                // time since last call
                case Communication.ACTION_AWARE_CALL_ACCEPTED:
                    last_call = System.currentTimeMillis();
                    break;
                // battery level
                case Battery.ACTION_AWARE_BATTERY_FULL:
                    battery_level = 1;
                    break;
                case Battery.ACTION_AWARE_BATTERY_LOW:
                    battery_level = 0;
                    break;
                case Battery.ACTION_AWARE_BATTERY_CHARGING_USB:
                    battery_level = 1;
                    battery_charging = 1;
                    break;
                case Battery.ACTION_AWARE_BATTERY_CHARGING_AC:
                    battery_level = 1;
                    battery_charging = 1;
                    break;
                case Battery.ACTION_AWARE_BATTERY_DISCHARGING:
                    battery_charging = 0;
                    break;
                case Proximity.ACTION_AWARE_PROXIMITY:
                    if (intent.hasExtra(Proximity.EXTRA_DATA)) {
                        ContentValues c = intent.getParcelableExtra(Proximity.EXTRA_DATA);
                        setProximity(c);
                    }
                    break;
                default:
                    break;
            }
        }

    }

    private void setProximity(ContentValues c) {
        if (c.getAsDouble("double_proximity") > 0) proximity = 1;
        else proximity = 0;
    }

    private void calculateNetworkType() {
        if (!wifi_available && !internet_available) network_type = 0;
        else if (wifi_available && !internet_available) network_type = 1;
        else if (!wifi_available) network_type = 2;
        else network_type = 3;
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
    public void onCreate() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Aware.setSetting(this, Aware_Preferences.STATUS_ROTATION, true);
        Aware.setSetting(this, Aware_Preferences.STATUS_BATTERY, true);
        Aware.setSetting(this, Aware_Preferences.STATUS_CALLS, true);
        Aware.setSetting(this, Aware_Preferences.STATUS_NETWORK_EVENTS, true);
        Aware.setSetting(this, Aware_Preferences.STATUS_APPLICATIONS, true);
        Aware.setSetting(this, Aware_Preferences.STATUS_PROXIMITY, true);

        // only poll once per second
        Aware.setSetting(this, Aware_Preferences.FREQUENCY_ROTATION, 200000);

        co = new ContextReceiver();
        IntentFilter i = new IntentFilter();

        i.addAction(Rotation.ACTION_AWARE_ROTATION);
        i.addAction(Network.ACTION_AWARE_INTERNET_AVAILABLE);
        i.addAction(Network.ACTION_AWARE_INTERNET_UNAVAILABLE);
        i.addAction(Network.ACTION_AWARE_WIFI_ON);
        i.addAction(Network.ACTION_AWARE_WIFI_OFF);
        i.addAction(Applications.ACTION_AWARE_APPLICATIONS_FOREGROUND);
        i.addAction(Proximity.ACTION_AWARE_PROXIMITY);

        registerReceiver(co, i);

        Intent aware = new Intent(this, Aware.class);
        startService(aware);

        setTimes();

        return START_STICKY;
    }

    Calendar cal;
    private void setTimes() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                cal = Calendar.getInstance();
                hour = cal.get(Calendar.HOUR_OF_DAY);
                minute = cal.get(Calendar.MINUTE);
                day_of_week = cal.get(Calendar.DAY_OF_WEEK);
                setTimes();
            }
        }, 60000);
    }

    @Override
    protected void onHandleIntent(Intent intent) {}

    @Override
    public void onDestroy() {
        Aware.setSetting(this, Aware_Preferences.STATUS_ROTATION, false);
        Aware.setSetting(this, Aware_Preferences.STATUS_BATTERY, false);
        Aware.setSetting(this, Aware_Preferences.STATUS_CALLS, false);
        Aware.setSetting(this, Aware_Preferences.STATUS_NETWORK_EVENTS, false);
        Aware.setSetting(this, Aware_Preferences.STATUS_APPLICATIONS, false);

        unregisterReceiver(co);

        Intent aware = new Intent(this, Aware.class);
        startService(aware);
    }

    public static String getUserContext() {
        return "{}";
    }
}
