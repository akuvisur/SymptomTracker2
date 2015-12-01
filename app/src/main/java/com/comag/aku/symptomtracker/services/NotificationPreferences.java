package com.comag.aku.symptomtracker.services;

import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.comag.aku.symptomtracker.Settings;
import com.comag.aku.symptomtracker.model.NoSQLStorage;

import java.util.HashMap;

/**
 * Created by aku on 26/11/15.
 */
public class NotificationPreferences {

    private static String myPreferences = "NotificationData";
    private static SharedPreferences sharedPrefs;

    private static int okCount = 0;
    private static int botherCount = 0;

    private static void init() {
        if (sharedPrefs == null) sharedPrefs = NotificationService.getContext().getSharedPreferences(myPreferences, NotificationService.getContext().MODE_PRIVATE);
    }

    public static void addDummyOk() {
        //Log.d("notiprefs", "everything is fine :)");
        init();
        SharedPreferences.Editor editor = sharedPrefs.edit();
        okCount = sharedPrefs.getInt("okcount", 0);
        okCount++;
        editor.putInt("okcount", okCount);
        editor.commit();
        //Log.d("notiprefs", "okcount: " + okCount);
    }

    public static void addDummyDontBother() {
        //Log.d("notiprefs", "dont bother plx!");
        init();
        SharedPreferences.Editor editor = sharedPrefs.edit();
        botherCount = sharedPrefs.getInt("bothercount", 0);
        botherCount++;
        editor.putInt("bothercount", botherCount);
        editor.commit();
        //Log.d("notiprefs", "bothercount: " + botherCount);
    }

    private static double getCurrentDummyPreference() {
        init();
        okCount = sharedPrefs.getInt("okcount", 0);
        botherCount = sharedPrefs.getInt("bothercount", 0);
        // average propability from inputted responses (ok / dont bother)
        // ok counts as 1, 'dont bother' as 0 and if # of inputs < 20,
        // each missing input counts as 0.5
        double a = (okCount + (20-okCount-botherCount)*0.5) / 20;
        // min/max propability window calculated over first 100 inputted responses
        // starting from range of 25% to 75% and going down to range of 5% to 95%
        double b =  (25 - ((Math.min(1.0, ((double) (okCount+botherCount)/100)))*20)) / 100;
        // if a is outside the given accepted window, return a boundary value
        if (a < b) return b;
        else if (a > (100-b)) return 100-b;
        else return a;
    }

    private static double getCurrentLearningPreference() {
        return 50;
    }

    public static double getCurrentPreference() {
        switch (NotificationService.getMode()) {
            case DUMMY_MODE:
                return getCurrentDummyPreference();
            case LEARNING_MODE:
                return getCurrentLearningPreference();
            default:
                return 50;
        }
    }

}
