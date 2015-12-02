package com.comag.aku.symptomtracker.model;

import android.util.Log;

import com.colintmiller.simplenosql.NoSQL;
import com.colintmiller.simplenosql.NoSQLEntity;
import com.colintmiller.simplenosql.RetrievalCallback;
import com.comag.aku.symptomtracker.MainActivity;
import com.comag.aku.symptomtracker.AppHelpers;
import com.comag.aku.symptomtracker.app_settings.AppPreferences;
import com.comag.aku.symptomtracker.graphics.adapters.FactorRowAdapter;
import com.comag.aku.symptomtracker.graphics.adapters.SymptomRowAdapter;
import com.comag.aku.symptomtracker.model.data_storage.Values;
import com.comag.aku.symptomtracker.objects.ValueMap;
import com.comag.aku.symptomtracker.objects.tracking.Condition;
import com.comag.aku.symptomtracker.services.NotificationService;

import java.util.List;

/**
 * Created by aku on 04/11/15.
 */
public class NoSQLStorage {

    public static void clear(String db_name) {
        NoSQL.with(AppHelpers.currentContext).using(DataObject.class)
                .bucketId(db_name)
                .delete();
    }

    public static void drop(Condition c) {
        DataObject o = new DataObject(c, Values.values.get(c));
        NoSQL.with(AppHelpers.currentContext).using(DataObject.class)
                .bucketId(AppPreferences.getSchema().db_name)
                .entityId(o.getId())
                .delete();
    }

    public static void storeSingle(Condition c, ValueMap v) {
        Log.d("NoSQL", "Storing: " + v.toRenderableString());
        Log.d("NoSQL", "time is : " + c.timestamp);
        AppHelpers.cal.setTimeInMillis(c.timestamp);
        Log.d("NoSQL", "date: " + AppHelpers.dayFormat.format(AppHelpers.cal.getTime()));
        Condition removed = null;
        searchExisting:
        for (Condition existing : Values.values.keySet()) {
            if (c.isSimilar(existing)) {
                c = existing;
                drop(c);
                removed = c;
                break searchExisting;
            }
        }
        if (removed != null) Values.values.remove(removed);
        DataObject o = new DataObject(c, v);
        NoSQLEntity<DataObject> entity = new NoSQLEntity<>(AppPreferences.getSchema().db_name, o.getId());
        entity.setData(o);
        NoSQL.with(AppHelpers.currentContext).using(DataObject.class).save(entity);
        Values.put(o.c, o.v);
        //loadValues(MainActivity.tab);
    }

    public static void loadValues(final String tabName) {
        final long start = System.currentTimeMillis();
        Values.values.clear();
        NoSQL.with(AppHelpers.currentContext).using(DataObject.class)
            .bucketId(AppPreferences.getSchema().db_name)
            .retrieve(new RetrievalCallback<DataObject>() {
                public void retrievedResults(List<NoSQLEntity<DataObject>> entities) {
                    // Display results or something
                    for (int i = 0; i < entities.size(); i++) {
                        if (entities.get(i).getData().c.isCurrent()) {
                            Values.values.put(entities.get(i).getData().c, entities.get(i).getData().v);
                        }
                    }
                    if (tabName.equals("symptoms"))
                        ((SymptomRowAdapter) MainActivity.symptom_list.getAdapter()).notifyDataSetChanged();
                    else if (tabName.equals("factors"))
                        ((FactorRowAdapter) MainActivity.factor_list.getAdapter()).notifyDataSetChanged();
                    Log.d("log", "load took: " + String.valueOf(System.currentTimeMillis() - start));
                }
            });
    }

    public static void loadAllValues() {
        Values.values.clear();
        NoSQL.with(AppHelpers.currentContext).using(DataObject.class)
            .bucketId(AppPreferences.getSchema().db_name)
            .retrieve(new RetrievalCallback<DataObject>() {
                public void retrievedResults(List<NoSQLEntity<DataObject>> entities) {
                    if (entities.size() > 0) {
                        for (int i = 0; i < entities.size(); i++) {
                            DatabaseStorage.values.put(entities.get(i).getData().c, entities.get(i).getData().v);
                            //Log.d("noSQLData", entities.get(i).getData().c.toRenderableString() + " , value: " + entities.get(i).getData().v.toRenderableString());
                        }
                        MainActivity.chartChanged();
                    }
                }
            });
    }

    public static void serviceLoad() {
        Values.values.clear();
        NoSQL.with(NotificationService.getContext()).using(DataObject.class)
                .bucketId(AppPreferences.getSchema().db_name)
                .retrieve(new RetrievalCallback<DataObject>() {
                    public void retrievedResults(List<NoSQLEntity<DataObject>> entities) {
                        if (entities.size() > 0) {
                            for (int i = 0; i < entities.size(); i++) {
                                DatabaseStorage.values.put(entities.get(i).getData().c, entities.get(i).getData().v);
                                //Log.d("noSQLData", entities.get(i).getData().c.toRenderableString() + " , value: " + entities.get(i).getData().v.toRenderableString());
                            }
                        }
                    }
                });

    }

}
