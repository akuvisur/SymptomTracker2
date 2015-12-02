package com.comag.aku.symptomtracker.model;

import com.comag.aku.symptomtracker.AppHelpers;
import com.comag.aku.symptomtracker.model.Deserializers.SchemaDeserializer;
import com.comag.aku.symptomtracker.objects.Schema;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import retrofit.Call;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;

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
                    .baseUrl(AppHelpers.APIURL)
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
