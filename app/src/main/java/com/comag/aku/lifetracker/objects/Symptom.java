package com.comag.aku.lifetracker.objects;

import android.util.Log;

import com.comag.aku.lifetracker.app_settings.AppPreferences;
import com.comag.aku.lifetracker.model.Serializers.SymptomSerializer;
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
    public String key;
    public String severity;

    public boolean positiveRange = false;

    public Symptom(JsonObject o) {
        this.json = o;
        name = o.get("name").getAsString();
        desc = o.get("desc").getAsString();
        _class = o.get("class").getAsString();
        type = o.get("type").getAsString();
        key = o.get("key").getAsString();
        rep_window = o.get("rep_window").getAsString();
        severity = o.get("severity").getAsString();
        if (o.has("range_type") && o.get("range_type").getAsString().equals("positive")) positiveRange = true;
    }

    public Symptom(String name, String desc, String rep_window) {
        Log.d("symptom_window", name + ": " + rep_window);
        this.name = name;
        this.desc = desc;
        if (rep_window == null) this.rep_window = "day";
        else this.rep_window = rep_window;
        _class = "generated";
        severity = "generated";
        key = "symptom_generated_" + name;
        type = "symptom";
        json = SymptomSerializer.serialize(this);
    }

    public String toString() {
        return name + " : " + desc;
    }

    public static String keyToName(String key) {
        return AppPreferences.symptoms.get(key).name;
    }
}
