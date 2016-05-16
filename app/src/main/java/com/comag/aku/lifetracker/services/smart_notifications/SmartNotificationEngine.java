//Jan 22
package com.comag.aku.lifetracker.services.smart_notifications;

import android.database.Cursor;
import android.util.Log;

import com.comag.aku.lifetracker.data_syncronization.Plugin;
import com.comag.aku.lifetracker.data_syncronization.SyncProvider;
import com.comag.aku.lifetracker.services.UserContextService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instances;

//for machine learning


/**
 *  Copyright Chu Luo
 */

public class SmartNotificationEngine {
    private static final String LOG = "SmartNotificationEngine";
    public enum Prediction {
        ML_YES,
        ML_NO,
        ML_UNDEFINED
    }

    private static Prediction NAIVE_BAYES_PREDICTOR = Prediction.ML_UNDEFINED;
    private static Prediction C49_PREDICTOR = Prediction.ML_UNDEFINED;
    public static Prediction getNaiveBayesPredictor() {
        return NAIVE_BAYES_PREDICTOR;
    }
    public static Prediction getC49Predictor() {
        return C49_PREDICTOR;
    }
    private static Integer contextSize;
    public static Integer getContextSize() {
        if (contextSize == null) return -1;
        else return contextSize;
    }
    public static String enumToString(Prediction p) {
        switch (p) {
            case ML_NO:
                return "no";
            case ML_YES:
                return "yes";
            case ML_UNDEFINED:
                return "undefined";
            default:
                return "undefined";
        }
    }

    // can be enabled after future client update
    public static boolean isEnabled() {
        return true;
    }

    private static final int MINIMUM_CONTEXT_SIZE = 5;

