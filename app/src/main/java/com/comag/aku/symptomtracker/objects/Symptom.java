package com.comag.aku.symptomtracker.objects;

import com.comag.aku.symptomtracker.app_settings.AppPreferences;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Created by aku on 26/10/15.
 */
public class Symptom extends Trackable {
    public JsonElement json;

    public String name;
    public String desc;
    public String _class;
    public String type;
    public String key;
    public String severity;

    public Symptom(JsonObject o) {
        this.json = o;
        name = o.get("name").getAsString();
        desc = o.get("desc").getAsString();
        _class = o.get("class").getAsString();
        type = o.get("type").getAsString();
        key = o.get("key").getAsString();
        rep_window = o.get("rep_window").getAsString();
        severity = o.get("severity").getAsString();
    }

    public String toString() {
        return name + " : " + desc;
    }

    public static String keyToName(String key) {
        return AppPreferences.symptoms.get(key).name;
    }
}
