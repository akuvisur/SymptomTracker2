package com.comag.aku.lifetracker.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityManagerCompat;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import com.aware.Aware;
import com.comag.aku.lifetracker.R;
import com.comag.aku.lifetracker.app_settings.AppPreferences;

import java.util.List;

/**
 * Created by aku on 22/12/15.
 */
public class ApplicationMonitor extends AccessibilityService {

    final public static String NEW_FOREGROUND = "appmonitor_new_foreground";

    @Override
    public void onServiceConnected() {
        //Log.d("appmonitor", "service connected");
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.packageNames = null;
        this.setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        //Log.d("appmonitor", "new access event");
        PackageManager packageManager = getPackageManager();

        ApplicationInfo appInfo;
        try {
            appInfo = packageManager.getApplicationInfo(event.getPackageName().toString(), PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException | NullPointerException | Resources.NotFoundException e) {
            appInfo = null;
        }

        PackageInfo pkgInfo;
        try {
            pkgInfo = packageManager.getPackageInfo(event.getPackageName().toString(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException | NullPointerException | Resources.NotFoundException e) {
            pkgInfo = null;
        }

        String appName = "";
        try {
            if (appInfo != null) {
                appName = packageManager.getApplicationLabel(appInfo).toString();
            }
        } catch (Resources.NotFoundException | NullPointerException e) {
            appName = "";
        }

        Intent newForeground = new Intent(NEW_FOREGROUND);
        newForeground.putExtra("app_name", appName);
        if (pkgInfo != null) newForeground.putExtra("package_name", pkgInfo.packageName);
        sendBroadcast(newForeground);

        AppPreferences.addUsedApplication(appName, pkgInfo.packageName, "category");
    }

    @Override
    public void onInterrupt() {
        Log.d("appmonitor", "has been destroyed");
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // TODO Auto-generated method stub
        Intent restartService = new Intent(getApplicationContext(),
                ApplicationMonitor.class);
        restartService.setPackage(getPackageName());
        PendingIntent restartServicePI = PendingIntent.getService(
                getApplicationContext(), 1, restartService,
                PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmService = (AlarmManager)getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePI);

    }

    private static boolean isAccessibilityEnabled(Context c) {
        boolean enabled = false;
        Log.d("accessibility_service", "checking for package name: " + c.getPackageName());

        AccessibilityManager accessibilityManager = (AccessibilityManager) c.getSystemService(ACCESSIBILITY_SERVICE);

        //Try to fetch active accessibility services directly from Android OS database instead of broken API...
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        String settingValue = Settings.Secure.getString(c.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if( settingValue != null ) {
            splitter.setString(settingValue);
            while (splitter.hasNext()) {

                if (splitter.next().matches(c.getPackageName())){
                    enabled = true;
                    break;
                }
            }
        }
        if( ! enabled ) {
            try {
                List<AccessibilityServiceInfo> enabledServices = AccessibilityManagerCompat.getEnabledAccessibilityServiceList(accessibilityManager, AccessibilityEventCompat.TYPES_ALL_MASK);
                if( ! enabledServices.isEmpty() ) {
                    for( AccessibilityServiceInfo service : enabledServices ) {
                        Log.d("accessibility_service", service.toString());
                        if( service.getId().contains(c.getPackageName()) ) {
                            enabled = true;
                            break;
                        }
                    }
                }
            } catch ( NoSuchMethodError e ) {}
        }
        if( ! enabled ) {
            try{
                List<AccessibilityServiceInfo> enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
                if ( ! enabledServices.isEmpty()) {
                    for (AccessibilityServiceInfo service : enabledServices) {
                        Log.d(Aware.TAG, service.toString());
                        if (service.getId().contains(c.getPackageName())) {
                            enabled = true;
                            break;
                        }
                    }
                }
            }catch (NoSuchMethodError e) {}
        }
        return enabled;
    }

    public static boolean isAccessibilityServiceActive(Context c) {
        // service verification was bugged 15/1/2016
        // would always remind user to switch services on
        return true;
        /*
        if( ! isAccessibilityEnabled(c) ) {
            sendAccessibilityServiceVerification(c);
        }
        return isAccessibilityEnabled(c);
        */
    }

    public static void sendAccessibilityServiceVerification(Context c) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(c);
        mBuilder.setSmallIcon(R.drawable.ic_stat_aware_accessibility);
        mBuilder.setContentTitle("LifeTracker configuration");
        mBuilder.setContentText(c.getResources().getString(R.string.activate_accessibility));
        mBuilder.setAutoCancel(true);
        mBuilder.setOnlyAlertOnce(true); //notify the user only once
        mBuilder.setDefaults(NotificationCompat.DEFAULT_ALL);

        Intent accessibilitySettings = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
        accessibilitySettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent clickIntent = PendingIntent.getActivity(c, 0, accessibilitySettings, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(clickIntent);
        NotificationManager notManager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        notManager.notify(987654, mBuilder.build());
    }
}
