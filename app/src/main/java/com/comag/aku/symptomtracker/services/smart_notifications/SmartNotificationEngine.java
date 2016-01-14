package com.comag.aku.symptomtracker.services.smart_notifications;

import android.database.Cursor;
import android.util.Log;

import com.comag.aku.symptomtracker.data_syncronization.Plugin;
import com.comag.aku.symptomtracker.data_syncronization.SyncProvider;
import com.comag.aku.symptomtracker.services.UserContextService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by aku on 11/01/16.
 */
public class SmartNotificationEngine {
    // can be enabled after future client update
    public static boolean isEnabled() {
        return false;
    }

    static JSONObject userContext;
    public static boolean emitNow() {
        // generate past context
        generatePastContext();

        // get current context
        userContext = UserContextService.getUserContext();

        // get context data from userContext JSONObject


        // do analysis


        // return if this userContext is a favorable moment or not
        // can e.g. calculate a confidence value and compare Math.Random() to it to create true/false
        return false;
    }

    private static SyncProvider sp;
    private static ArrayList<JSONObject> pastContext;
    public static void generatePastContext() {
        pastContext = new ArrayList<>();
        sp = new SyncProvider();

        String[] projection = {SyncProvider.NotificationEventData.CONTEXT};

        Cursor c = sp.query(Plugin.URI[1], projection, null, null, null);
        if (c == null) return;
        while (c.moveToNext()) {
            try {
                pastContext.add(new JSONObject(c.getString(0)));
            }
            catch (JSONException e) {//do nothing on expeption
            }
        }
        c.close();
    }

}
