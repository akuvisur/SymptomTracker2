package com.comag.aku.lifetracker.graphics.listeners;

import android.os.Handler;
import android.util.Log;
import android.view.View;

import com.comag.aku.lifetracker.MainActivity;
import com.comag.aku.lifetracker.graphics.SymptomDataViewer;
import com.comag.aku.lifetracker.objects.tracking.ConditionMap;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

/**
 * Created by aku on 10/11/15.
 */
public class BubbleSelectorListener implements OnChartValueSelectedListener {
    private View singleView;
    private ConditionMap value;

    private boolean selecting = false;

    @Override
    public void onValueSelected(final Entry e, int dataSetIndex, Highlight h) {
        value = (ConditionMap) e.getData();
        SymptomDataViewer.selectedChartSymptom = value;
        Log.d("index", ":" + dataSetIndex);
        if (!selecting) {
            selecting = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SymptomDataViewer.showSymptomInfo(value);
                    selecting = false;
                }
            }, 250);
        }
    }

    @Override
    public void onNothingSelected() {
        MainActivity.extraContainer.removeAllViews();
    }

    public void deSelect() {
        onNothingSelected();
    }
}
