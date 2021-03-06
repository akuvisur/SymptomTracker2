package com.comag.aku.lifetracker.app_settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.aware.Aware;
import com.comag.aku.lifetracker.AppHelpers;
import com.comag.aku.lifetracker.model.ApiManager;
import com.comag.aku.lifetracker.model.NoSQLStorage;
import com.comag.aku.lifetracker.objects.Factor;
import com.comag.aku.lifetracker.objects.Schema;
import com.comag.aku.lifetracker.objects.Symptom;
import com.comag.aku.lifetracker.services.NotificationService;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by aku on 30/10/15.
 */
public class AppPreferences {

    final public static String NOTIFICATION_HOUR = "notification_hour";
    final public static String POPUP_INTERVAL = "popup_interval";
    final public static String POPUP_AUTOMATED = "popup_automated";
    final public static String POPUP_FREQUENCY = "popup_freq";
    final public static String DATASYNC_ENABLED = "datasync";
    final public static String USER_ID = "user_id";

    private static Gson gson = new Gson();
    private static SharedPreferences sharedPrefs;
    public static Schema schema;
    public static HashMap<String, Symptom> symptoms = new HashMap<>();
    public static HashMap<String, Symptom> generatedSymptoms = new HashMap<>();
    public static TreeMap<String, Factor> factors = new TreeMap<>();

    public static UserSettings userSettings = new UserSettings();

