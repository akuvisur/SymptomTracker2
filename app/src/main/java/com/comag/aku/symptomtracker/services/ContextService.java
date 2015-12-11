package com.comag.aku.symptomtracker.services;

import android.app.IntentService;
import android.content.Intent;

/**
 * Created by aku on 11/12/15.
 */
public class ContextService extends IntentService {

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public ContextService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

    }

    public static String getContext() {

        return "{}";
    }
}
