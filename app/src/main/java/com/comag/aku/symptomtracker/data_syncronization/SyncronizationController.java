package com.comag.aku.symptomtracker.data_syncronization;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.Settings;
import android.util.Log;

import com.comag.aku.symptomtracker.app_settings.AppPreferences;
import com.comag.aku.symptomtracker.model.data_storage.Values;
import com.comag.aku.symptomtracker.objects.Trackable;
import com.comag.aku.symptomtracker.objects.ValueMap;
import com.comag.aku.symptomtracker.objects.tracking.Condition;
import com.comag.aku.symptomtracker.services.NotificationService;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;

/**
 * Created by aku on 10/12/15.
 */
public class SyncronizationController {

    public static void storeAdverseEvent(Condition cond, ValueMap values) {
        ContentValues c = new ContentValues();

        c.put(SyncProvider.AdverseEventData.TIMESTAMP, cond.timestamp / 1000);
        c.put(SyncProvider.AdverseEventData.DEVICE_ID, Settings.Secure.getString(NotificationService.getContext().getContentResolver(), Settings.Secure.ANDROID_ID));
        c.put(SyncProvider.AdverseEventData.USER_ID, AppPreferences.userSettings.getUserId());
        c.put(SyncProvider.AdverseEventData.TRACKABLE_KEY, cond.key);
        if (cond.key.contains("symptom_")) {
            c.put(SyncProvider.AdverseEventData.TRACKABLE_FREQUENCY, AppPreferences.symptoms.get(cond.key).rep_window);
            c.put(SyncProvider.AdverseEventData.TRACKABLE_TYPE, "symptom");
            c.put(SyncProvider.AdverseEventData.TRACKABLE_FREQUENCY_VALUE, getFrequencyValue(AppPreferences.symptoms.get(cond.key).rep_window, cond.timestamp));
        }
        else if (cond.key.contains("factor_")) {
            c.put(SyncProvider.AdverseEventData.TRACKABLE_FREQUENCY, AppPreferences.factors.get(cond.key).rep_window);
            c.put(SyncProvider.AdverseEventData.TRACKABLE_TYPE, "factor");
            c.put(SyncProvider.AdverseEventData.TRACKABLE_FREQUENCY_VALUE, getFrequencyValue(AppPreferences.factors.get(cond.key).rep_window, cond.timestamp));
        }

        c.put(SyncProvider.AdverseEventData.COMMENT, values.getComment());
        c.put(SyncProvider.AdverseEventData.INPUT, values.getValue());

        if (values.getPicturePath() != null && values.getPicturePath().length() > 0) {
            Bitmap bm = BitmapFactory.decodeFile(values.getPicturePath());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bm.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
            byte[] imgData = outputStream.toByteArray();
            c.put(SyncProvider.AdverseEventData.PICTURE, imgData);
        }

        for (String k : c.keySet()) {
            Log.d("db_insert", k + " : " + c.getAsString(k));
        }
        SyncProvider s = new SyncProvider();
        try {
            s.insert(SyncProvider.AdverseEventData.CONTENT_URI, c);
        }
        catch (SQLException e) {
            Log.d("DataSync", "update value instead of insert");
            Cursor cur = s.query(
                    SyncProvider.AdverseEventData.CONTENT_URI,
                    new String[]{SyncProvider.AdverseEventData._ID},
                    SyncProvider.AdverseEventData.DEVICE_ID + "='" + c.getAsString(SyncProvider.AdverseEventData.DEVICE_ID) + "' AND " +
                    SyncProvider.AdverseEventData.TRACKABLE_FREQUENCY + "='" + c.getAsString(SyncProvider.AdverseEventData.TRACKABLE_FREQUENCY) + "' AND " +
                    SyncProvider.AdverseEventData.TRACKABLE_FREQUENCY_VALUE + "='" + c.getAsString(SyncProvider.AdverseEventData.TRACKABLE_FREQUENCY_VALUE) + "' AND " +
                    SyncProvider.AdverseEventData.TRACKABLE_KEY + "='" + c.getAsString(SyncProvider.AdverseEventData.TRACKABLE_KEY) + "'"
                    ,
                    null,
                    null
            );
            if (cur != null && cur.moveToFirst()) {
                s.update(
                        SyncProvider.AdverseEventData.CONTENT_URI,
                        c,
                        SyncProvider.AdverseEventData._ID + " = ?",
                        new String[]{cur.getString(cur.getColumnIndex(SyncProvider.AdverseEventData._ID))}
                );
            }
        }

    }

    private static int getFrequencyValue(String window, long timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        switch (window) {
            case "hour":
                return c.get(Calendar.HOUR_OF_DAY);
            case "day":
                return c.get(Calendar.DAY_OF_YEAR);
            case "week":
                return c.get(Calendar.WEEK_OF_YEAR);
            case "month":
                return c.get(Calendar.MONTH);
            default:
                return 0;
        }
    }

    public static void storeNotificationResponse(String value, String type, String context) {
        ContentValues c = new ContentValues();
        c.put(SyncProvider.NotificationEventData.TIMESTAMP, System.currentTimeMillis() / 1000);
        c.put(SyncProvider.NotificationEventData.DEVICE_ID, Settings.Secure.getString(NotificationService.getContext().getContentResolver(), Settings.Secure.ANDROID_ID));
        c.put(SyncProvider.NotificationEventData.USER_ID, AppPreferences.userSettings.getUserId());
        c.put(SyncProvider.NotificationEventData.VALUE, value);
        c.put(SyncProvider.NotificationEventData.NOTIFICATION_TYPE, type);
        c.put(SyncProvider.NotificationEventData.CONTEXT, context);
        SyncProvider s = new SyncProvider();
        try {
            s.insert(SyncProvider.NotificationEventData.CONTENT_URI, c);
        }
        catch (SQLException e) {
            Log.d("notification row exists", "updating");
            Cursor cur = s.query(
                    SyncProvider.NotificationEventData.CONTENT_URI,
                    new String[]{SyncProvider.NotificationEventData._ID},
                    SyncProvider.NotificationEventData.DEVICE_ID + "='" + c.getAsString(SyncProvider.NotificationEventData.DEVICE_ID) + "' AND " +
                            SyncProvider.NotificationEventData.TIMESTAMP + "='" + c.getAsString(SyncProvider.NotificationEventData.TIMESTAMP) + "'",
                    null,
                    null
            );
            if (cur != null && cur.moveToFirst()) {
                s.update(
                        SyncProvider.NotificationEventData.CONTENT_URI,
                        c,
                        SyncProvider.NotificationEventData._ID,
                        new String[]{cur.getString(cur.getColumnIndex(SyncProvider.NotificationEventData._ID))}
                        );
            }
        }
    }

}
