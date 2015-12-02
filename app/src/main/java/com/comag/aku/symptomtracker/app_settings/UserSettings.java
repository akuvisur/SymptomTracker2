package com.comag.aku.symptomtracker.app_settings;

import com.comag.aku.symptomtracker.services.NotificationPreferences;
import com.comag.aku.symptomtracker.services.NotificationService;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;

/**
 * Created by aku on 30/11/15.
 */
public class UserSettings {
    boolean popupsAutomated = true;
    int notificationHour = 18;
    int popupFrequency = 50;
    int popupInterval = NotificationService.NOTIFICATION_DELAY_MS;

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
        return popupInterval;
    }

}
