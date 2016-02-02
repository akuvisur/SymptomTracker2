package com.comag.aku.lifetracker.services;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by aku on 02/02/16.
 */
public class WindowCleaner {
    private static HashMap<String, View> views = new HashMap<>();

    public static void addView(String key, View v) {
        // remove the existing before adding new
        if (views.containsKey(key)) {
            final WindowManager windowManager = (WindowManager) NotificationService.getContext().getSystemService(Context.WINDOW_SERVICE);
            try {
                windowManager.removeView(views.get(key));
            }
            catch (Exception e) {
                Log.d("WindowCleaner", "crashed when trying to remove a view");
            }
        }
        views.put(key, v);
    }

    public static void clearViews() {
        final WindowManager windowManager = (WindowManager) NotificationService.getContext().getSystemService(Context.WINDOW_SERVICE);
        ArrayList<String> removedKeys = new ArrayList<>();
        for (String key : views.keySet()) {
            try {
                windowManager.removeView(views.get(key));
                removedKeys.add(key);
            }
            catch (Exception e) {
                Log.d("WindowCleaner", "crashed when trying to remove a view");
            }
        }

        // clean removed views from map
        for (String removed : removedKeys) {
            views.remove(removed);
        }
    }

    public static void removeView(String key) {
        if (views.containsKey(key)) {
            final WindowManager windowManager = (WindowManager) NotificationService.getContext().getSystemService(Context.WINDOW_SERVICE);
            try {
                windowManager.removeView(views.get(key));
            }
            catch (Exception e) {
                Log.d("WindowCleaner", "crashed when trying to remove a view");
            }
        }
    }
}
