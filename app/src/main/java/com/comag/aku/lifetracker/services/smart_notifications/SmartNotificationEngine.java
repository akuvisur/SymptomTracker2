package com.comag.aku.lifetracker.services.smart_notifications;

import android.database.Cursor;
import android.util.Log;

import com.comag.aku.lifetracker.data_syncronization.Plugin;
import com.comag.aku.lifetracker.data_syncronization.SyncProvider;
import com.comag.aku.lifetracker.services.UserContextService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

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

        String[] projection = {
                SyncProvider.NotificationEventData.CONTEXT,
                SyncProvider.NotificationEventData.VALUE
        };

        Cursor c = sp.query(Plugin.URI[1], projection, null, null, null);
        if (c == null) return;
        while (c.moveToNext()) {
            // create a new list that contains all the sensors that are required
            ArrayList<String> curRequired = new ArrayList<>(UserContextService.required);
            try {
                JSONObject o = new JSONObject(c.getString(0));
                // add all missing keys as -1
                Iterator<String> keys = o.keys();
                while (keys.hasNext()) {
                    curRequired.remove(keys.next());
                }
                for (String key : curRequired) {
                    o.put(key, -1);
                }
                o.put("value", c.getString(1));
                pastContext.add(o);
                Log.d("past_context_dump", o.toString());
            }
            catch (JSONException e) {
                Log.d("past_context", "error generating JSON from string: " + c.getString(0));
            }

        }
        c.close();
    }

}
