package com.comag.aku.lifetracker.app_settings;

import com.aware.Aware;
import com.comag.aku.lifetracker.AppHelpers;
import com.comag.aku.lifetracker.services.NotificationPreferences;
import com.comag.aku.lifetracker.services.NotificationService;

/**
 * Created by aku on 30/11/15.
 */
public class UserSettings {
    boolean popupsAutomated = true;
    int notificationHour = 18;
    int popupFrequency = 50;
    int popupInterval = 1 * AppHelpers.MINUTE_IN_MILLISECONDS;
    String userId = "";
    boolean data_sync = false;


    public void setPopupsAutomated(Boolean automated) {
        popupsAutomated = automated;
    }

    public void setNotificationHour(int hour) {
        notificationHour = hour;
    }

    public void setPopupFrequency(int freq) {
        popupFrequency = freq;
    }

    public void setPopupInterval(int interval) {
        popupInterval = interval;
    }

    public void setUserId(String id) {
        userId = id;
    }

    public void setDataSync(boolean enabled) {data_sync = enabled;}

    public boolean isPopupsAutomated() {
        return popupsAutomated;
    }

    public int getNotificationHour() {
        return notificationHour;
    }

    public int getPopupFrequency() {
        if (!popupsAutomated) return popupFrequency;
        else return NotificationPreferences.getCurrentPreference();
    }

    public int getPopupInterval() {
        //Log.d("UserSettings", "popupinterval: " + popupInterval);
        return popupInterval;
    }

    public boolean dataSyncEnabled() {
        //Log.d("study url", AppPreferences.schema.aware_study_url);
        if (AppPreferences.schema.aware_study_url != null && Aware.getSetting(NotificationService.getContext(), Aware.STUDY_ID).length() > 0) {
            //Log.d("settings", "joined study");
            Aware.joinStudy(NotificationService.getContext(), AppPreferences.schema.aware_study_url);
        }
        return (Aware.getSetting(NotificationService.getContext(), Aware.STUDY_ID).length() > 0);
    }

    public String getUserId() { return userId; }

}
