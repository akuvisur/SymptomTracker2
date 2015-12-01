package com.comag.aku.symptomtracker.model;

import android.util.Log;
import android.widget.Toast;

import com.comag.aku.symptomtracker.MainActivity;
import com.comag.aku.symptomtracker.Settings;
import com.comag.aku.symptomtracker.objects.Schema;
import com.comag.aku.symptomtracker.objects.Symptom;
import com.comag.aku.symptomtracker.objects.ValueMap;
import com.comag.aku.symptomtracker.objects.tracking.Condition;
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
import java.util.Random;
import java.util.Set;

/**
 * Created by aku on 28/10/15.
 */
public class DatabaseStorage {
    public static List<Schema> schemaList = new ArrayList<Schema>();
    public static Schema schema;

    public static HashMap<Condition, ValueMap> values = new HashMap<Condition, ValueMap>();

    public static void fetchAll() {
        NoSQLStorage.loadAllValues();
    }


}
