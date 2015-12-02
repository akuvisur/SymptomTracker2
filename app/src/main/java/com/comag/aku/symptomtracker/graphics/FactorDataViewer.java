package com.comag.aku.symptomtracker.graphics;

import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.comag.aku.symptomtracker.AppHelpers;
import com.comag.aku.symptomtracker.MainActivity;
import com.comag.aku.symptomtracker.R;
import com.comag.aku.symptomtracker.model.DatabaseStorage;
import com.comag.aku.symptomtracker.objects.Factor;
import com.comag.aku.symptomtracker.objects.tracking.Condition;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by aku on 18/11/15.
 */
public class FactorDataViewer {

    private static TreeMap<Integer, String> timeSlots;

    public static HashMap<String, Integer> chartOrder = new HashMap<>();

    private static XAxis x;
    private static YAxis y;

    public static View generateFactorCombinedChart(String key, int order) {
        View view = AppHelpers.factory.inflate(R.layout.data_factor_combochart, null);
        CombinedChart b = (CombinedChart) view.findViewById(R.id.data_factorchart);
        TextView title = (TextView) view.findViewById(R.id.data_factor_title);
        LinearLayout topColor = (LinearLayout) view.findViewById(R.id.data_factorchart_top_color);
        topColor.setBackgroundColor(AppHelpers.generateFactorChartColor(order));

        title.setText(Factor.keyToName(key));

        MainActivity.factorCharts.put(key, b);
        MainActivity.factorViews.put(key, view);

        b.setData(generateFactorComboChartData(key, order));
        b.setGridBackgroundColor(ContextCompat.getColor(AppHelpers.currentContext, android.R.color.transparent));
        b.setDescription("");
        b.setDrawOrder(new CombinedChart.DrawOrder[]
                {CombinedChart.DrawOrder.BAR, CombinedChart.DrawOrder.BUBBLE, CombinedChart.DrawOrder.CANDLE,
                        CombinedChart.DrawOrder.LINE, CombinedChart.DrawOrder.SCATTER});
        b.animateXY(1000, 1000);

        x = b.getXAxis();
        x.setTextSize(6f);

        y = b.getAxisLeft();
        y.setTextSize(6f);
        y.setStartAtZero(true);
        y.setGridColor(AppHelpers.generateFactorChartColor(order));

        // disable right side y-values
        y = b.getAxisRight();
        y.setEnabled(false);

        chartOrder.put(key, order);

        return view;
    }

    public static View generateFactorBarChart(String key, int order) {
        View view = AppHelpers.factory.inflate(R.layout.data_factor_barchart, null);
        BarChart b = (BarChart) view.findViewById(R.id.data_factorchart);
        TextView title = (TextView) view.findViewById(R.id.data_factor_title);
        LinearLayout topColor = (LinearLayout) view.findViewById(R.id.data_factorchart_top_color);
        topColor.setBackgroundColor(AppHelpers.generateFactorChartColor(order));

        title.setText(Factor.keyToName(key));

        MainActivity.factorCharts.put(key, b);
        MainActivity.factorViews.put(key, view);

        b.setGridBackgroundColor(ContextCompat.getColor(AppHelpers.currentContext, android.R.color.transparent));
        b.setDescription("");
        b.animateXY(1000, 1000);

        x = b.getXAxis();
        x.setTextSize(6f);

        y = b.getAxisLeft();
        y.setTextSize(6f);
        y.setStartAtZero(true);
        y.setGridColor(AppHelpers.generateFactorChartColor(order));

        // disable right side y-values
        y = b.getAxisRight();
        y.setEnabled(false);

        chartOrder.put(key, order);

        return view;
    }


