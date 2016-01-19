package com.comag.aku.lifetracker.objects;

import com.comag.aku.lifetracker.app_settings.AppPreferences;
import com.google.gson.JsonObject;

/**
 * Created by aku on 26/10/15.
 */
public class Factor extends Trackable {
    public JsonObject json;

    public String name;
    public String key;
    public String input;
    public String desc;
    public String range_min;
    public String range_max;
    public String values;

    public Factor(JsonObject o) {
        this.json = o;
        this.name = o.get("name").getAsString();
        this.desc = o.get("desc").getAsString();
        this.type = o.get("type").getAsString();
        this.key = o.get("key").getAsString();
        this.input = o.get("input").getAsString();
        this.rep_window = o.get("rep_window").getAsString();
        this.range_max = o.get("range_max").getAsString();
        this.range_min = o.get("range_min").getAsString();
        if (o.has("values") && !o.get("values").isJsonNull()) this.values = o.get("values").getAsString();
        else values = "empty";

    }

    public String toString() {
        return name + " : " + desc;
    }

    public static String keyToName(String key) {
        return AppPreferences.factors.get(key).name;
    }
}
