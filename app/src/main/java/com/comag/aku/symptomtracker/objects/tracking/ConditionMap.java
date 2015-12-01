package com.comag.aku.symptomtracker.objects.tracking;

import com.comag.aku.symptomtracker.Settings;
import com.comag.aku.symptomtracker.objects.ValueMap;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by aku on 18/11/15.
 */
public class ConditionMap {
    public String key;
    public String window;
    public Integer timeValue;
    public Integer year;
    public Integer day;

    public final long timestamp;

    private ArrayList<ValueMap> values;

    public ConditionMap(String key, Integer time, String window, Integer year, Integer day, long timestamp) {
        this.key = key;
        this.timeValue = time;
        this.window = window;
        this.year = year;
        this.day = day;
        this.timestamp = timestamp;
        this.values = new ArrayList<>();
    }

    public String getRenderableString() {
        return key + " : " + timeValue + " at " + day + " / " + year + " sum: " + getValue();
    }

    @Override
    public boolean equals(Object o) {
        ConditionMap c = (ConditionMap) o;
        Calendar cal = Calendar.getInstance();
        if (window == null) {return false;}
        else {
            // equals depends on the selected dataviewer grouping variable
            switch(Settings.calTime) {
                case Calendar.HOUR_OF_DAY:
                    cal.setTimeInMillis(c.timestamp);
                    int oHour = cal.get(Settings.calTime);
                    cal.setTimeInMillis(timestamp);
                    return (cal.get(Settings.calTime) == oHour)
                            && key.equals(c.key)
                            && year.equals(c.year)
                            && day.equals(c.day);

                case Calendar.DAY_OF_YEAR:
                    cal.setTimeInMillis(c.timestamp);
                    int oDay = cal.get(Settings.calTime);
                    cal.setTimeInMillis(timestamp);
                    return (cal.get(Settings.calTime) == oDay)
                            && key.equals(c.key)
                            && year.equals(c.year);

                case Calendar.WEEK_OF_YEAR:
                    cal.setTimeInMillis(c.timestamp);
                    int oWeek = cal.get(Settings.calTime);
                    //Log.d("week", String.valueOf(oWeek));
                    cal.setTimeInMillis(timestamp);
                    //Log.d("other week", String.valueOf(cal.get(calTime)));
                    return (cal.get(Settings.calTime) == oWeek) && key.equals(c.key) && year.equals(c.year);

                case Calendar.MONTH:
                    cal.setTimeInMillis(c.timestamp);
                    int oMonth = cal.get(Settings.calTime);
                    //Log.d("month", String.valueOf(oMonth));
                    cal.setTimeInMillis(timestamp);
                    //Log.d("other month", String.valueOf(cal.get(calTime)));
                    return (cal.get(Settings.calTime) == oMonth) && key.equals(c.key) && year.equals(c.year);

                // i guess something went wrong..
                default:
                    return false;
            }
        }
    }

    @Override
    public int hashCode() {
        if (window != null && !window.equals("hour"))
            return (key+String.valueOf(timeValue)+String.valueOf(year)).hashCode();
        else
            return (key+String.valueOf(timeValue)+String.valueOf(year)+String.valueOf(day)).hashCode();
    }

    public void put(ValueMap v) {
        //Log.d("put value", v.toString() + " to " + key);
        values.add(v);
    }

    private Float parseValue(String value) {
        switch(value) {
            case "none":
                //Log.d("data", key + " none");
                return new Float(0.33);
            case "mild":
                //Log.d("data", key + " mild");
                return new Float(1);
            case "severe":
                //Log.d("data", key + " severe");
                return new Float(3);
            default:
                return new Float(0);
        }
    }

    public float getValue() {
        float sum = 0;
        for (int i = 0; i < values.size(); i++) {
            sum += parseValue(values.get(i).getValue());
        }
        //Log.d(key, "value: " + sum + "/" + values.size() + " = " + sum/values.size());
        if (values.size() > 0) {
            //Log.d(key, "value: " + sum + "/" + values.size() + " = " + sum/values.size());
            return sum/values.size();
        }
        else return 0.0f;
        //else return new Float(Math.random() * 3);
    }
}