    static JSONObject userContext;
    public static boolean emitNow() {
        Log.d(LOG, "emitnow");
        Double TreeResult =0d;
        long start=System.currentTimeMillis();

        // generate past context
        generatePastContext();

        // get current context
        userContext = UserContextService.getUserContext();

        if (pastContext.size() < 5) return true;

        try {
            //max training size
            final int size=2000;

            int actual_size=pastContext.size();
            contextSize = actual_size;

            //container for training data
            ArrayList<Attribute> atts = new ArrayList<Attribute>(14);
            ArrayList<String> classVal = new ArrayList<String>();
            classVal.add("ok");
            classVal.add("no");
            double[][] instanceValue1 = new double[size][14];

            // get context data from userContext JSONObject
            for (int i = 0; i < actual_size ; i++) {

                if(i==size)
                {
                    break;
                }
                try {
                    // get the doubles for past to NB
                    JSONObject o = pastContext.get(actual_size-1-i);
                    Double hour = o.getDouble("hour");
                    Double minute = o.getDouble("minute");
                    Double day_of_week = o.getDouble("day_of_week");
                    Double battery_level = o.getDouble("battery_level");
                    Double battery_charging = o.getDouble("battery_charging");
                    Double foreground_package = o.getDouble("foreground_package");
                    Double proximity = o.getDouble("proximity");
                    Double last_call = o.getDouble("last_call");
                    Double internet_available = o.getDouble("internet_available");
                    Double wifi_available = o.getDouble("wifi_available");
                    Double network_type = o.getDouble("network_type");
                    Double last_action = o.getDouble("last_action");
                    Double activity = o.getDouble("activity");
                    String valueString = o.getString("value");
                    int value=0;
                    if(valueString.equals("ok") || valueString.equals("app"))
                    {
                        value=1;
                    }
                    instanceValue1[i][0]=hour;
                    instanceValue1[i][1]=minute;
                    instanceValue1[i][2]=day_of_week;
                    instanceValue1[i][3]=battery_level;
                    instanceValue1[i][4]=battery_charging;
                    instanceValue1[i][5]=foreground_package;
                    instanceValue1[i][6]=proximity;
                    instanceValue1[i][7]=last_call;
                    instanceValue1[i][8]=internet_available;
                    instanceValue1[i][9]=wifi_available;
                    instanceValue1[i][10]=network_type;
                    instanceValue1[i][11]=last_action;
                    instanceValue1[i][12]=activity;
                    instanceValue1[i][13]=value;

                }
                catch (JSONException e) {
                    C49_PREDICTOR = Prediction.ML_UNDEFINED;
                    //Log.d("past_context", "object naive = "+i);
                }
            }

            // do training
            atts.add(new Attribute("1"));
            atts.add(new Attribute("2"));
            atts.add(new Attribute("3"));
            atts.add(new Attribute("4"));
            atts.add(new Attribute("5"));
            atts.add(new Attribute("6"));
            atts.add(new Attribute("7"));
            atts.add(new Attribute("8"));
            atts.add(new Attribute("9"));
            atts.add(new Attribute("10"));
            atts.add(new Attribute("11"));
            atts.add(new Attribute("12"));
            atts.add(new Attribute("13"));
            atts.add(new Attribute("class", classVal));
            Instances dataRaw = new Instances("TestInstances", atts, 0);

            for(int i=0;i<actual_size;i++) {
                dataRaw.add(new DenseInstance(1.0, instanceValue1[i]));
            }
            dataRaw.setClassIndex(dataRaw.numAttributes() - 1);


            Instances dataRaw2 = new Instances("EvalInstances", atts, 0);

            double[] instanceValue3 = new double[dataRaw2.numAttributes()];
            try {
                // get the doubles for current
                Double hour = userContext.getDouble("hour");
                //Log.d("past_context","134 hour = "+ hour);
                Double minute = userContext.getDouble("minute");
                Double day_of_week = userContext.getDouble("day");
                Double battery_level = userContext.getDouble("battery_level");
                Double battery_charging = userContext.getDouble("battery_charging");
                Double foreground_package = userContext.getDouble("foreground_package");
                Double proximity = userContext.getDouble("proximity");
                Double last_call = userContext.getDouble("last_call");
                Double internet_available = userContext.getDouble("internet_available");
                Double wifi_available = userContext.getDouble("wifi_available");
                Double network_type = userContext.getDouble("network_type");
                Double last_action = userContext.getDouble("last_action");
                Double activity = userContext.getDouble("activity");
                instanceValue3[0]=hour;
                instanceValue3[1]=minute;
                instanceValue3[2]=day_of_week;
                instanceValue3[3]=battery_level;
                instanceValue3[4]=battery_charging;
                instanceValue3[5]=foreground_package;
                instanceValue3[6]=proximity;
                instanceValue3[7]=last_call;
                instanceValue3[8]=internet_available;
                instanceValue3[9]=wifi_available;
                instanceValue3[10]=network_type;
                instanceValue3[11]=last_action;
                instanceValue3[12]=activity;

            }
            catch (JSONException e) {
                C49_PREDICTOR = Prediction.ML_UNDEFINED;
            }

            dataRaw2.add(new DenseInstance(1.0, instanceValue3));
            dataRaw2.setClassIndex(dataRaw2.numAttributes() - 1);

            String[] options = new String[1];

            //we use weka j48 here
            options[0] = "-U";
            J48 tree = new J48();
            tree.setOptions(options);
            tree.buildClassifier(dataRaw);
            TreeResult=tree.classifyInstance(dataRaw2.instance(0));

        }
        catch(Exception e) {
            e.printStackTrace();
            C49_PREDICTOR = Prediction.ML_UNDEFINED;
        }

        C49_PREDICTOR = (TreeResult > 0.1) ? Prediction.ML_YES : Prediction.ML_NO;

        Log.d("C49Tree", C49_PREDICTOR.toString());
        Log.d("C49Result", TreeResult.toString());
        Log.d("ContextSize", contextSize.toString());

        // prevent total lack of popups with contextSize of 100 or less
        if (C49_PREDICTOR == Prediction.ML_NO && contextSize < 100) {
            return (Math.random() * 100) < 10;
        }

        return TreeResult>0.1; //true if J48 says yes

    }

    private static SyncProvider sp;
    private static ArrayList<JSONObject> pastContext;
    public static void generatePastContext() {
        pastContext = new ArrayList<>();
        sp = new SyncProvider();

        String[] projection = {
                SyncProvider.NotificationEventData.CONTEXT,
                SyncProvider.NotificationEventData.VALUE
        };

        Cursor c = sp.query(Plugin.URI[1], projection, "value = ? OR value = ? OR value = ?", new String[] {"ok", "no", "app"}, null);
        if (c == null) return;
        while (c.moveToNext()) {
            // create a new list that contains all the sensors that are required
            ArrayList<String> curRequired = new ArrayList<>(UserContextService.required);
            try {
                JSONObject o = new JSONObject(c.getString(0));
                // add all missing keys as -1
                Iterator<String> keys = o.keys();
                while (keys.hasNext()) {
                    curRequired.remove(keys.next());
                }
                for (String key : curRequired) {
                    o.put(key, -1);
                }
                o.put("value", c.getString(1));
                pastContext.add(o);
                //Log.d("past_context_dump", o.toString());
            }
            catch (JSONException e) {
                Log.d("past_context", "error generating JSON from string: " + c.getString(0));
            }

        }
        c.close();
    }

}




