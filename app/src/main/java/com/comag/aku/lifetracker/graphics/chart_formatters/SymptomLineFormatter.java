package com.comag.aku.lifetracker.graphics.chart_formatters;

import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.YAxisValueFormatter;

/**
 * Created by aku on 17/11/15.
 */
public class SymptomLineFormatter implements YAxisValueFormatter {
    @Override
    public String getFormattedValue(float value, YAxis yAxis) {
        if (value == 0.0) return "Missing";
        else if (value == 1.0) return "None/Low";
        else if (value == 2.0) return "Mild/Med";
        else if (value == 3.0) return "Severe/high";
        return "";
    }
}
