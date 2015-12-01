package com.comag.aku.symptomtracker.graphics;

import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.comag.aku.symptomtracker.MainActivity;
import com.comag.aku.symptomtracker.R;
import com.comag.aku.symptomtracker.Settings;
import com.comag.aku.symptomtracker.app_settings.AppPreferences;
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
import com.github.mikephil.charting.data.ChartData;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
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
        View view = Settings.factory.inflate(R.layout.data_factor_combochart, null);
        CombinedChart b = (CombinedChart) view.findViewById(R.id.data_factorchart);
        TextView title = (TextView) view.findViewById(R.id.data_factor_title);
        LinearLayout topColor = (LinearLayout) view.findViewById(R.id.data_factorchart_top_color);
        topColor.setBackgroundColor(Settings.generateFactorChartColor(order));

        title.setText(Factor.keyToName(key));

        MainActivity.factorCharts.put(key, b);
        MainActivity.factorViews.put(key, view);

        b.setData(generateFactorComboChartData(key, order));
        b.setGridBackgroundColor(ContextCompat.getColor(Settings.currentContext, android.R.color.transparent));
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
        y.setGridColor(Settings.generateFactorChartColor(order));

        // disable right side y-values
        y = b.getAxisRight();
        y.setEnabled(false);

        chartOrder.put(key, order);

        return view;
    }

    public static View generateFactorBarChart(String key, int order) {
        View view = Settings.factory.inflate(R.layout.data_factor_barchart, null);
        BarChart b = (BarChart) view.findViewById(R.id.data_factorchart);
        TextView title = (TextView) view.findViewById(R.id.data_factor_title);
        LinearLayout topColor = (LinearLayout) view.findViewById(R.id.data_factorchart_top_color);
        topColor.setBackgroundColor(Settings.generateFactorChartColor(order));

        title.setText(Factor.keyToName(key));

        MainActivity.factorCharts.put(key, b);
        MainActivity.factorViews.put(key, view);

        b.setGridBackgroundColor(ContextCompat.getColor(Settings.currentContext, android.R.color.transparent));
        b.setDescription("");
        b.animateXY(1000, 1000);

        x = b.getXAxis();
        x.setTextSize(6f);

        y = b.getAxisLeft();
        y.setTextSize(6f);
        y.setStartAtZero(true);
        y.setGridColor(Settings.generateFactorChartColor(order));

        // disable right side y-values
        y = b.getAxisRight();
        y.setEnabled(false);

        chartOrder.put(key, order);

        return view;
    }


    public static BarData generateFactorBarChartData(String key, int order) {
        Calendar cal = Calendar.getInstance();
        timeSlots = generateTimeSlots();
        ArrayList<Float> avg = new ArrayList<>();
        HashMap<Integer, ArrayList<Float>> values = new HashMap<>();
        String cTime = "";
        for (Condition c : DatabaseStorage.values.keySet()) {
            if (c.key.equals(key)) {
                avg.add(DatabaseStorage.values.get(c).getNumericValue());
                cal.setTimeInMillis(c.timestamp);
                switch (Settings.calTime) {
                    case Calendar.HOUR_OF_DAY:
                        cal.set(Calendar.MINUTE, 0);
                        cTime = Settings.hourFormat.format(cal.getTime());
                        break;
                    case Calendar.DAY_OF_YEAR:
                        cTime = Settings.dayFormat.format(cal.getTime());
                        break;
                    case Calendar.WEEK_OF_YEAR:
                        cTime = Settings.weekFormat.format(cal.getTime());
                        break;
                    case Calendar.MONTH:
                        cTime = Settings.monthFormat.format(cal.getTime());
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

        ArrayList<BarEntry> barEntries = new ArrayList<>();
        for (Integer index : values.keySet()) {
            barEntries.add(new BarEntry(calculateAverage(values.get(index)), index));
        }
        BarDataSet barSet = new BarDataSet(barEntries, Factor.keyToName(key));
        barSet.setDrawValues(false);
        barSet.setColor(Settings.generateFactorChartColor(order));

        BarData factorBarData = new BarData();
        factorBarData.addDataSet(barSet);

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
                switch (Settings.calTime) {
                    case Calendar.HOUR_OF_DAY:
                        cal.set(Calendar.MINUTE, 0);
                        cTime = Settings.hourFormat.format(cal.getTime());
                        break;
                    case Calendar.DAY_OF_YEAR:
                        cTime = Settings.dayFormat.format(cal.getTime());
                        break;
                    case Calendar.WEEK_OF_YEAR:
                        cTime = Settings.weekFormat.format(cal.getTime());
                        break;
                    case Calendar.MONTH:
                        cTime = Settings.monthFormat.format(cal.getTime());
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
        lineSet.setColor(ContextCompat.getColor(Settings.currentContext, R.color.colorPrimaryDark));
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
        barSet.setColor(Settings.generateFactorChartColor(order));

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
        switch (Settings.calTime) {
            // last 12 hours
            case Calendar.HOUR_OF_DAY:
                // in chunks of 12 hours
                c.add(Calendar.HOUR, -Settings.dayOffset*12);
                result.put(11, Settings.hourFormat.format(c.getTime()));
                for (int hour : new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11))) {
                    // latest is on the right, so start from last index and remove hours
                    c.add(Calendar.HOUR_OF_DAY, -1);
                    result.put(11-hour, Settings.hourFormat.format(c.getTime()));
                }
                break;
            // last 7 days
            case Calendar.DAY_OF_YEAR:
                c.add(Calendar.DAY_OF_YEAR, -Settings.weekOffset*7);
                result.put(6, Settings.dayFormat.format(c.getTime()));
                for (int day : new ArrayList<>(Arrays.asList(1,2,3,4,5,6))) {
                    c.add(Calendar.DAY_OF_YEAR, -1);
                    result.put(6 - day, Settings.dayFormat.format(c.getTime()));
                }
                break;
            // last 4 weeks
            case Calendar.WEEK_OF_YEAR:
                c.add(Calendar.WEEK_OF_YEAR, -Settings.monthOffset*4);
                result.put(3, Settings.weekFormat.format(c.getTime()));
                for (int week : new ArrayList<>(Arrays.asList(1,2,3))) {
                    c.add(Calendar.WEEK_OF_YEAR, -1);
                    result.put(3-week, Settings.weekFormat.format(c.getTime()));
                }
                break;
            // last 6 month
            case Calendar.MONTH:
                c.add(Calendar.YEAR, -Settings.yearOffset);
                result.put(11, Settings.monthFormat.format(c.getTime()));
                for (int month : new ArrayList<>(Arrays.asList(1,2,3,4,5,6,7,8,9,10,11))) {
                    c.add(Calendar.MONTH, -1);
                    result.put(11-month, Settings.monthFormat.format(c.getTime()));
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
