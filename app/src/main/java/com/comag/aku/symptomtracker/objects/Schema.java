package com.comag.aku.symptomtracker.objects;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by aku on 26/10/15.
 */
public class Schema {

    public JsonElement json;

    public String author;
    public String key;
    public String title;
    public String desc;
    public String db_name;
    public List<String> factors;
    public List<String> symptoms;
    public String schema_type;

    public Schema(String author, String title, String desc, String dbname, List<String> factors, List<String> symptoms, String type, String key) {
        this.author = author;
        this.title = title;
        this.desc = desc;
        this.db_name = dbname;
        this.factors = factors;
        this.symptoms = symptoms;
        this.schema_type = type;
        this.key = key;
    }

    public Schema(JsonObject o) {
        this.json = o;
        factors = new ArrayList<String>();
        symptoms = new ArrayList<String>();
        this.author = o.get("author").getAsString();
        this.title = o.get("title").getAsString();
        this.desc = o.get("desc").getAsString();
        this.db_name = o.get("db_name").getAsString();
        this.schema_type = o.get("schema_type").getAsString();
        this.key = o.get("key").getAsString();

        Log.d("schema", "schema: " + o.toString());
        try {
            for (JsonElement e : o.get("factors").getAsJsonArray()) {
                if (e instanceof JsonPrimitive) {
                    JsonPrimitive a = (JsonPrimitive) e;
                    factors.add(a.toString());
                } else {
                    for (JsonElement j : e.getAsJsonArray()) {
                        factors.add(j.toString());
                    }
                }
            }
        }
        // was not an array
        catch (IllegalStateException e) {
            symptoms.add(o.get("factors").getAsString());
        }

        try {
            for (JsonElement e : o.get("symptoms").getAsJsonArray()) {
                if (e instanceof JsonPrimitive) {
                    JsonPrimitive a = (JsonPrimitive) e;
                    symptoms.add(a.toString());
                } else {
                    for (JsonElement j : e.getAsJsonArray()) {
                        symptoms.add(j.toString());
                    }
                }
            }
        }
        catch (IllegalStateException e) {
            factors.add(o.get("symptoms").getAsString());
        }

    }

    public String toString() {
        return this.title;
    }

    public String symptomKeys() {
        Gson gson = new Gson();
        return gson.toJson(symptoms);
    }

    public String factorKeys() {
        Log.d("factors", "to keys:" + factors.toString());
        Gson gson = new Gson();
        return gson.toJson(factors);
    }
}
