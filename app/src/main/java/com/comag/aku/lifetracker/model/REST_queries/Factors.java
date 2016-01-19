package com.comag.aku.lifetracker.model.REST_queries;

import com.comag.aku.lifetracker.app_settings.AppPreferences;
import com.comag.aku.lifetracker.model.APIConnector;
import com.comag.aku.lifetracker.objects.Factor;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by aku on 05/11/15.
 */
public class Factors implements Callback<JsonObject> {

    public void Keys(String keys) {
        Call<JsonObject> schemas = APIConnector.getConnector().getFactorsKeys(keys);
        schemas.enqueue(this);
    }

    @Override
    public void onResponse(Response<JsonObject> response, Retrofit retrofit) {
        JsonElement a = response.body().get("results");
        for (int i = 0; i < a.getAsJsonArray().size(); i++) {
            Factor f = new Factor(a.getAsJsonArray().get(i).getAsJsonObject());
            AppPreferences.factors.put(f.key, f);
            //Log.d("factors", "added " + f);
        }
        AppPreferences.addFactors();
    }

    @Override
    public void onFailure(Throwable t) {
        //Toast.makeText(AppHelpers.currentContext, "Loading factors - No internet connection available?", Toast.LENGTH_SHORT).show();
    }
}
