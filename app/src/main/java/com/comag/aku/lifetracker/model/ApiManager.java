package com.comag.aku.lifetracker.model;

import com.comag.aku.lifetracker.app_settings.AppPreferences;
import com.comag.aku.lifetracker.model.REST_queries.Factors;
import com.comag.aku.lifetracker.model.REST_queries.Schemas;
import com.comag.aku.lifetracker.model.REST_queries.Symptoms;

/**
 * Created by aku on 28/10/15.
 */
public class ApiManager {
    private static Schemas s;
    private static Symptoms sy;
    private static Factors fa;

    public static void getAllSchemas() {
        if (s == null) s = new Schemas();
        s.All();
    }

    public static void getSymptomsForSchema() {
        if (sy == null) sy = new Symptoms();
        sy.Keys(AppPreferences.getSchema().symptomKeys());
    }

    public static void getFactorsForSchema() {
        if (fa == null) fa = new Factors();
        fa.Keys(AppPreferences.getSchema().factorKeys());
    }


}
