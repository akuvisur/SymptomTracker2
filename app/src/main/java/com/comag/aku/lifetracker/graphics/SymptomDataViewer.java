package com.comag.aku.lifetracker.graphics;

import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import com.comag.aku.lifetracker.AppHelpers;
import com.comag.aku.lifetracker.MainActivity;
import com.comag.aku.lifetracker.R;
import com.comag.aku.lifetracker.app_settings.AppPreferences;
import com.comag.aku.lifetracker.graphics.chart_formatters.SymptomLineFormatter;
import com.comag.aku.lifetracker.graphics.chart_formatters.XAxisTimeFormatter;
import com.comag.aku.lifetracker.graphics.listeners.OnSwipeTouchListener;
import com.comag.aku.lifetracker.model.DatabaseStorage;
import com.comag.aku.lifetracker.objects.Symptom;
import com.comag.aku.lifetracker.objects.tracking.Condition;
import com.comag.aku.lifetracker.objects.tracking.ConditionMap;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.BubbleData;
import com.github.mikephil.charting.data.BubbleDataSet;
import com.github.mikephil.charting.data.BubbleEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by aku on 10/11/15.
 */
public class SymptomDataViewer {

    public static ArrayList<ConditionMap> dataValues;
    private static BubbleData symptomChartData;
    public static HashMap<String, Float> symptomNames;
    private static TreeMap<Integer, String> symptomTimeSlots;

    public static BubbleData generateSymptomChartData() {
        dataValues = new ArrayList<>();

        parseSymptomData();
        Collections.sort(dataValues, new TimeComparator());
        parseSymptomChartValues();

        return symptomChartData;
    }

    private static void parseSymptomData() {
        // group values together based on selected grouping (calTime)
        mapSymptomValuesToTimeslots();

        // symptom names map (name of app, "value" on y-axis)
        symptomNames = parseSymptomNames();

        // timeframes, (Date as string, index on x-axis)
        symptomTimeSlots = parseSymptomTimeslots();
    }

    private static HashMap<String, Float> parseSymptomNames() {
        //MainActivity.dataSymptoms.clear();
        HashMap<String, Float> result = new HashMap<>();
        int valueIndex = 1;
        for (ConditionMap m : dataValues) {
            if (!result.keySet().contains(m.key) && MainActivity.dataSymptoms.contains(m.key)) {
                result.put(m.key, Float.valueOf(valueIndex));
                //MainActivity.dataSymptoms.add(m.key);
                valueIndex++;
            }
        }
        return result;
    }