    // hardcoded ML participant listing
    public static void setNotificationMode(boolean smart) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putBoolean("smart_notification", true);
        editor.commit();
        /*
        if (userSettings.getUserId() != null) {
            Integer uId = Integer.valueOf(userSettings.getUserId());
            if (mlParticipants.contains(uId)) {
                SharedPreferences.Editor editor = sharedPrefs.edit();
                //editor.putBoolean("smart_notification", smart);
                editor.putBoolean("smart_notification", true);
                editor.commit();
            }
            else {
                SharedPreferences.Editor editor = sharedPrefs.edit();
                //editor.putBoolean("smart_notification", smart);
                editor.putBoolean("smart_notification", false);
                editor.commit();
            }
        }
        else {
            load();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    setNotificationMode(true);
                }
            }, 5000);
        }
        */
    }

    public static NotificationService.NotificationMode getNotificationMode() {
        return NotificationService.NotificationMode.LEARNING_MODE;

        /*try {
            if (userSettings != null && userSettings.getUserId() != null) {
                Integer uId = Integer.valueOf(userSettings.getUserId());
                if (mlParticipants.contains(uId)) {
                    return NotificationService.NotificationMode.LEARNING_MODE;
                } else {
                    return NotificationService.NotificationMode.DUMMY_MODE;
                }
            }
            else {
                load();

                    Integer uId = Integer.valueOf(userSettings.getUserId());
                    if (mlParticipants.contains(uId)) {
                        return NotificationService.NotificationMode.LEARNING_MODE;
                    } else {
                        return NotificationService.NotificationMode.DUMMY_MODE;
                    }
                }

        }
        catch (NullPointerException e) {
            Log.d("AppPrefs", "Could not fetch user_id");
            return NotificationService.NotificationMode.UNDEFINED;
        }
        catch (NumberFormatException e) {
            Log.d("AppPrefs", "undefined user id");
            return NotificationService.NotificationMode.UNDEFINED;
        }
        try {
            init();
            Boolean b = sharedPrefs.getBoolean("smart_notification", false);
            if (b) return NotificationService.NotificationMode.LEARNING_MODE;
            else return NotificationService.NotificationMode.DUMMY_MODE;
        }
        catch (NullPointerException e) {
        }
        */
    }

    private static void init() {
        String myTrackedData = "TrackingData";
        if (AppHelpers.currentContext != null) {
            if (sharedPrefs == null)
                sharedPrefs = AppHelpers.currentContext.getSharedPreferences(myTrackedData, Context.MODE_PRIVATE);
        }
        else {
            if (sharedPrefs == null)
                sharedPrefs = NotificationService.getContext().getSharedPreferences(myTrackedData, Context.MODE_PRIVATE);
        }
    }

    public static Schema getSchema() {
        if (!hasLoaded()) load();
        return schema;
    }

    public static void join(final Schema s) {
        Intent aware = new Intent(AppHelpers.currentContext, Aware.class);
        AppHelpers.currentContext.startService(aware);

        try {
            init();
        }
        catch (NullPointerException e) {
            return;
        }
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString("schema", gson.toJson(s.json));
        Toast.makeText(AppHelpers.currentActivity, "joined " + s.title, Toast.LENGTH_SHORT).show();
        editor.apply();

        if (s.aware_study_url != null) {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d("study_join", "from delayed join..");
                    Intent join = new Intent(AppHelpers.currentContext, Aware.JoinStudy.class);
                    join.putExtra("study_url", s.aware_study_url);
                    AppHelpers.currentContext.startService(join);
                    //Aware.joinStudy(AppHelpers.currentContext, s.aware_study_url);
                    AppPreferences.setUserSetting(DATASYNC_ENABLED, true);
                }
            }, 30000);
        }

        schema = s;

        if (AppHelpers.DEBUG) NoSQLStorage.clear(schema.db_name);
    }

    public static void addUserSymptom(Symptom s) {
        Log.d("prefs", "adding user generated symptom: " + s.toString());
        generatedSymptoms.put(s.key, s);
        symptoms.put(s.key, s);
        storeSymptomsToSharedPrefs();
    }

    public static void storeSymptomsToSharedPrefs() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString("symptoms", gson.toJson(AppPreferences.symptoms));
        editor.putString("generated", gson.toJson(AppPreferences.generatedSymptoms));

        editor.apply();
    }

    public static void addUsedApplication(String appName, String appPackage, String appCategory) {
        try {
            init();
        }
        catch (NullPointerException e) {
            return;
        }
        //Log.d("add_app", appPackage);
        int i = 0;
        try {
            JSONObject apps = new JSONObject(sharedPrefs.getString("apps", "{}"));
            Iterator<String> keys = apps.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                //Log.d("add_app", key + ": " + apps.get(key));
                if (key.equals(appPackage)) return;
                else if (apps.getInt(key) > i) i = apps.getInt(key);
            }
            i++;
            SharedPreferences.Editor editor = sharedPrefs.edit();
            apps.put(appPackage, i);
            editor.putString("apps", apps.toString());
            editor.apply();
        }
        catch (JSONException e) {}
    }

    public static int getApplicationIndex(String appPackage) {
        init();
        try {
            JSONObject apps = new JSONObject(sharedPrefs.getString("apps", "{}"));
            Iterator<String> keys = apps.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                //Log.d("app_index", key + ": " + apps.get(key));
                if (key.equals(appPackage)) return apps.getInt(key);
            }
        }
        catch (JSONException e) {}
        // not found
        return -1;
    }

    public static void addFactors() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putString("factors", gson.toJson(AppPreferences.factors));
        editor.apply();
    }

    public static boolean hasLoaded() {
        return !(schema == null);
    }

    public static boolean load() {
        init();
        String s = sharedPrefs.getString("schema", null);
        if (s == null) {
            return false;
        }
        else {
            //Log.d("schema", sharedPrefs.getString("schema", "{}"));
            schema = gson.fromJson(sharedPrefs.getString("schema", "{}"), Schema.class);

            // use normal JSONObject because it has a constructor directly from a string
            try {
                JSONObject o = new JSONObject(sharedPrefs.getString("symptoms", "{}"));
                Iterator<String> keys = o.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    //Log.d("loading", "normal symptoms " + o.getJSONObject(key));
                    // use the gson serializer to generate a JsonObject for Symptom constructor
                    symptoms.put(o.getJSONObject(key).getString("key"), gson.fromJson(o.getJSONObject(key).toString(), Symptom.class));
                }
            }
            catch (JSONException e) {e.printStackTrace();}

            try {
                JSONObject o = new JSONObject(sharedPrefs.getString("generated", "{}"));
                Iterator<String> keys = o.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    //Log.d("loading", "generated symptoms " + o.getJSONObject(key));
                    // use the gson serializer to generate a JsonObject for Symptom constructor
                    symptoms.put(o.getJSONObject(key).getString("key"), gson.fromJson(o.getJSONObject(key).toString(), Symptom.class));
                    generatedSymptoms.put(o.getJSONObject(key).getString("key"), gson.fromJson(o.getJSONObject(key).toString(), Symptom.class));
                }
            }
            catch (JSONException e) {e.printStackTrace();}

            try {
                JSONObject o = new JSONObject(sharedPrefs.getString("factors", "{}"));
                Iterator<String> keys = o.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    // use the gson serializer to generate a JsonObject for Symptom constructor
                    factors.put(o.getJSONObject(key).getString("key"), gson.fromJson(o.getJSONObject(key).getString("json"), Factor.class));
                    //Log.d("prefs", "added to factors");
                }
            }
            catch (JSONException e) {e.printStackTrace();}
        }
        if (symptoms.isEmpty()) {
            ApiManager.getSymptomsForSchema();
        }

        userSettings.setNotificationHour(sharedPrefs.getInt(NOTIFICATION_HOUR, 18));
        userSettings.setPopupFrequency(sharedPrefs.getInt(POPUP_FREQUENCY, 50));
        userSettings.setPopupsAutomated(sharedPrefs.getBoolean(POPUP_AUTOMATED, true));
        userSettings.setPopupInterval(sharedPrefs.getInt(POPUP_INTERVAL, NotificationService.NOTIFICATION_DELAY_MS));
        userSettings.setUserId(sharedPrefs.getString(USER_ID, ""));

        setNotificationMode(true);

        return true;
    }

    public static void setUserSetting(String setting, Object value) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        switch (setting) {
            case NOTIFICATION_HOUR:
                int hour = (int) value;
                editor.putInt(setting, hour);
                userSettings.setNotificationHour(hour);
                break;
            case POPUP_FREQUENCY:
                int freq = (int) value;
                editor.putInt(setting, freq);
                userSettings.setPopupFrequency(freq);
                break;
            case POPUP_AUTOMATED:
                boolean automated = (boolean) value;
                editor.putBoolean(setting, automated);
                userSettings.setPopupsAutomated(automated);
                break;
            case POPUP_INTERVAL:
                int interval = (int) value;
                editor.putInt(setting, interval);
                userSettings.setPopupInterval(interval);
                break;
            case USER_ID:
                String id = (String) value;
                editor.putString(USER_ID, id);
                userSettings.setUserId(id);
                break;
            case DATASYNC_ENABLED:
                boolean enabled = (boolean) value;
                editor.putBoolean(DATASYNC_ENABLED, enabled);
                userSettings.setDataSync(enabled);
                break;
            default:
                break;
        }

        editor.apply();
    }

    public static List<Symptom> symptomsAsList() {
        List<Symptom> result = new ArrayList<>();
        for (Symptom s : symptoms.values()) {result.add(s);}
        return result;
    }

    public static List<Factor> factorsAsList() {
        List<Factor> result = new ArrayList<>();
        for (Factor f : factors.values()) {result.add(f);}
        return result;
    }

    public static void clear() {
        init();
        NoSQLStorage.clear(schema.db_name);

        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.clear();
        editor.apply();

        schema = null;

    }

    public static boolean appIsSetUp() {
        return load();
    }
}