    public static BarData generateFactorBarChartData(String key, int order) {
        Calendar cal = Calendar.getInstance();
        timeSlots = generateTimeSlots();
        HashMap<Integer, ArrayList<String>> values = new HashMap<>();
        String cTime = "";
        for (Condition c : DatabaseStorage.values.keySet()) {
            if (c.key.equals(key)) {
                cal.setTimeInMillis(c.timestamp);
                switch (AppHelpers.calTime) {
                    case Calendar.HOUR_OF_DAY:
                        cal.set(Calendar.MINUTE, 0);
                        cTime = AppHelpers.hourFormat.format(cal.getTime());
                        break;
                    case Calendar.DAY_OF_YEAR:
                        cTime = AppHelpers.dayFormat.format(cal.getTime());
                        break;
                    case Calendar.WEEK_OF_YEAR:
                        cTime = AppHelpers.weekFormat.format(cal.getTime());
                        break;
                    case Calendar.MONTH:
                        cTime = AppHelpers.monthFormat.format(cal.getTime());
                        break;
                }
                for (Integer timeslot : timeSlots.keySet()) {
                    if (timeSlots.get(timeslot).equals(cTime)) {
                        if (values.containsKey(timeslot)) values.get(timeslot).add(DatabaseStorage.values.get(c).getValue());
                        else {
                            ArrayList<String> newEntry = new ArrayList<>();
                            newEntry.add(DatabaseStorage.values.get(c).getValue());
                            values.put(timeslot, newEntry);
                        }
                    }
                }
            }
        }

        ArrayList<BarEntry> barEntries = new ArrayList<>();
        ArrayList<String> entryNames = new ArrayList<>();
        ArrayList<String> entryValues;

        // map all possible values so each stack has correct coloring
        for (Integer index : values.keySet()) {
            ArrayList<String> point = values.get(index);
            for (String pointValues : point) {
                for (String value : pointValues.split(",")) {
                    value = value.trim();
                    if (!entryNames.contains(value)) {
                        entryNames.add(value);
                    }
                }
            }
        }
        Collections.sort(entryNames);
        Log.d("entrynames", entryNames.toString());

        for (Integer index : values.keySet()) {
            barEntries = new ArrayList<>();
            ArrayList<String> point = values.get(index);

            // construct a normalized stacked bar entry for each column
            for (String pointValues : point) {
                entryValues = new ArrayList<>();
                for (String value : pointValues.split(",")) {
                    value = value.trim();
                    entryValues.add(value);
                }
                float[] xVal = new float[entryNames.size()];
                float fractionValue = 1;
                for (int nameIndex = 0; nameIndex < entryNames.size(); nameIndex++) {
                    if (entryValues.contains(entryNames.get(nameIndex))) {
                        xVal[nameIndex] = 1f / entryValues.size();
                        fractionValue++;
                        Log.d("value", "found " + fractionValue + " : " + entryValues.size());
                    }
                    else {
                        Log.d("value", "not found 0");
                        xVal[nameIndex] = 0f;
                    }
                }
                BarEntry entry = new BarEntry(xVal, index);
                Log.d("new entry", entry.toString() + " val: " + entry.getVal());
                barEntries.add(entry);
            }
        }

        BarDataSet b = new BarDataSet(barEntries, "");
        b.setDrawValues(false);
        b.setColors(AppHelpers.generateColorList());
        String[] labels = new String[entryNames.size()];
        int i = 0;
        for (String name : entryNames) {
            labels[i] = name;
            i++;
        }
        b.setStackLabels(labels);

        ArrayList<String> xVals = new ArrayList<>();
        for (String time : timeSlots.values()) xVals.add(time);

        BarData factorBarData = new BarData(xVals);
        factorBarData.addDataSet(b);

        return factorBarData;
    }

