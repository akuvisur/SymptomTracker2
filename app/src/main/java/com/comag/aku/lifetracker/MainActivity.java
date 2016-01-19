package com.comag.aku.lifetracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.aware.Aware;
import com.comag.aku.lifetracker.analytics.AnalyticsApplication;
import com.comag.aku.lifetracker.app_settings.AppPreferences;
import com.comag.aku.lifetracker.graphics.FactorDataViewer;
import com.comag.aku.lifetracker.graphics.LeaveStudy;
import com.comag.aku.lifetracker.graphics.NewSymptom;
import com.comag.aku.lifetracker.graphics.SettingsGeneratedSymptomRow;
import com.comag.aku.lifetracker.graphics.SymptomDataViewer;
import com.comag.aku.lifetracker.graphics.adapters.FactorRowAdapter;
import com.comag.aku.lifetracker.graphics.adapters.SymptomRowAdapter;
import com.comag.aku.lifetracker.graphics.elements.CheckBoxSelector;
import com.comag.aku.lifetracker.graphics.listeners.BubbleSelectorListener;
import com.comag.aku.lifetracker.graphics.listeners.OnSwipeTouchListener;
import com.comag.aku.lifetracker.model.ApiManager;
import com.comag.aku.lifetracker.model.DatabaseStorage;
import com.comag.aku.lifetracker.model.NoSQLStorage;
import com.comag.aku.lifetracker.model.data_storage.Values;
import com.comag.aku.lifetracker.objects.Factor;
import com.comag.aku.lifetracker.objects.ValueMap;
import com.comag.aku.lifetracker.objects.tracking.Condition;
import com.comag.aku.lifetracker.services.ApplicationMonitor;
import com.comag.aku.lifetracker.services.NotificationPreferences;
import com.comag.aku.lifetracker.services.NotificationService;
import com.comag.aku.lifetracker.services.UserContextService;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BubbleChart;
import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

/**
 * Created by aku on 29/10/15.
 */
public class MainActivity extends AppCompatActivity {
    public static String tab;

    public static Boolean onlyShowMissingSymptoms = false;
    public static Boolean onlyShowMissingFactors = false;

