package com.comag.aku.lifetracker.model.Deserializers;

import com.comag.aku.lifetracker.objects.Symptom;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Created by aku on 03/11/15.
 */
public class SymptomDeserializer implements JsonDeserializer<Symptom> {

    @Override
    public Symptom deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return new Symptom(json.getAsJsonObject());
    }
}
