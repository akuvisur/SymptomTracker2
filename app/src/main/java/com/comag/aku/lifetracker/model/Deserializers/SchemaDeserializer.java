package com.comag.aku.lifetracker.model.Deserializers;

import com.comag.aku.lifetracker.objects.Schema;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * Created by aku on 28/10/15.
 */
public class SchemaDeserializer implements JsonDeserializer<Schema> {

    @Override
    public Schema deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return new Schema(json.getAsJsonObject());
    }
}
