package com.comag.aku.symptomtracker;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;

import com.comag.aku.symptomtracker.objects.Schema;
import com.comag.aku.symptomtracker.objects.Symptom;
import com.comag.aku.symptomtracker.services.NotificationService;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by aku on 26/10/15.
 */
public class AppHelpers {
    public static String package_name;

    public static final boolean DEBUG = false;

    public static Activity currentActivity;
    public static Context currentContext;
    public static LayoutInflater factory;

    public static int MINUTE_IN_MILLISECONDS = 60000;

    public static Calendar cal = Calendar.getInstance();

    public final static String APIURL = "http://kala1928.ddns.net:10001";

    public final static int REQUEST_IMAGE_CAPTURE = 1;

    public static String curPicturePath;
    public static String curPictureKey;

    public static SimpleDateFormat hourFormat = new SimpleDateFormat("E HH:mm");
    public static SimpleDateFormat extendedHourFormat = new SimpleDateFormat("E dd/M HH:mm");
    public static SimpleDateFormat dayFormat = new SimpleDateFormat("E dd/M");
    public static SimpleDateFormat weekFormat = new SimpleDateFormat("w/yyyy");
    public static SimpleDateFormat monthFormat = new SimpleDateFormat("M/yyyy");

    public static int calTime = Calendar.DAY_OF_YEAR;
    public static int yearOffset = 0;
    public static int dayOffset = 0;
    public static int weekOffset = 0;
    public static int monthOffset = 0;
    public static boolean showingPopup = false;

    public static int randomizeListColor(int position) {
        int n = position % 10;
        switch(n) {
            case 0:
                return ContextCompat.getColor(currentContext, R.color.list1);
            case 1:
                return ContextCompat.getColor(currentContext, R.color.list2);
            case 2:
                return ContextCompat.getColor(currentContext, R.color.list3);
            case 3:
                return ContextCompat.getColor(currentContext, R.color.list4);
            case 4:
                return ContextCompat.getColor(currentContext, R.color.list5);
            case 5:
                return ContextCompat.getColor(currentContext, R.color.list5);
            case 6:
                return ContextCompat.getColor(currentContext, R.color.list6);
            case 7:
                return ContextCompat.getColor(currentContext, R.color.list7);
            case 8:
                return ContextCompat.getColor(currentContext, R.color.list8);
            case 9:
                return ContextCompat.getColor(currentContext, R.color.list9);
            case 10:
                return ContextCompat.getColor(currentContext, R.color.list10);
            default:
                return ContextCompat.getColor(currentContext, R.color.colorPrimary);
        }
    }

    public static int generateServiceColor(int position) {
        int n = position % 10;
        switch(n) {
            case 0:
                return ContextCompat.getColor(NotificationService.getContext(), R.color.list1);
            case 1:
                return ContextCompat.getColor(NotificationService.getContext(), R.color.list2);
            case 2:
                return ContextCompat.getColor(NotificationService.getContext(), R.color.list3);
            case 3:
                return ContextCompat.getColor(NotificationService.getContext(), R.color.list4);
            case 4:
                return ContextCompat.getColor(NotificationService.getContext(), R.color.list5);
            case 5:
                return ContextCompat.getColor(NotificationService.getContext(), R.color.list5);
            case 6:
                return ContextCompat.getColor(NotificationService.getContext(), R.color.list6);
            case 7:
                return ContextCompat.getColor(NotificationService.getContext(), R.color.list7);
            case 8:
                return ContextCompat.getColor(NotificationService.getContext(), R.color.list8);
            case 9:
                return ContextCompat.getColor(NotificationService.getContext(), R.color.list9);
            case 10:
                return ContextCompat.getColor(NotificationService.getContext(), R.color.list10);
            default:
                return ContextCompat.getColor(NotificationService.getContext(), R.color.colorPrimary);
        }
    }

    public static int[] generateColorList(int size) {
        int[] result = new int[size];

        for (int i = 0; i < size; i++) {
            result[i] = generateServiceColor(i);
        }
        return result;
    }

    public static int generateFactorChartColor(int order) {
        switch (order) {
            case 0:
                return ContextCompat.getColor(AppHelpers.currentContext, R.color.Factor1);
            case 1:
                return ContextCompat.getColor(AppHelpers.currentContext, R.color.Factor2);
            case 2:
                return ContextCompat.getColor(AppHelpers.currentContext, R.color.Factor3);
            case 3:
                return ContextCompat.getColor(AppHelpers.currentContext, R.color.Factor4);
            case 4:
                return ContextCompat.getColor(AppHelpers.currentContext, R.color.Factor5);
            case 5:
                return ContextCompat.getColor(AppHelpers.currentContext, R.color.Factor6);
            default:
                return ContextCompat.getColor(AppHelpers.currentContext, R.color.Factor1);
        }
    }

    public static String parseSchema(Schema s) {
        String text = "This schema tracks "+s.symptoms.size()+" symptoms and "+s.factors.size()+" factors.";

        return text;
    }

    public static File createImageFile(String key) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_" + key;
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        return image;
    }

    public static void setGroup(int c) {
        calTime = c;}

    public static void offset(boolean reduce) {
        //Log.d("offset", reduce ? "reducing" : "adding");
        switch (calTime) {
            case Calendar.HOUR_OF_DAY:
                if (reduce) dayOffset++; else dayOffset--;
                weekOffset = monthOffset = yearOffset = 0;
                break;
            case Calendar.DAY_OF_YEAR:
                if (reduce) weekOffset++; else weekOffset--;
                dayOffset = monthOffset = yearOffset = 0;
                break;
            case Calendar.WEEK_OF_YEAR:
                if (reduce) monthOffset++; else monthOffset--;
                dayOffset = weekOffset = yearOffset = 0;
                break;
            case Calendar.MONTH:
                if (reduce) yearOffset++; else yearOffset--;
                dayOffset = weekOffset = monthOffset = 0;
                break;
        }
        //Log.d("offsets", String.valueOf(dayOffset) + String.valueOf(weekOffset));
    }

    // get name for the symptom's value based on the positive/negative input range
    public static String getSymptomValueName(Symptom s, int input) {
        if (s.positiveRange) {
            switch (input) {
                case 0: return "low";
                case 1: return "medium";
                case 2: return "high";
                default: return "low";
            }
        }
        switch (input) {
            case 0: return "none";
            case 1: return "mild";
            case 2: return "severe";
            default: return "none";
        }
    }
}
