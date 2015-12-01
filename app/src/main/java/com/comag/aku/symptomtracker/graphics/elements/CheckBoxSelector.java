package com.comag.aku.symptomtracker.graphics.elements;

import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import com.comag.aku.symptomtracker.MainActivity;
import com.comag.aku.symptomtracker.R;
import com.comag.aku.symptomtracker.Settings;
import com.comag.aku.symptomtracker.app_settings.AppPreferences;
import com.comag.aku.symptomtracker.graphics.FactorDataViewer;
import com.comag.aku.symptomtracker.objects.Factor;
import com.comag.aku.symptomtracker.objects.Symptom;

/**
 * Created by aku on 12/11/15.
 */
public class CheckBoxSelector {
    public static View getSymptomSelector() {
        View view = Settings.factory.inflate(R.layout.empty_horizontal_linear, null);

        LinearLayout container = (LinearLayout) view.findViewById(R.id.container);
        //container.removeAllViews();

        CheckBox c;
        for (String symptom : AppPreferences.symptoms.keySet()) {
            c = new CheckBox(Settings.currentContext);
            c.setOnClickListener(new SymptomKeyListener(symptom, c));
            c.setText(Symptom.keyToName(symptom));
            c.setTextSize(12);
            c.setTextColor(ContextCompat.getColor(Settings.currentContext, R.color.black));
            c.setChecked(MainActivity.dataSymptoms.contains(symptom));
            container.addView(c);
        }

        return view;
    }

    public static View getFactorSelector() {
        View view = Settings.factory.inflate(R.layout.empty_horizontal_linear, null);

        LinearLayout container = (LinearLayout) view.findViewById(R.id.container);
        //container.removeAllViews();

        CheckBox c;
        for (String factor : AppPreferences.factors.keySet()) {
            c = new CheckBox(Settings.currentContext);
            c.setOnClickListener(new FactorKeyListener(factor, c));
            c.setText(Factor.keyToName(factor));
            c.setTextSize(12);
            c.setTextColor(ContextCompat.getColor(Settings.currentContext, R.color.black));
            c.setChecked(MainActivity.dataFactors.contains(factor));
            container.addView(c);
        }

        return view;
    }

    private static class SymptomKeyListener implements View.OnClickListener {
        private String key;
        private CheckBox _this;
        public SymptomKeyListener(String key, CheckBox c) {this.key=key;_this = c;}

        public void onClick(View v) {
            Log.d("click", key);
            if (MainActivity.dataSymptoms.contains(key)) {
                Log.d("removed", key);
                MainActivity.dataSymptoms.remove(key);
                _this.setChecked(false);
            }
            else if (!MainActivity.dataSymptoms.contains(key)) {
                Log.d("added", key);
                MainActivity.dataSymptoms.add(key);
                _this.setChecked(true);
            }
            MainActivity.chartChanged();
        }
    }

    private static class FactorKeyListener implements View.OnClickListener {
        private String key;
        private CheckBox _this;
        public FactorKeyListener(String key, CheckBox c) {this.key=key;_this = c;}

        public void onClick(View v) {
            //Log.d("click", key);
            if (MainActivity.dataFactors.contains(key)) {
                //Log.d("removed", key);
                MainActivity.dataFactors.remove(key);
                _this.setChecked(false);
                MainActivity.scrollContainer.removeView(MainActivity.factorViews.get(key));
            }
            else if (!MainActivity.dataFactors.contains(key)) {
                //Log.d("added", key);
                MainActivity.dataFactors.add(key);
                _this.setChecked(true);
                if (AppPreferences.factors.get(key).input.equals("tracked"))
                    MainActivity.scrollContainer.addView(FactorDataViewer.generateFactorCombinedChart(key, (int) Math.random() * 6));
                else
                    MainActivity.scrollContainer.addView(FactorDataViewer.generateFactorBarChart(key, (int) Math.random() * 6));
            }
            MainActivity.chartChanged();
        }
    }
}
