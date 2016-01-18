package com.comag.aku.symptomtracker.model.REST_queries;

import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.comag.aku.symptomtracker.AppHelpers;
import com.comag.aku.symptomtracker.Launch;
import com.comag.aku.symptomtracker.model.APIConnector;
import com.comag.aku.symptomtracker.model.DatabaseStorage;
import com.comag.aku.symptomtracker.objects.Schema;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by aku on 28/10/15.
 */
public class Schemas implements Callback<JsonObject> {

    public void All() {
        Call<JsonObject> schemas = APIConnector.getConnector().getSchemas();
        schemas.enqueue(this);
    }
    @Override
    public void onResponse(Response<JsonObject> response, Retrofit retrofit) {
        JsonElement a = response.body().get("schemas");

        for (int i = 0; i < a.getAsJsonArray().size(); i++) {
            Schema s = new Schema(a.getAsJsonArray().get(i).getAsJsonObject());
            if (!DatabaseStorage.schemaList.contains(s)) {
                DatabaseStorage.schemaList.add(s);}
            Launch.allSchemas.put(s.key, s);
            Launch.visibleSchemas.put(s.key, s);
        }

        Toast.makeText(AppHelpers.currentContext, "Loaded " + Launch.allSchemas.size() + " schemas from repository", Toast.LENGTH_LONG).show();
        ((ArrayAdapter) Launch.schemaView.getAdapter()).notifyDataSetChanged();
    }
    @Override
    public void onFailure(Throwable t) {
        t.printStackTrace();
        Toast.makeText(AppHelpers.currentContext, "Failed to load schemas from repository. No internet connection?", Toast.LENGTH_LONG).show();
    }
}