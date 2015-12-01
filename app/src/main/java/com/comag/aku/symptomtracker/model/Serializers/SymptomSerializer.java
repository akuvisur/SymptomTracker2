package com.comag.aku.symptomtracker.model.Serializers;

import com.comag.aku.symptomtracker.objects.Symptom;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

/**
 * Created by aku on 01/12/15.
 */
public class SymptomSerializer {

    public static JsonElement serialize(Symptom src) {
        JsonObject result = new JsonObject();
        result.add("name", new JsonPrimitive(src.name));
        result.add("desc", new JsonPrimitive(src.desc));
        result.add("class", new JsonPrimitive(src._class));
        result.add("type", new JsonPrimitive(src.type));
        result.add("key", new JsonPrimitive(src.key));
        result.add("rep_window", new JsonPrimitive(src.rep_window));
        result.add("severity", new JsonPrimitive(src.severity));

        return result;
    }
}