    public static CombinedData generateFactorComboChartData(String key, int order) {
        Calendar cal = Calendar.getInstance();
        timeSlots = generateTimeSlots();
        ArrayList<Float> avg = new ArrayList<>();
        HashMap<Integer, ArrayList<Float>> values = new HashMap<>();
        String cTime = "";
        for (Condition c : DatabaseStorage.values.keySet()) {
            if (c.key.equals(key)) {
                avg.add(DatabaseStorage.values.get(c).getNumericValue());
                cal.setTimeInMillis(c.timestamp);
                switch (AppHelpers.calTime) {
                    case Calendar.HOUR_OF_DAY:
                        cal.set(Calendar.MINUTE, 0);
                        cTime = AppHelpers.hourFormat.format(cal.getTime());
                        break;
                    case Calendar.DAY_OF_YEAR:
                        cTime = AppHelpers.dayFormat.format(cal.getTime());
                        break;
                    case Calendar.WEEK_OF_YEAR:
                        cTime = AppHelpers.weekFormat.format(cal.getTime());
                        break;
                    case Calendar.MONTH:
                        cTime = AppHelpers.monthFormat.format(cal.getTime());
                        break;
                }
                for (Integer timeslot : timeSlots.keySet()) {
                    if (timeSlots.get(timeslot).equals(cTime)) {
                        if (values.containsKey(timeslot)) values.get(timeslot).add(DatabaseStorage.values.get(c).getNumericValue());
                        else {
                            ArrayList<Float> newEntry = new ArrayList<>();
                            newEntry.add(DatabaseStorage.values.get(c).getNumericValue());
                            values.put(timeslot, newEntry);
                        }
                    }
                }
            }
        }

        ArrayList<String> xVals = new ArrayList<>();
        for (String time : timeSlots.values()) xVals.add(time);

        CombinedData result = new CombinedData(xVals);

        Float lineAverage = calculateAverage(avg);

        ArrayList<Entry> averageLineEntries = new ArrayList<>();
        averageLineEntries.add(new Entry(lineAverage, -1));
        averageLineEntries.add(new Entry(lineAverage, timeSlots.size()));
        LineDataSet lineSet = new LineDataSet(averageLineEntries, "Overall average");
        lineSet.setColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.colorPrimaryDark));
        lineSet.setLineWidth(2f);
        lineSet.setCircleSize(0f);
        lineSet.setDrawValues(false);

        LineData averageLine = new LineData();
        averageLine.addDataSet(lineSet);
        result.setData(averageLine);

        ArrayList<BarEntry> barEntries = new ArrayList<>();
        for (Integer index : values.keySet()) {
            barEntries.add(new BarEntry(calculateAverage(values.get(index)), index));
        }
        BarDataSet barSet = new BarDataSet(barEntries, Factor.keyToName(key));
        barSet.setDrawValues(false);
        barSet.setColor(AppHelpers.generateFactorChartColor(order));

        BarData factorBarData = new BarData();
        factorBarData.addDataSet(barSet);
        result.setData(factorBarData);

        return result;
    }

    private static TreeMap<Integer, String> generateTimeSlots() {
        TreeMap<Integer, String> result = new TreeMap<>();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.MINUTE, 0);
        switch (AppHelpers.calTime) {
            // last 12 hours
            case Calendar.HOUR_OF_DAY:
                // in chunks of 12 hours
                c.add(Calendar.HOUR, -AppHelpers.dayOffset*12);
                result.put(11, AppHelpers.hourFormat.format(c.getTime()));
                for (int hour : new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))) {
                    // latest is on the right, so start from last index and remove hours
                    c.add(Calendar.HOUR_OF_DAY, -1);
                    result.put(11-hour, AppHelpers.hourFormat.format(c.getTime()));
                }
                break;
            // last 7 days
            case Calendar.DAY_OF_YEAR:
                c.add(Calendar.DAY_OF_YEAR, -AppHelpers.weekOffset*7);
                result.put(6, AppHelpers.dayFormat.format(c.getTime()));
                for (int day : new ArrayList<>(Arrays.asList(1,2,3,4,5,6))) {
                    c.add(Calendar.DAY_OF_YEAR, -1);
                    result.put(6 - day, AppHelpers.dayFormat.format(c.getTime()));
                }
                break;
            // last 4 weeks
            case Calendar.WEEK_OF_YEAR:
                c.add(Calendar.WEEK_OF_YEAR, -AppHelpers.monthOffset*4);
                result.put(3, AppHelpers.weekFormat.format(c.getTime()));
                for (int week : new ArrayList<>(Arrays.asList(1,2,3))) {
                    c.add(Calendar.WEEK_OF_YEAR, -1);
                    result.put(3-week, AppHelpers.weekFormat.format(c.getTime()));
                }
                break;
            // last 6 month
            case Calendar.MONTH:
                c.add(Calendar.YEAR, -AppHelpers.yearOffset);
                result.put(11, AppHelpers.monthFormat.format(c.getTime()));
                for (int month : new ArrayList<>(Arrays.asList(1,2,3,4,5,6,7,8,9,10,11))) {
                    c.add(Calendar.MONTH, -1);
                    result.put(11-month, AppHelpers.monthFormat.format(c.getTime()));
                }
                break;

        }

        return result;
    }

    private static Integer fetchTimeSlot(String dateAsString) {
        for (Integer index : timeSlots.keySet()) {
            if (timeSlots.get(index).equals(dateAsString)) return index;
        }
        return -1;
    }

    private static float calculateAverage(List<Float> marks) {
        float sum = 0;
        if(!marks.isEmpty()) {
            for (Float mark : marks) {
                sum += mark;
            }
            return sum / marks.size();
        }
        return sum;
    }
}