    private Tracker mTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppHelpers.currentActivity = this;
        AppHelpers.currentContext = getApplicationContext();
        AppHelpers.factory = LayoutInflater.from(this);
        AppHelpers.package_name = getPackageName();

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                ApiManager.getSymptomsForSchema();
                ApiManager.getFactorsForSchema();
            }
        });
        // Obtain the shared Google Analytics Tracker instance.
        AnalyticsApplication application = (AnalyticsApplication) getApplication();
        mTracker = application.getDefaultTracker();

        // start aware
        Intent aware = new Intent(this, Aware.class);
        startService(aware);

        // restart the service if coming from launch.class
        //Log.d("main", "starting contextservice");
        startService(new Intent(this, UserContextService.class));

        //Log.d("main", "starting notificationservice");
        startService(new Intent(this, NotificationService.class));

        startService(new Intent(this, ApplicationMonitor.class));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
         //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {showSettings();}
        else if (id == R.id.action_dataview) {showData();}
        else if (id == R.id.action_factors) {showFactors();}
        else if (id == R.id.action_symptoms) {showSymptoms();}
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsApplication.sendEvent("main_app", "resumed", null, null);
        AppHelpers.currentActivity = this;
        AppHelpers.currentContext = getApplicationContext();
        Log.d("main", "launched main");
        if(tab == null) tab = "data";
        switch(tab) {
            case "symptoms":
                showSymptoms();
                break;
            case "factors":
                showFactors();
                break;
            case "settings":
                showSettings();
                break;
            case "data":
                showData();
                break;
            default:
                showSymptoms();
                break;
        }

    }

    public static ListView symptom_list;
    SymptomRowAdapter symptomAdapter;
    public static Switch symptomSwitch;
    private void showSymptoms() {
        tab = "symptoms";

        mTracker.setScreenName(tab);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        NoSQLStorage.loadValues(tab);
        setContentView(R.layout.symptoms);

        Toolbar t = (Toolbar) findViewById(R.id.symptomstoolbar);
        t.setTitle("Symptoms");

        symptomSwitch = (Switch) findViewById(R.id.symptom_switch);
        symptomSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onlyShowMissingSymptoms = isChecked;
                ((SymptomRowAdapter) MainActivity.symptom_list.getAdapter()).notifyDataSetChanged();
            }
        });

        symptom_list = (ListView) findViewById(R.id.symptom_list);
        symptomAdapter = new SymptomRowAdapter(getApplicationContext(), R.layout.symptomrow, AppPreferences.symptomsAsList());
        symptom_list.setAdapter(symptomAdapter);
    }

    FactorRowAdapter factorAdapter;
    public static ListView factor_list;
    public static Switch factorSwitch;
    private void showFactors() {
        tab = "factors";

        mTracker.setScreenName(tab);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        NoSQLStorage.loadValues(tab);
        setContentView(R.layout.factors);

        Toolbar t = (Toolbar) findViewById(R.id.factorstoolbar);
        t.setTitle("Factors");

        factorSwitch = (Switch) findViewById(R.id.factor_switch);
        factorSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                onlyShowMissingFactors = isChecked;
                ((FactorRowAdapter) MainActivity.factor_list.getAdapter()).notifyDataSetChanged();
            }
        });

        factor_list = (ListView) findViewById(R.id.factor_list);
        factorAdapter = new FactorRowAdapter(getApplicationContext(), R.layout.factorrow, AppPreferences.factorsAsList());
        factor_list.setAdapter(factorAdapter);
    }

    SeekBar popupFreq;
    TextView popupFreqText;
    TextView notifTime;
    LinearLayout settingsHelp;
    CheckBox popupAutomated;
    EditText popupInterval;
    AlertDialog clockDialog;
    Snackbar settingsSnack;
    CheckBox dataSync;

    private static HashMap<String, View> generatedSymptomRows = new HashMap<>();
    private static LinearLayout generatedRowsContainer;

    private void showSettings() {
        setContentView(R.layout.settings);
        tab = "settings";

        mTracker.setScreenName(tab);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        Toolbar t = (Toolbar) findViewById(R.id.settingstoolbar);
        t.setTitle("Settings");

        AppPreferences.load();

        settingsHelp = (LinearLayout) findViewById(R.id.settings_help);

        popupFreq = (SeekBar) findViewById(R.id.settings_popup_seekbar);
        popupFreqText = (TextView) findViewById(R.id.settings_popup_freq_text);
        notifTime = (TextView) findViewById(R.id.settings_notif_time);
        popupAutomated = (CheckBox) findViewById(R.id.settings_popup_automated);
        popupInterval = (EditText) findViewById(R.id.settings_popup_interval);
        generatedRowsContainer = (LinearLayout) findViewById(R.id.settings_generated_symptoms_container);
        dataSync = (CheckBox) findViewById(R.id.settings_datasync);

        popupFreq.setEnabled(!AppPreferences.userSettings.isPopupsAutomated());
        popupFreqText.setText(String.valueOf(AppPreferences.userSettings.getPopupFrequency()));

        popupAutomated.setChecked(AppPreferences.userSettings.isPopupsAutomated());
        popupFreq.setProgress(AppPreferences.userSettings.getPopupFrequency());

        notifTime.setText(String.valueOf(AppPreferences.userSettings.getNotificationHour()));

        popupInterval.setText(String.valueOf(AppPreferences.userSettings.getPopupInterval() / AppHelpers.MINUTE_IN_MILLISECONDS));

        dataSync.setChecked(AppPreferences.userSettings.dataSyncEnabled());

        notifTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsHelp(AppPreferences.NOTIFICATION_HOUR);
                View dialogView = View.inflate(MainActivity.this, R.layout.timepicker, null);
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(MainActivity.this);

                final TimePicker timePicker = (TimePicker) dialogView.findViewById(R.id.timePicker);

                timePicker.setCurrentHour(AppPreferences.userSettings.getNotificationHour());

                Button ok = (Button) dialogView.findViewById(R.id.dialog_ok);
                Button cancel = (Button) dialogView.findViewById(R.id.dialog_cancel);
                ok.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        timePicker.clearFocus();
                        AppPreferences.setUserSetting(AppPreferences.NOTIFICATION_HOUR, timePicker.getCurrentHour());
                        notifTime.setText(String.valueOf(timePicker.getCurrentHour()));
                        notifTime.invalidate();
                        clockDialog.dismiss();
                        Toast.makeText(AppHelpers.currentContext, "Notification time changed", Toast.LENGTH_SHORT).show();
                        AnalyticsApplication.sendEvent("settings", "notification_time", null, null);

                        settingsSnack.dismiss();
                    }
                });
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        clockDialog.cancel();
                        settingsSnack.dismiss();
                    }
                });
                dialogBuilder.setView(dialogView);
                clockDialog = dialogBuilder.create();

                clockDialog.show();
            }
        });

        popupFreq.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                AppPreferences.setUserSetting(AppPreferences.POPUP_FREQUENCY, progress);
                popupFreqText.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                showSettingsHelp(AppPreferences.POPUP_FREQUENCY);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(AppHelpers.currentContext, "Popup frequency changed to " + AppPreferences.userSettings.getPopupFrequency() + ".", Toast.LENGTH_SHORT).show();
                AnalyticsApplication.sendEvent("settings", "notification_freq", String.valueOf(AppPreferences.userSettings.getPopupFrequency()), null);
                hideSettingsHelp();
            }
        });

        popupAutomated.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                AppPreferences.setUserSetting(AppPreferences.POPUP_AUTOMATED, isChecked);
                popupFreq.setEnabled(!isChecked);
                if (isChecked) {
                    popupFreqText.setText(String.valueOf(NotificationPreferences.getCurrentPreference()));
                    popupFreq.setProgress(NotificationPreferences.getCurrentPreference());
                }
                Toast.makeText(AppHelpers.currentContext, "Popup automation changed to " + isChecked + ".", Toast.LENGTH_SHORT).show();
                AnalyticsApplication.sendEvent("settings", "popup_automation", String.valueOf(isChecked), null);

            }
        });

        popupInterval.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    showSettingsHelp(AppPreferences.POPUP_INTERVAL);
                }
                return false;
            }
        });

        popupInterval.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    try {
                        int interval = Integer.valueOf(popupInterval.getText().toString()) * AppHelpers.MINUTE_IN_MILLISECONDS;
                        if (interval > AppHelpers.MINUTE_IN_MILLISECONDS) {
                            AppPreferences.setUserSetting(AppPreferences.POPUP_INTERVAL, interval);
                            Toast.makeText(AppHelpers.currentContext, "Popup interval changed to " + interval / AppHelpers.MINUTE_IN_MILLISECONDS + " minutes.", Toast.LENGTH_SHORT).show();
                            AnalyticsApplication.sendEvent("settings", "popup_interval", String.valueOf(interval / AppHelpers.MINUTE_IN_MILLISECONDS), null);

                        } else
                            Toast.makeText(AppHelpers.currentContext, "Too brief interval.", Toast.LENGTH_SHORT).show();
                    } catch (NumberFormatException e) {
                    }
                    InputMethodManager inputMethodManager = (InputMethodManager) AppHelpers.currentActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(AppHelpers.currentActivity.getCurrentFocus().getWindowToken(), 0);
                    hideSettingsHelp();
                    return true;
                }
                return false;
            }
        });

        dataSync.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast.makeText(AppHelpers.currentContext, "Enabling data sync...", Toast.LENGTH_SHORT).show();
                    AnalyticsApplication.sendEvent("settings", "data_sync", "enabled", null);
                    Log.d("Joining", AppPreferences.schema.aware_study_url);
                    Aware.joinStudy(AppHelpers.currentContext, AppPreferences.schema.aware_study_url);
                }
                else {
                    Toast.makeText(AppHelpers.currentContext, "Disabling data sync...", Toast.LENGTH_SHORT).show();
                    AnalyticsApplication.sendEvent("settings", "data_sync", "disabled", null);
                    Aware.reset(AppHelpers.currentContext);
                }
            }
        });

        for (String symKey : AppPreferences.generatedSymptoms.keySet()) {
            SettingsGeneratedSymptomRow row = new SettingsGeneratedSymptomRow(symKey);
            generatedRowsContainer.addView(row.get());
            generatedSymptomRows.put(symKey, row.get());
        }

    }

    public static void updateGeneratedSymptoms() {
        generatedRowsContainer.removeAllViews();
        for (String symKey : AppPreferences.generatedSymptoms.keySet()) {
            SettingsGeneratedSymptomRow row = new SettingsGeneratedSymptomRow(symKey);
            generatedRowsContainer.addView(row.get());
            generatedSymptomRows.put(symKey, row.get());
        }
        generatedRowsContainer.invalidate();
    }


    private boolean showSettingsHelp(String type) {
        settingsHelp.removeAllViews();
        TextView t = new TextView(this);
        t.setPadding(8,8,8,8);
        switch (type) {
            case AppPreferences.NOTIFICATION_HOUR:
                settingsSnack = Snackbar.make(settingsHelp, R.string.help_notification_hour, Snackbar.LENGTH_INDEFINITE);
                settingsSnack.show();
                return false;
            case AppPreferences.POPUP_INTERVAL:
                t.setText(R.string.help_popup_interval);
                break;
            case AppPreferences.POPUP_FREQUENCY:
                settingsSnack = Snackbar.make(settingsHelp, R.string.help_popup_frequency, Snackbar.LENGTH_LONG);
                settingsSnack.show();
                break;
        }
        t.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        t.setTextSize(12f);
        anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, R.anim.anim_in_right);
        anim.setDuration(250);
        t.startAnimation(anim);
        settingsHelp.addView(t);
        return false;
    }

    private void hideSettingsHelp() {
        if (settingsSnack != null) settingsSnack.dismiss();
        anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, R.anim.anim_out_left);
        anim.setDuration(250);
        if (settingsHelp.getChildCount() > 0) settingsHelp.getChildAt(0).startAnimation(anim);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                settingsHelp.removeAllViews();
                settingsHelp.invalidate();
            }
        }, 250);
    }

    public static void addSymptom(View view) {
        new NewSymptom().show(AppHelpers.currentActivity);
    }

    public static void leaveStudy(View view) {
        try {
            new LeaveStudy().show(AppHelpers.currentActivity);
        }
        catch (Exception e) {
            Log.d("leave_study", "crashed while trying to leave ze study");
            e.printStackTrace();
        }
    }

    public static LinearLayout scrollContainer;

    public static BubbleChart symptomChart;
    public static HashMap<String, View> factorViews;
    public static HashMap<String, Chart> factorCharts;
    public static LinearLayout extraContainer;
    public static TextView groupButton;
    public TextView symButton;
    public TextView facButton;

    public static ArrayList<String> dataSymptoms = new ArrayList<>();
    public static ArrayList<String> dataFactors = new ArrayList<>();

    private static Animation anim;
    private static RadioGroup group;
    private static int selectedGroup = R.id.data_selector_week;
    private static View groupView;

    private void showData() {
        SymptomDataViewer.showingSymptoms.clear();
        factorCharts = new HashMap<>();
        factorViews = new HashMap<>();

        setContentView(R.layout.dataview);
        tab = "data";

        Log.i("Analytics", "Setting screen name: " + tab);
        mTracker.setScreenName(tab);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        for (String symptom : AppPreferences.symptoms.keySet()) {
            if (!dataSymptoms.contains(symptom)) dataSymptoms.add(symptom);}
        for (String factor : AppPreferences.factors.keySet()) {
            if (!dataFactors.contains(factor)) dataFactors.add(factor);}

        scrollContainer = (LinearLayout) findViewById(R.id.data_scrollview);

        symptomChart = (BubbleChart) findViewById(R.id.data_symptomchart);

        CheckBox symCheck = (CheckBox) findViewById(R.id.data_symptom_checkbox);
        symCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) scrollContainer.addView(symptomChart);
                else scrollContainer.removeView(symptomChart);
            }
        });
        CheckBox facCheck = (CheckBox) findViewById(R.id.data_factor_checkbox);
        facCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    for (String key : factorViews.keySet()) {
                        if (dataFactors.contains(key)) scrollContainer.addView(factorViews.get(key));
                    }
                } else {
                    for (View v : factorViews.values()) {
                        scrollContainer.removeView(v);
                    }
                }
            }
        });

        symptomChart.setDragEnabled(true);
        symptomChart.setScaleEnabled(true);

        symptomChart.setDescription("");
        setSymptomChartSize();

        symptomChart.setOnChartValueSelectedListener(new BubbleSelectorListener());

        symptomChart.setOnTouchListener(new OnSwipeTouchListener(AppHelpers.currentContext) {
            @Override
            public void onSwipeRight() {
                anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, android.R.anim.slide_out_right);
                anim.setDuration(250);
                symptomChart.startAnimation(anim);
                AppHelpers.offset(true);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        chartChanged();
                    }
                }, 250);
            }

            @Override
            public void onSwipeLeft() {
                anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, R.anim.anim_out_left);
                anim.setDuration(250);
                symptomChart.startAnimation(anim);
                AppHelpers.offset(false);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        chartChanged();
                    }
                }, 250);
            }
        });

        setYAxisMinMax();

        XAxis x = symptomChart.getXAxis();
        x.setGridColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.Symptom));
        x.setLabelRotationAngle(60);
        x.setTextSize(8f);

        Legend legend = symptomChart.getLegend();
        legend.setPosition(Legend.LegendPosition.ABOVE_CHART_LEFT);
        legend.setWordWrapEnabled(true);
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setTextSize(14f);
        legend.setTypeface(Typeface.createFromAsset(AppHelpers.currentContext.getAssets(), "font/Roboto-Regular.ttf"));

        extraContainer = (LinearLayout) findViewById(R.id.data_extracontainer);

        // select different grouping option
        groupButton = (TextView) findViewById(R.id.data_grouping_option);
        groupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                groupView = AppHelpers.factory.inflate(R.layout.dataview_groupselector, null);
                if (extraContainer.getChildCount() > 0) {
                    extraContainer.removeAllViews();
                    SymptomDataViewer.showingSymptoms.clear();
                }
                group = (RadioGroup) groupView.findViewById(R.id.data_group);
                for (int i = 0; i < group.getChildCount(); i++) {
                    CheckBox c = (CheckBox) group.getChildAt(i);
                    if (c.getId() == selectedGroup) c.setChecked(true);
                    c.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            group.clearCheck();
                            if (isChecked) {
                                group.check(buttonView.getId());
                                changeGrouping(buttonView.getId());
                                clearExtraRow();
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        chartChanged();
                                    }
                                }, 250);
                            }
                            else {
                                group.check(R.id.data_selector_day);
                                changeGrouping(R.id.data_selector_day);
                                clearExtraRow();
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        chartChanged();
                                    }
                                }, 250);
                            }
                        }
                    });
                }
                extraContainer.addView(groupView);
            }
        });

        symButton = (TextView) findViewById(R.id.data_symptoms);
        symButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = CheckBoxSelector.getSymptomSelector();
                if (extraContainer.getChildCount() > 0) {
                    clearExtraRow();
                } else {
                    extraContainer.addView(view);
                }
            }
        });

        facButton = (TextView) findViewById(R.id.data_factors);
        facButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view = CheckBoxSelector.getFactorSelector();
                if (extraContainer.getChildCount() > 0) {
                    clearExtraRow();
                } else {
                    extraContainer.addView(view);
                }
            }
        });

        int order = 0;
        for (Factor f : AppPreferences.factors.values()) {
            if (f.input.equals("tracked"))
                scrollContainer.addView(FactorDataViewer.generateFactorCombinedChart(f.key, order % 6));
            else
                scrollContainer.addView(FactorDataViewer.generateFactorBarChart(f.key, order % 6));
            order++;
        }

        changeGrouping(selectedGroup);

        animateChart();

        DatabaseStorage.fetchAll();
    }

    // change the y axis alignment based on the number of visible elements
    private static void setYAxisMinMax() {
        YAxis y = symptomChart.getAxisLeft();
        y.setDrawLabels(false);
        //y.setSpaceTop(5f);
        //y.setSpaceBottom(5f);
        //y.setValueFormatter(new ChartSymptomLabelFormatter());
        y.setStartAtZero(false);
        y.setAxisMinValue(0.5f);
        y.setAxisMaxValue((float) (dataSymptoms.size() + 0.5));
        y.setGridColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.Symptom));

        y = symptomChart.getAxisRight();
        y.setDrawLabels(false);
        y.setStartAtZero(false);
        y.setAxisMinValue(0.5f);
        y.setAxisMaxValue((float) (dataSymptoms.size() + 0.5));
        y.setGridColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.Symptom));
    }

    // change the symptom chart size based on number of visible elements
    private static void setSymptomChartSize() {
        Resources r = AppHelpers.currentContext.getResources();
        float heightInPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150 + (50 * dataSymptoms.size()), r.getDisplayMetrics());
        BubbleChart.LayoutParams p = symptomChart.getLayoutParams();
        p.height = (int) heightInPx;
        symptomChart.setLayoutParams(p);
    }

    private static void animateChart() {
        symptomChart.animateX(750, Easing.EasingOption.EaseInQuart);
        Animation chartAnim = AnimationUtils.loadAnimation(AppHelpers.currentContext, android.R.anim.fade_in);
        chartAnim.setDuration(250);
        symptomChart.startAnimation(chartAnim);
    }

    private static void changeGrouping(int buttonid) {
        selectedGroup = buttonid;
        if (buttonid == R.id.data_selector_day) {
            groupButton.setText("12 hours");
            AppHelpers.setGroup(Calendar.HOUR_OF_DAY);
            AnalyticsApplication.sendEvent("dataview", "change_grouping", "day", null);
        }
        else if (buttonid == R.id.data_selector_week) {
            groupButton.setText("7 days");
            AppHelpers.setGroup(Calendar.DAY_OF_YEAR);
            AnalyticsApplication.sendEvent("dataview", "change_grouping", "week", null);
        }
        else if (buttonid == R.id.data_selector_month) {
            groupButton.setText("4 weeks");
            AppHelpers.setGroup(Calendar.WEEK_OF_YEAR);
            AnalyticsApplication.sendEvent("dataview", "change_grouping", "month", null);
        }
        else if (buttonid == R.id.data_selector_year) {
            groupButton.setText("6 months");
            AppHelpers.setGroup(Calendar.MONTH);
            AnalyticsApplication.sendEvent("dataview", "change_grouping", "year", null);
        }
    }

    public static void chartChanged() {
        MainActivity.symptomChart.setData(SymptomDataViewer.generateSymptomChartData());

        setSymptomChartSize();

        MainActivity.symptomChart.notifyDataSetChanged();
        for (String key : factorCharts.keySet()) {
            Log.d("update f c", key);
            switch (AppPreferences.factors.get(key).input) {
                case "tracked":
                    ((CombinedChart) factorCharts.get(key)).setData(FactorDataViewer.generateFactorComboChartData(key, FactorDataViewer.chartOrder.get(key)));
                    factorCharts.get(key).notifyDataSetChanged();
                    factorCharts.get(key).invalidate();
                    break;
                case "multiple":
                    factorCharts.get(key).setData(FactorDataViewer.generateFactorBarChartData(key, FactorDataViewer.chartOrder.get(key)));
                    factorCharts.get(key).notifyDataSetChanged();
                    factorCharts.get(key).invalidate();
                    break;
            }
        }
        symptomChart.invalidate();
        extraContainer.invalidate();
        animateChart();
    }

    public static void clearExtraRow() {
        anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, android.R.anim.slide_out_right);
        anim.setDuration(250);
        extraContainer.getChildAt(0).startAnimation(anim);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                extraContainer.removeAllViews();
            }
        }, 250);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    // get values back from other intents (camera)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == AppHelpers.REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            ValueMap m = Values.fetch(AppHelpers.curPictureKey);
            m.setPicturePath(AppHelpers.curPicturePath);
            NoSQLStorage.storeSingle(new Condition(AppHelpers.curPictureKey), m);

            Toast.makeText(AppHelpers.currentContext, "Added new image", Toast.LENGTH_SHORT).show();
            // loading takes a few (50-100) milliseconds and is threaded..
            NoSQLStorage.loadValues(tab);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    switch (tab) {
                        case "factors":
                            ((FactorRowAdapter) MainActivity.factor_list.getAdapter()).notifyDataSetChanged();
                            break;
                        case "symptoms":
                            ((SymptomRowAdapter) MainActivity.symptom_list.getAdapter()).notifyDataSetChanged();
                            break;
                        default:
                            break;
                    }
                }
            }, 200);
        }
        else {
            Log.d("logpicture", "failed");
        }

    }

    public static void emitNotification(View view) {

    }

    public static void emitPopup(View view) {

    }

}
