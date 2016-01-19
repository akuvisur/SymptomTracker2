package com.comag.aku.lifetracker.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.colintmiller.simplenosql.NoSQL;
import com.colintmiller.simplenosql.NoSQLEntity;
import com.colintmiller.simplenosql.RetrievalCallback;
import com.comag.aku.lifetracker.Launch;
import com.comag.aku.lifetracker.R;
import com.comag.aku.lifetracker.app_settings.AppPreferences;
import com.comag.aku.lifetracker.model.DataObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aku on 02/12/15.
 */
public class ReminderNotification {

    public void show() {
        missingValues();
    }

    private void emit() {
        Intent launchIntent = new Intent(NotificationService.getContext(), Launch.class);
        PendingIntent intent = PendingIntent.getActivity(NotificationService.getContext(), 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder mBuilder =
                new Notification.Builder(NotificationService.getContext().getApplicationContext())
                        .setSmallIcon(R.drawable.info_color)
                        .setContentTitle("Symptom tracker")
                        .setContentText("You are still missing " + missingKeys.size() + " inputs for today.");
        mBuilder.setContentIntent(intent);
        // Gets an instance of the NotificationController service
        NotificationManager mNotifyMgr = (NotificationManager) NotificationService.getContext().getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(0, mBuilder.build());
    }

    private ArrayList<String> missingKeys;
    public void missingValues() {
        // list of all the factors and symptoms we are tracking
        missingKeys = new ArrayList<>();
        for (String key : AppPreferences.factors.keySet()) missingKeys.add(key);
        for (String key : AppPreferences.symptoms.keySet()) missingKeys.add(key);

        NoSQL.with(NotificationService.getContext()).using(DataObject.class)
                .bucketId(AppPreferences.getSchema().db_name)
                .retrieve(new RetrievalCallback<DataObject>() {
                    public void retrievedResults(List<NoSQLEntity<DataObject>> entities) {
                        // remove keys from missingList if they are current data
                        for (int i = 0; i < entities.size(); i++) {
                            if (entities.get(i).getData().c.isCurrent()) {
                                missingKeys.remove(entities.get(i).getData().c.key);
                            }
                        }
                        if (!missingKeys.isEmpty()) emit();
                    }
                });
    }
}
