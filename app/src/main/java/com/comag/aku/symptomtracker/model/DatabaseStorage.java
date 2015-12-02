package com.comag.aku.symptomtracker.model;

import com.comag.aku.symptomtracker.objects.Schema;
import com.comag.aku.symptomtracker.objects.ValueMap;
import com.comag.aku.symptomtracker.objects.tracking.Condition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