    private static TreeMap<Integer, String> parseSymptomTimeslots() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.MINUTE, 0);
        TreeMap<Integer, String> result = new TreeMap<>();
        switch (AppHelpers.calTime) {
            // last 12 hours
            case Calendar.HOUR_OF_DAY:
                // in chunks of 12 hours
                c.add(Calendar.HOUR, -AppHelpers.dayOffset*12);
                result.put(11, AppHelpers.hourFormat.format(c.getTime()));
                for (int hour : new ArrayList<>(Arrays.asList(1,2,3,4,5,6,7,8,9,10,11))) {
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

    public static HashMap<String, Integer> chartSymptomColors = new HashMap<>();
    private static void parseSymptomChartValues() {
        Calendar c = Calendar.getInstance();
        HashMap<String, ArrayList<BubbleEntry>> rows = new HashMap<>();
        ArrayList<BubbleDataSet> data = new ArrayList<>();

        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.MINUTE, 0);

        int timeslot;
        for (ConditionMap value : dataValues) {
            if (!MainActivity.dataSymptoms.contains(value.key)) continue;
            if (!rows.keySet().contains(value.key)) {
                rows.put(value.key, new ArrayList<BubbleEntry>());
            }
            switch (AppHelpers.calTime) {
                case Calendar.HOUR_OF_DAY:
                    c.setTimeInMillis(System.currentTimeMillis());
                    int curday = c.get(Calendar.DAY_OF_YEAR);
                    c.setTimeInMillis(value.timestamp);
                    c.set(Calendar.MINUTE, 0);
                    if (c.get(Calendar.DAY_OF_YEAR) != curday) break;
                    timeslot = fetchTimeSlot(AppHelpers.hourFormat.format(c.getTime()));
                    if (timeslot >= 0) rows.get(value.key).add(
                            new BubbleEntry(timeslot,
                                    symptomNames.get(value.key),
                                    value.getValue(),
                                    value));
                    break;
                case Calendar.DAY_OF_YEAR:
                    c.setTimeInMillis(value.timestamp);
                    timeslot = fetchTimeSlot(AppHelpers.dayFormat.format(c.getTime()));
                    if (timeslot >= 0) rows.get(value.key).add(
                        new BubbleEntry(timeslot,
                            symptomNames.get(value.key),
                            value.getValue(),
                            value));
                    break;
                case Calendar.WEEK_OF_YEAR:
                    c.setTimeInMillis(value.timestamp);
                    timeslot = fetchTimeSlot(AppHelpers.weekFormat.format(c.getTime()));
                    if (timeslot >= 0)
                        rows.get(value.key).add(
                            new BubbleEntry(timeslot,
                                symptomNames.get(value.key),
                                value.getValue(),
                                value));
                    break;
                case Calendar.MONTH:
                    c.setTimeInMillis(value.timestamp);
                    timeslot = fetchTimeSlot(AppHelpers.monthFormat.format(c.getTime()));
                    if (timeslot >= 0)
                        rows.get(value.key).add(
                            new BubbleEntry(timeslot,
                                symptomNames.get(value.key),
                                value.getValue(),
                                value));
                    break;
            }
        }

        ArrayList<String> xLabels = new ArrayList<>();
        for (String date : symptomTimeSlots.values()) {
            xLabels.add(date);
        }
        BubbleDataSet row;
        int rowIndex = 0;

        for (String label : rows.keySet()) {
            row = new ModifiedBubbleDataSet(rows.get(label), Symptom.keyToName(label));
            chartSymptomColors.put(label, AppHelpers.randomizeListColor(rowIndex));
            row.setColor(AppHelpers.randomizeListColor(rowIndex), 130);
            row.setHighLightColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.colorPrimaryDark));
            // dont draw text values for entries
            row.setDrawValues(false);
            if (MainActivity.dataSymptoms.contains(label)) {
                data.add(row);
                rowIndex++;
            }
        }
        for (BubbleDataSet ds : data) {
            Log.d("bubblesize", "yvals: " + ds.getYVals() + " name: " + ds.getLabel());
        }
        symptomChartData = new BubbleData(xLabels, data);
        //symptomChartData.setValueFormatter(new ChartSymptomValueFormatter());
        symptomChartData.setHighlightEnabled(true);
        symptomChartData.setHighlightCircleWidth(5f);

    }

    private static Integer fetchTimeSlot(String dateAsString) {
        for (Integer index : symptomTimeSlots.keySet()) {
            if (symptomTimeSlots.get(index).equals(dateAsString)) return index;
        }
        return -1;
    }

    private static void mapSymptomValuesToTimeslots() {
        for (Condition c : DatabaseStorage.values.keySet()) {
            if (!c.key.contains("symptom_")) {continue;}

            ConditionMap entry = new ConditionMap(c.key, c.hour, c.window, c.year, c.day, c.timestamp);
            if (!dataValues.contains(entry)) {
                //Log.d("entry", "not found");
                entry.put(DatabaseStorage.values.get(c));
                dataValues.add(entry);
            }
            else {
                //Log.d("entry", "should be there");
                findExisting:
                for (int i = 0; i < dataValues.size(); i++) {
                    if (dataValues.get(i).equals(entry)) {
                        dataValues.get(i).put(DatabaseStorage.values.get(c));
                        break findExisting;
                    }
                }
            }
        }
    }

    public static HashMap<String, LineChart> symptomLine = new HashMap<>();
    public static BarChart symptomBar;
    private static HashMap<String, LinearLayout> symptomDetailContainers = new HashMap<>();
    private static Animation detailAnim;
    private static YAxis y;
    private static XAxis x;
    private static Legend l;
    public static ArrayList<String> showingSymptoms = new ArrayList<>();
    public static void showSymptomInfo(final ConditionMap value) {
        if (!showingSymptoms.contains(value.key)) {
            showingSymptoms.add(value.key);
            final View symInfo = AppHelpers.factory.inflate(R.layout.symptom_detail, null);
            LinearLayout background = (LinearLayout) symInfo.findViewById(R.id.symptom_detail_background);
            background.setBackgroundColor(chartSymptomColors.get(value.key));

            symptomDetailContainers.put(value.key, (LinearLayout) symInfo.findViewById(R.id.symptom_detail_extracontainer));

            symInfo.setOnTouchListener(new OnSwipeTouchListener(AppHelpers.currentContext) {
                @Override
                public void onSwipeRight() {
                    detailAnim = AnimationUtils.loadAnimation(AppHelpers.currentContext, android.R.anim.slide_out_right);
                    detailAnim.setDuration(250);
                    symInfo.startAnimation(detailAnim);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.extraContainer.removeView(symInfo);
                            symptomLine.remove(value.key);
                            showingSymptoms.remove(value.key);
                            showingDetail.remove(value.key);
                        }
                    }, 250);
                }

                @Override
                public void onSwipeLeft() {
                    detailAnim = AnimationUtils.loadAnimation(AppHelpers.currentContext, R.anim.anim_out_left);
                    detailAnim.setDuration(250);
                    symInfo.startAnimation(detailAnim);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.extraContainer.removeView(symInfo);
                            symptomLine.remove(value.key);
                            showingSymptoms.remove(value.key);
                            showingDetail.remove(value.key);
                        }
                    }, 250);
                }
            });

            LineChart lc = (LineChart) symInfo.findViewById(R.id.symptom_detail_line);
            lc.setOnTouchListener(null);
            lc.animateX(1500, Easing.EasingOption.EaseInQuad);
            lc.setGridBackgroundColor(ContextCompat.getColor(AppHelpers.currentContext, android.R.color.transparent));
            lc.setDescription(Symptom.keyToName(value.key) + ": average over 14 days");
            lc.setDescriptionColor(chartSymptomColors.get(value.key));
            lc.setDescriptionPosition(450, 25);

            y = lc.getAxisLeft();
            y.setValueFormatter(new SymptomLineFormatter());
            y.setTextSize(9f);
            y.setTextColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.Symptom));
            y.setSpaceTop(5f);
            y.setAxisMinValue(0f);
            y.setAxisMaxValue(3.15f);
            y.setEnabled(true);

            y = lc.getAxisRight();
            y.setEnabled(false);
            l = lc.getLegend();
            l.setEnabled(false);

            x = lc.getXAxis();
            x.setDrawLabels(false);
            x.setLabelRotationAngle(60);
            x.setPosition(XAxis.XAxisPosition.BOTTOM);

            symptomLine.put(value.key, lc);

            symptomBar = (BarChart) symInfo.findViewById(R.id.symptom_detail_bar);
            symptomBar.setGridBackgroundColor(ContextCompat.getColor(AppHelpers.currentContext, android.R.color.transparent));

            symptomBar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showSymptomDistributionDetail(value);
                }
            });
            symptomBar.setDescription("Overall input distribution:");
            symptomBar.setDescriptionTextSize(8f);
            symptomBar.setDescriptionColor(chartSymptomColors.get(value.key));
            symptomBar.setDescriptionPosition(290, 25);
            symptomBar.animateY(1000, Easing.EasingOption.EaseInOutQuad);

            y = symptomBar.getAxisRight();
            y.setEnabled(false);
            y = symptomBar.getAxisLeft();
            y.setEnabled(false);

            x = symptomBar.getXAxis();
            x.setEnabled(false);

            l = symptomBar.getLegend();
            l.setEnabled(false);

            setSymptomInfoData(value);
            setSymptomDistributionBarData(value);

            //setPieData(value);

            MainActivity.extraContainer.addView(symInfo);
        }
        else {
            setSymptomInfoData(value);
            symptomLine.get(value.key).invalidate();
        }
    }

    public static ConditionMap selectedChartSymptom;

    private static void setSymptomInfoData(ConditionMap value) {
        Calendar cal = Calendar.getInstance();
        Calendar entryCal = Calendar.getInstance();
        Calendar curCal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DAY_OF_YEAR, -13);
        TreeMap<Integer, ArrayList<Float>> averagedValues = new TreeMap<>();
        ArrayList<Entry> yVals = new ArrayList<>();
        ArrayList<String> xVals = new ArrayList<>();
        ArrayList<Entry> yHighlight = new ArrayList<Entry>();
        // set items
        for (int x = 0; x < 14; x++) {
            boolean found = false;
            xVals.add(AppHelpers.dayFormat.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, 1);
            for (Condition c : DatabaseStorage.values.keySet()) {
                if (!c.key.equals(value.key)) continue;
                entryCal.setTimeInMillis(c.timestamp);
                Float row = DatabaseStorage.values.get(c).getNumericValue();
                if (entryCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)) {
                    if (!averagedValues.containsKey(entryCal.get(Calendar.DAY_OF_YEAR))) {
                        ArrayList<Float> entryRow = new ArrayList<>();
                        entryRow.add(DatabaseStorage.values.get(c).getNumericValue());
                        averagedValues.put(entryCal.get(Calendar.DAY_OF_YEAR), entryRow);
                    }
                    else {
                        averagedValues.get(entryCal.get(Calendar.DAY_OF_YEAR)).add(row);
                    }
                    found = true;
                    curCal.setTimeInMillis(c.timestamp);
                }

            }
            if (found) {
                float avg = calculateAverage(averagedValues.get(curCal.get(Calendar.DAY_OF_YEAR)));
                yVals.add(new Entry(avg, x));
            }
            else {
                yVals.add(new Entry(0f, x));
            }
            // reuse this here to check if the selected entry is for this index
            // have to include all values in order to animate it in the same order as "real" line
            curCal.setTimeInMillis(selectedChartSymptom.timestamp);
            if (selectedChartSymptom != null && curCal.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)) {
                yHighlight.add(new Entry(calculateAverage(averagedValues.get(curCal.get(Calendar.DAY_OF_YEAR))), x));
            }
            else yHighlight.add(new Entry(-2f, x));
        }

        ArrayList<LineDataSet> values = new ArrayList<>();
        LineDataSet l = new LineDataSet(yVals, "");
        l.setDrawValues(false);
        l.setColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.Symptom_lighter));
        l.setCircleColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.Symptom));

        LineDataSet highlight = new LineDataSet(yHighlight, "");
        highlight.setDrawValues(false);
        highlight.setColor(ContextCompat.getColor(AppHelpers.currentContext, android.R.color.transparent));
        highlight.setCircleColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.Symptom));
        highlight.setCircleSize(6f);

        values.add(l);
        values.add(highlight);

        LineData d = new LineData(xVals, values);
        symptomLine.get(value.key).setData(d);
    }

    private static void setSymptomDistributionBarData(final ConditionMap value) {
        ArrayList<BarEntry> yVals = new ArrayList<>();
        float none = 0, mild = 0, severe = 0;
        for (Condition c : DatabaseStorage.values.keySet()) {
            if (!c.key.equals(value.key)) continue;
            switch (DatabaseStorage.values.get(c).getValue()) {
                // negative range
                case "none":
                    none = none + 1;
                    break;
                case "mild":
                    mild = mild + 1;
                    break;
                case "severe":
                    severe = severe + 1;
                    break;
                // positive range
                case "low":
                    none = none + 1;
                    break;
                case "medium":
                    mild = mild + 1;
                    break;
                case "high":
                    severe = severe + 1;
                    break;
                default:
                    break;
            }
        }
        BarEntry n = new BarEntry(none, 0);
        BarEntry m = new BarEntry(mild, 1);
        BarEntry s = new BarEntry(severe, 2);
        yVals.add(n);
        yVals.add(m);
        yVals.add(s);
        BarDataSet p = new BarDataSet(yVals, "");

        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(ContextCompat.getColor(AppHelpers.currentContext, R.color.None));
        colors.add(ContextCompat.getColor(AppHelpers.currentContext, R.color.Mild));
        colors.add(ContextCompat.getColor(AppHelpers.currentContext, R.color.Severe));
        p.setColors(colors);
        p.setDrawValues(false);

        ArrayList<String> xVals = new ArrayList<>();
        xVals.add("None/Low");
        xVals.add("Mild/Medium");
        xVals.add("Severe/High");

        BarData pd = new BarData(xVals, p);

        symptomBar.setData(pd);

    }

    private static ArrayList<String> showingDetail = new ArrayList<>();
    private static void showSymptomDistributionDetail(final ConditionMap value) {
        if (!showingDetail.contains(value.key)) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(System.currentTimeMillis());
            int repWindow = Calendar.DAY_OF_YEAR;
            showingDetail.add(value.key);

            ArrayList<Integer> timeSlots = new ArrayList<>();
            HashMap<Integer, Float> noneCount = new HashMap<>();
            HashMap<Integer, Float> mildCount = new HashMap<>();
            HashMap<Integer, Float> severeCount = new HashMap<>();
            int timeOffset = 1;

            BarChart detail = new BarChart(AppHelpers.currentContext);
            switch (AppPreferences.symptoms.get(value.key).rep_window) {
                case "hour":
                    repWindow = Calendar.HOUR_OF_DAY;
                    timeOffset = 7;
                    for (int x = 7;x < 24; x++) timeSlots.add(x);
                    break;
                case "day":
                    repWindow = Calendar.DAY_OF_WEEK;
                    for (int x = 1; x < 8; x++) timeSlots.add(x);
                    break;
                case "week":
                    repWindow = Calendar.WEEK_OF_MONTH;
                    for (int x = 1; x < 5; x++) timeSlots.add(x);
                    break;
                case "month":
                    repWindow = Calendar.MONTH;
                    for (int x = 1; x < 13; x++) timeSlots.add(x);
                    break;
            }
            Calendar entryCal = Calendar.getInstance();
            Float val;

            for (Integer time : timeSlots) {
                for (Condition c : DatabaseStorage.values.keySet()) {
                    entryCal.setTimeInMillis(c.timestamp);
                    if (c.key.equals(value.key) && time == entryCal.get(repWindow)) {
                        switch (DatabaseStorage.values.get(c).getValue()) {
                            // negative ranges
                            case "none":
                                if (noneCount.containsKey(time)) val = noneCount.get(time);
                                else val = 0f;
                                val = val + 1;
                                noneCount.put(time, val);
                                break;
                            case "mild":
                                if (mildCount.containsKey(time)) val = mildCount.get(time);
                                else val = 0f;
                                val = val + 1;
                                mildCount.put(time, val);
                                break;
                            case "severe":
                                if (severeCount.containsKey(time)) val = severeCount.get(time);
                                else val = 0f;
                                val = val + 1;
                                severeCount.put(time, val);
                                break;
                            // positive ranges
                            case "low":
                                if (noneCount.containsKey(time)) val = noneCount.get(time);
                                else val = 0f;
                                val = val + 1;
                                noneCount.put(time, val);
                                break;
                            case "medium":
                                if (mildCount.containsKey(time)) val = mildCount.get(time);
                                else val = 0f;
                                val = val + 1;
                                mildCount.put(time, val);
                                break;
                            case "high":
                                if (severeCount.containsKey(time)) val = severeCount.get(time);
                                else val = 0f;
                                val = val + 1;
                                severeCount.put(time, val);
                                break;
                        }
                    }
                }
            }

            ArrayList<BarEntry> entries = new ArrayList<>();
            for (Integer time : timeSlots) {
                float[] cVal = new float[3];
                float total;
                float none;
                float mild;
                float severe;
                if (noneCount.containsKey(time)) none = noneCount.get(time);
                else none = 0f;

                if (mildCount.containsKey(time)) mild = mildCount.get(time);
                else mild = 0f;

                if (severeCount.containsKey(time)) severe = severeCount.get(time);
                else severe = 0f;

                total = none + mild + severe;

                cVal[0] = none / total;
                cVal[1] = mild / total;
                cVal[2] = severe / total;

                entries.add(new BarEntry(cVal, time-timeOffset));
            }

            BarDataSet set = new BarDataSet(entries, "");
            List<Integer> colors = new ArrayList<>();
            colors.add(ContextCompat.getColor(AppHelpers.currentContext, R.color.None));
            colors.add(ContextCompat.getColor(AppHelpers.currentContext, R.color.Mild));
            colors.add(ContextCompat.getColor(AppHelpers.currentContext, R.color.Severe));

            set.setColors(colors);
            set.setStackLabels(new String[]{"None/Low", "Mild/Medium", "Severe/High"});
            set.setDrawValues(false);

            ArrayList<String> times = new ArrayList<>();
            for (Integer time : timeSlots) {
                times.add(time.toString());
            }

            BarData data = new BarData(times, set);

            detail.setData(data);
            detail.animateXY(1000, 1000);
            detail.setDescription("");
            detail.setGridBackgroundColor(ContextCompat.getColor(AppHelpers.currentContext, android.R.color.white));
            detail.setMinimumHeight(300);
            detail.setOnTouchListener(null);

            y = detail.getAxisLeft();
            y.setEnabled(false);
            y = detail.getAxisRight();
            y.setEnabled(false);

            x = detail.getXAxis();
            x.setTextSize(4f);
            x.setValueFormatter(new XAxisTimeFormatter(repWindow));
            x.setLabelRotationAngle(90);
            x.setPosition(XAxis.XAxisPosition.BOTTOM);

            l = detail.getLegend();
            l.setPosition(Legend.LegendPosition.RIGHT_OF_CHART);
            l.setTextSize(8f);
            l.setForm(Legend.LegendForm.SQUARE);
            l.setWordWrapEnabled(true);

            symptomDetailContainers.get(value.key).addView(detail);
        }
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


    private static class TimeComparator implements Comparator<ConditionMap> {
        @Override
        public int compare(ConditionMap lhs, ConditionMap rhs) {
            if (lhs.timestamp < rhs.timestamp) {
                return -1;
            }
            else if (lhs.timestamp == rhs.timestamp) {
                return 0;
            }
            else return 1;
            //return lhs.timestamp.compareTo(rhs.timestamp);
        }
    }


    private static class ModifiedBubbleDataSet extends BubbleDataSet {

        public ModifiedBubbleDataSet(List<BubbleEntry> yVals, String label) {
            super(yVals, label);
        }

        @Override
        protected void calcMinMax(int start, int end) {
            super.calcMinMax(start, end);
            // set max size to slightly bigger than maxvalue(3) so the edges dont touch
            mMaxSize = 3.1f;
        }

    }

}
