package com.comag.aku.lifetracker.model.data_storage;

import android.util.Log;

import com.comag.aku.lifetracker.model.NoSQLStorage;
import com.comag.aku.lifetracker.objects.ValueMap;
import com.comag.aku.lifetracker.objects.tracking.Condition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by aku on 03/11/15.
 */
public class Values {

    public static HashMap<Condition, ValueMap> values = new HashMap<>();

    public static void storeDB() {
        // store values to db
    }

    public static void put(Condition condition, ValueMap value) {
        searchExisting:
        for (Condition existing : values.keySet()) {
            if (existing.isSimilar(condition)) {
                ValueMap m = values.get(existing);
                if (m.hasComment() && !value.hasComment()) {
                    String comment = m.getComment();
                    value.setComment(comment);
                }
                if (m.hasPicture() && !value.hasPicture()) {
                    String picPath = m.getPicturePath();
                    value.setPicturePath(picPath);
                }
                condition = existing;
                NoSQLStorage.drop(existing);
                break searchExisting;
            }
        }

        values.put(condition, value);
    }

    public static void addComment(Condition condition, String text) {
        Log.d("addcomment", "ei voi olla null?" + (condition == null));
        values.get(condition).setComment(text);
        NoSQLStorage.storeSingle(condition, values.get(condition));
    }

    public static void addPicturePath(Condition condition, String path) {
        values.get(condition).setPicturePath(path);
        NoSQLStorage.storeSingle(condition, values.get(condition));
    }

    public static void addMultipleValues(Condition condition, List<String> v) {
        values.put(condition, new ValueMap(v));
        NoSQLStorage.storeSingle(condition, values.get(condition));
    }

    public static ValueMap fetch(String key) {
        if (values == null) {
            NoSQLStorage.loadAllValues();
            return new ValueMap("missing");
        }
        Condition cc = new Condition(key);
        for (Condition c : values.keySet()) {
            if (cc.isSimilar(c)) return values.get(c);
        }
        return new ValueMap("missing");
    }

    // get values for factor with 'multiple' input type
    public static List<String> fetchMultipleValues(String key) {
        List<String> result = new ArrayList<>();
        Condition current = new Condition(key);
        for (Condition c : values.keySet()) {
            if (c.equals(current)) {
                String[] s = values.get(c).getValue().split(",");
                for (int i = 0; i < s.length; i++) {
                    String value = s[i].trim();
                    result.add(value);
                }
            }
        }
        return result;
    }

}
