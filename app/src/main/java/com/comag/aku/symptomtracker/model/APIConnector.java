package com.comag.aku.symptomtracker.model;

import com.comag.aku.symptomtracker.Settings;
import com.comag.aku.symptomtracker.model.Deserializers.SchemaDeserializer;
import com.comag.aku.symptomtracker.objects.Schema;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Converter;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;

/**
 * Created by aku on 26/10/15.
 */
public class APIConnector {
    //an instance of this client object
    private static ApiInterface ApiInterface;
    //if the omdbApiInterface object has been instantiated, return it, but if not, build it then return it.
    // ** SINGLETON :)(:**
    public static ApiInterface getConnector() {
        if (ApiInterface == null) {
            GsonBuilder schemaBuilder = new GsonBuilder();
            schemaBuilder.registerTypeAdapter(Schema.class, new SchemaDeserializer());

            Retrofit restAdapter = new Retrofit.Builder()
                    .baseUrl(Settings.APIURL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            ApiInterface = restAdapter.create(ApiInterface.class);
        }
        return ApiInterface;
    }

    public interface ApiInterface {
        @GET("/schema/all/obj/")
        Call<JsonObject> getSchemas();
        @POST("/symptom/keys/obj/")
        Call<JsonObject> getSymptomsKeys(@Body String body);
        @POST("/factor/keys/obj/")
        Call<JsonObject> getFactorsKeys(@Body String body);

    }

}
