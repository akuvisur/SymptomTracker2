package com.comag.aku.symptomtracker.graphics.chart_formatters;

import com.github.mikephil.charting.formatter.XAxisValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by aku on 18/11/15.
 */
public class XAxisTimeFormatter implements XAxisValueFormatter {
    private int calType;
    private Calendar cal = Calendar.getInstance();

    SimpleDateFormat day = new SimpleDateFormat("EEE");
    SimpleDateFormat month = new SimpleDateFormat("MMM");

    public XAxisTimeFormatter(int calType) {
        this.calType = calType;
    }

    @Override
    public String getXValue(String original, int index, ViewPortHandler viewPortHandler) {
        switch (calType) {
            case Calendar.HOUR_OF_DAY:
                return original;
            case Calendar.DAY_OF_WEEK:
                cal.set(Calendar.DAY_OF_WEEK, Integer.valueOf(original));
                return day.format(cal.getTime());
            case Calendar.WEEK_OF_MONTH:
                return "Week " + original;
            case Calendar.MONTH:
                cal.set(Calendar.MONTH, Integer.valueOf(original));
                return month.format(cal.getTime());
        }
        return original;
    }
}
