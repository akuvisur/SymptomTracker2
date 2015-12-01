package com.comag.aku.symptomtracker.objects.tracking;

import com.comag.aku.symptomtracker.Settings;
import com.comag.aku.symptomtracker.app_settings.AppPreferences;

import java.util.Calendar;

/**
 * Created by aku on 03/11/15.
 */
public class Condition {
    public int hour;
    public int day;
    public int month;
    public int year;
    public int week;

    public String key;
    public String window;
    public String type;

    public Boolean uploaded = false;

    public final long timestamp;

    public Condition(String key) {
        day = Settings.cal.get(Calendar.DAY_OF_YEAR);
        hour = Settings.cal.get(Calendar.HOUR_OF_DAY);
        week = Settings.cal.get(Calendar.WEEK_OF_YEAR);
        month = Settings.cal.get(Calendar.MONTH);
        year = Settings.cal.get(Calendar.YEAR);
        this.key = key;

        if (key.contains("symptom_")) {
            type = "symptom";
            window = AppPreferences.symptoms.get(key).rep_window;
        }
        else if (key.contains("factor_")) {
            type = "factor";
            window = AppPreferences.factors.get(key).rep_window;
        }

        timestamp = System.currentTimeMillis();
    }

    public Condition(String key, long timestamp) {
        this.timestamp = timestamp;
        this.key = key;
        if (key.contains("symptom_")) {
            type = "symptom";
            window = AppPreferences.symptoms.get(key).rep_window;
        }
        else if (key.contains("factor_")) {
            type = "factor";
            window = AppPreferences.factors.get(key).rep_window;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        day = cal.get(Calendar.DAY_OF_YEAR);
        hour = cal.get(Calendar.HOUR_OF_DAY);
        week = cal.get(Calendar.WEEK_OF_YEAR);
        month = cal.get(Calendar.MONTH);
        year = cal.get(Calendar.YEAR);
    }

    public String toRenderableString() {
        return key + " hour: " + hour + " day: " + day;
    }

    @Override
    public int hashCode() {
        String s = String.valueOf(hour) + String.valueOf(day) + String.valueOf(week) + String.valueOf(month) + String.valueOf(year) + key;
        //String s = String.valueOf(day) + String.valueOf(month) + String.valueOf(year) + key;
        //Log.d("Condition", "hashcode " + s.hashCode());
        return s.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        Condition c = (Condition) o;

        return isSimilar(c);
        /*
        // this.hour == c.hour missing
        if (this.hour == c.hour && this.day == c.day && this.month == c.month && this.year == c.year
            && this.key.equals(c.key)) {
            //Log.d("Condition", "equals: TRUE");
            return true;
        }
        else {
            //Log.d("Condition", "equals: FALSE");
            return false;
        }
        */
    }

    public boolean isSimilar(Condition c) {
        String rep_window = getRepWindow();
        switch (rep_window) {
            case "hour":
                return c.key.equals(key) && isHour(c.hour);
            case "day":
                return c.key.equals(key) && isDay(c.day);
            case "month":
                return c.key.equals(key) && isMonth(c.month);
            case "week":
                return c.key.equals(key) && isWeek(c.week);
            default:
                return false;
        }
    }

    public boolean isYear() {
        return Settings.cal.get(Calendar.YEAR) == year;
    }

    public boolean isMonth(int month) {
        return (isYear() && this.month == month);
    }

    public boolean isWeek(int week) {
        return (isYear() && this.week == week);
    }

    public boolean isDay(int day) {
        //Log.d("isDAY()", String.valueOf(day) + " compared to conditions " + this.day);
        return (isYear() && this.day == day);
    }

    // compare given hour to this conditions hour
    public boolean isHour(int hour) {
        //Log.d("isHour()", String.valueOf(hour) + " compared to conditions " + this.hour);
        return (isYear() && this.day == Settings.cal.get(Calendar.DAY_OF_YEAR) && this.hour == hour);
    }

    public boolean isCurrent() {
        //Log.d("Condition:isCurrent()", "isCurrent() called");
        String rep_window = getRepWindow();

        //Log.d("Condition:rep_window", rep_window);
        switch (rep_window) {
            case "hour":
                return isHour(Settings.cal.get(Calendar.HOUR_OF_DAY));
            case "day":
                return isDay(Settings.cal.get(Calendar.DAY_OF_YEAR));
            case "week":
                return isWeek(Settings.cal.get(Calendar.WEEK_OF_YEAR));
            case "month":
                return isMonth(Settings.cal.get(Calendar.MONTH));
            default:
                return false;
        }
    }


    private String getRepWindow() {
        String rep_window = "";
        if (!AppPreferences.symptoms.isEmpty()) {
            try {
                switch(type) {
                    case "symptom":
                        return AppPreferences.symptoms.get(key).rep_window;
                    case "factor":
                        return AppPreferences.factors.get(key).rep_window;
                    default:
                        return "day";
                }
            }
            catch (NullPointerException e) {
                // no rep window?
                return "day";
            }

        }
        return rep_window;
    }

}
