package com.comag.aku.symptomtracker.objects;

import com.comag.aku.symptomtracker.app_settings.AppPreferences;
import com.comag.aku.symptomtracker.model.Deserializers.SymptomDeserializer;
import com.comag.aku.symptomtracker.model.Serializers.SymptomSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    public Symptom(String name, String desc, String rep_window) {
        this.name = name;
        this.desc = desc;
        if (rep_window == null) rep_window = "day";
        this.rep_window = rep_window;
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
