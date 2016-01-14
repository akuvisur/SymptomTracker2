package com.comag.aku.symptomtracker.model.REST_queries;

import android.util.Log;

import com.comag.aku.symptomtracker.app_settings.AppPreferences;
import com.comag.aku.symptomtracker.model.APIConnector;
import com.comag.aku.symptomtracker.objects.Symptom;
import com.comag.aku.symptomtracker.services.NotificationService;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Created by aku on 03/11/15.
 */
public class Symptoms implements Callback<JsonObject> {

    public void Keys(String keys) {
        Call<JsonObject> schemas = APIConnector.getConnector().getSymptomsKeys(keys);
        schemas.enqueue(this);
    }

    @Override
    public void onResponse(Response<JsonObject> response, Retrofit retrofit) {
        JsonElement a = response.body().get("results");
        for (int i = 0; i < a.getAsJsonArray().size(); i++) {
            Symptom s = new Symptom(a.getAsJsonArray().get(i).getAsJsonObject());
            AppPreferences.symptoms.put(s.key, s);
            //Log.d("symptoms", "added " + s);
        }
        AppPreferences.storeSymptomsToSharedPrefs();

        // get the changed NotificationService users to see if this user has switched to smart mode
        JsonElement b = response.body().get("mode_changes");
        for (int i = 0; i < b.getAsJsonArray().size(); i++) {
            if (b.getAsJsonArray().get(i).getAsString().equals(AppPreferences.USER_ID)) {
                NotificationService.setMode(NotificationService.NotificationMode.LEARNING_MODE);
            }
        }
    }

    @Override
    public void onFailure(Throwable t) {
        Log.d("symptoms API", "failed to load, no internet connection?" +t);
    }
}
