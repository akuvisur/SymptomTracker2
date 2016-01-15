package com.comag.aku.symptomtracker.services;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.colintmiller.simplenosql.NoSQL;
import com.colintmiller.simplenosql.NoSQLEntity;
import com.colintmiller.simplenosql.RetrievalCallback;
import com.comag.aku.symptomtracker.AppHelpers;
import com.comag.aku.symptomtracker.Launch;
import com.comag.aku.symptomtracker.R;
import com.comag.aku.symptomtracker.analytics.AnalyticsApplication;
import com.comag.aku.symptomtracker.app_settings.AppPreferences;
import com.comag.aku.symptomtracker.data_syncronization.SyncronizationController;
import com.comag.aku.symptomtracker.graphics.FlowLayout;
import com.comag.aku.symptomtracker.graphics.UIManager;
import com.comag.aku.symptomtracker.graphics.elements.ObservedAnimation;
import com.comag.aku.symptomtracker.model.DataObject;
import com.comag.aku.symptomtracker.model.NoSQLStorage;
import com.comag.aku.symptomtracker.model.data_storage.Values;
import com.comag.aku.symptomtracker.objects.ButtonKey;
import com.comag.aku.symptomtracker.objects.Factor;
import com.comag.aku.symptomtracker.objects.Symptom;
import com.comag.aku.symptomtracker.objects.ValueMap;
import com.comag.aku.symptomtracker.objects.tracking.Condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * Created by aku on 26/11/15.
 */
public class InputPopup {
    private View row;
    private Animation anim;

    LayoutInflater inflater;

    public InputPopup() {}

    public void show() {
        getMissingInputs();
    }

    private static View popupLayout;
    public static void hidePopup() {
        final WindowManager windowManager = (WindowManager) NotificationService.getContext().getSystemService(Context.WINDOW_SERVICE);
        try {
            if (popupLayout != null) windowManager.removeView(popupLayout);
        }
        catch (IllegalArgumentException e) {
            Log.d("input_popup", "crash while trying to hide non-existing popups");
        }
    }

    public boolean emit(ArrayList<String> keys) {
        // nothing to show
        if (keys.isEmpty()) {
            Log.d("popup", "nothing to show!");
            // randomly prompt factor check ups
            AppHelpers.showingPopup = false;
            return false;
        }
        AppHelpers.showingPopup = true;

        inflater = (LayoutInflater) NotificationService.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        popupLayout = inflater.inflate(R.layout.popup, null);

        Button botherButton = (Button) popupLayout.findViewById(R.id.popup_bother);
        Button appButton = (Button) popupLayout.findViewById(R.id.popup_app);
        Button okButton = (Button) popupLayout.findViewById(R.id.popup_ok);

        LinearLayout inputRows = (LinearLayout) popupLayout.findViewById(R.id.popup_inputrows);

        final WindowManager windowManager = (WindowManager) NotificationService.getContext().getSystemService(Context.WINDOW_SERVICE);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.OPAQUE);

        params.windowAnimations = android.R.style.Animation_Dialog;

        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;

        // show max two elements at a time
        Collections.shuffle(keys);
        for (int i = 0; i < 2 && i < keys.size(); i++) {
            if (AppPreferences.symptoms.containsKey(keys.get(i))) inputRows.addView(createSymptomRow(keys.get(i)));
            else if (AppPreferences.factors.containsKey(keys.get(i))) inputRows.addView(CreateFactorRow(keys.get(i)));
        }

        botherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                anim = AnimationUtils.loadAnimation(NotificationService.getContext(), R.anim.anim_out_left);
                anim.setDuration(250);
                popupLayout.startAnimation(anim);
                Toast.makeText(NotificationService.getContext(), "I will try to bother you less frequently.", Toast.LENGTH_SHORT).show();

                SyncronizationController.storeNotificationResponse("no", "popup", UserContextService.getUserContextString());

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        windowManager.removeView(popupLayout);
                        AppHelpers.showingPopup = false;
                        switch (NotificationService.getMode()) {
                            case DUMMY_MODE:
                                NotificationPreferences.addDummyDontBother();
                                break;
                            case LEARNING_MODE:
                                break;
                        }

                    }
                }, 250);
            }

        });

        appButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                anim = AnimationUtils.loadAnimation(NotificationService.getContext(), android.R.anim.fade_out);
                anim.setDuration(250);
                popupLayout.startAnimation(anim);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        windowManager.removeView(popupLayout);
                        AppHelpers.showingPopup = false;
                        Intent intent = new Intent(NotificationService.getContext(), Launch.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        NotificationService.getContext().startActivity(intent);

                        SyncronizationController.storeNotificationResponse("app", "popup", UserContextService.getUserContextString());

                        switch (NotificationService.getMode()) {
                            case DUMMY_MODE:
                                NotificationPreferences.addDummyOk();
                                break;
                            case LEARNING_MODE:
                                break;
                        }
                    }
                }, 250);
            }

        });

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                anim = AnimationUtils.loadAnimation(NotificationService.getContext(), android.R.anim.slide_out_right);
                anim.setDuration(250);
                popupLayout.startAnimation(anim);
                Toast.makeText(NotificationService.getContext(), "Inputted values saved.", Toast.LENGTH_SHORT).show();
                SyncronizationController.storeNotificationResponse("ok", "popup", UserContextService.getUserContextString());

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        AppHelpers.showingPopup = false;
                        windowManager.removeView(popupLayout);
                        switch (NotificationService.getMode()) {
                            case DUMMY_MODE:
                                NotificationPreferences.addDummyOk();
                                break;
                            case LEARNING_MODE:
                                break;
                        }
                    }
                }, 250);
            }
        });


        windowManager.addView(popupLayout, params);
        // returned a popup
        return true;
    }

    ArrayList<String> missingKeys;
    private static String factorValue;
    private void getMissingInputs() {
        missingKeys = new ArrayList<>();
        factorValue = "";
        for (String key : AppPreferences.factors.keySet()) missingKeys.add(key);
        for (String key : AppPreferences.symptoms.keySet()) missingKeys.add(key);
        NoSQL.with(NotificationService.getContext()).using(DataObject.class)
                .bucketId(AppPreferences.getSchema().db_name)
                .retrieve(new RetrievalCallback<DataObject>() {
                    public void retrievedResults(List<NoSQLEntity<DataObject>> entities) {
                        // remove keys from missingList if they are current data
                        for (int i = 0; i < entities.size(); i++) {
                            if (entities.get(i).getData().c.isCurrent()) {
                                missingKeys.remove(entities.get(i).getData().c.key);
                            }
                        }
                        if (missingKeys.isEmpty()) {
                            // sometimes randomly show daily factor inputs to keep them up to date during the day
                            Calendar c  =Calendar.getInstance();
                            if (c.get(Calendar.HOUR_OF_DAY) < 18) {
                                return;
                            }
                            if ((new Random(System.currentTimeMillis()).nextInt(100) < 115) && !AppPreferences.factors.isEmpty()) {
                                ArrayList<String> factors = new ArrayList<>();
                                for (String key : AppPreferences.factors.keySet()) {
                                    if (AppPreferences.factors.get(key).rep_window.equals("day")) factors.add(key);
                                }
                                Collections.shuffle(factors);
                                missingKeys.add(factors.get(0));
                                for (int i = 0; i < entities.size(); i++) {
                                    if (entities.get(i).getData().c.isCurrent() && entities.get(i).getData().c.key.equals(missingKeys.get(0))) {
                                        factorValue = entities.get(i).getData().v.getValue();
                                    }
                                }
                            }
                        }
                        emit(missingKeys);
                    }
                });
    }

    private TextView title;
    private TextView desc;

    private View createSymptomRow(final String key) {
        Symptom symptom = AppPreferences.symptoms.get(key);

        row = inflater.inflate(R.layout.popup_symptomrow, null);

        LinearLayout stateBar = (LinearLayout) row.findViewById(R.id.symptom_row_color);
        stateBar.setBackgroundColor(AppHelpers.generateServiceColor((int) (Math.random() * 10)));

        LinearLayout container = (LinearLayout) row.findViewById(R.id.symptomrow_container);

        title = (TextView) row.findViewById(R.id.symptom_title);
        desc = (TextView) row.findViewById(R.id.symptom_desc);
        title.setTextColor(ContextCompat.getColor(NotificationService.getContext(), R.color.black));
        desc.setTextColor(ContextCompat.getColor(NotificationService.getContext(), R.color.black));
        Button b = (Button) row.findViewById(R.id.symptom_input);

        title.setText(symptom.name);
        desc.setText(symptom.desc);

        View inputs = inflater.inflate(R.layout.symptomrow_input, null);

        Button noneButton = (Button) inputs.findViewById(R.id.symptom_none);
        Button mildButton = (Button) inputs.findViewById(R.id.symptom_mild);
        Button severeButton = (Button) inputs.findViewById(R.id.symptom_severe);
        if (symptom.positiveRange) {
            noneButton.setText("Low");
            mildButton.setText("Medium");
            severeButton.setText("High");
        }

        noneButton.setOnClickListener(setSymptomButtonListener(AppHelpers.getSymptomValueName(symptom, 0), b, key, container, inputs));
        mildButton.setOnClickListener(setSymptomButtonListener(AppHelpers.getSymptomValueName(symptom, 1), b, key, container, inputs));
        severeButton.setOnClickListener(setSymptomButtonListener(AppHelpers.getSymptomValueName(symptom, 2), b, key, container, inputs));

        container.addView(inputs);

        //row.setBackground(ContextCompat.getDrawable(NotificationService.getUserContext(), R.drawable.roundedwhite));

        return row;
    }

    public static HashMap<String, Button> valueViews = new HashMap<>();
    Button inputButton;
    private View CreateFactorRow(String key) {
        final Factor factor = AppPreferences.factors.get(key);

        row = inflater.inflate(R.layout.popup_factorrow, null);

        LinearLayout stateBar = (LinearLayout) row.findViewById(R.id.factor_row_color);
        stateBar.setBackgroundColor(AppHelpers.generateServiceColor((int) (Math.random() * 10)));

        final LinearLayout container = (LinearLayout) row.findViewById(R.id.factorrow_container);

        title = (TextView) row.findViewById(R.id.factor_title);
        desc = (TextView) row.findViewById(R.id.factor_desc);
        title.setTextColor(ContextCompat.getColor(NotificationService.getContext(), R.color.black));
        desc.setTextColor(ContextCompat.getColor(NotificationService.getContext(), R.color.black));

        title.setText(factor.name);
        desc.setText(factor.desc);

        inputButton = (Button) row.findViewById(R.id.factor_input);
        valueViews.put(key, inputButton);
        if (factorValue.length() > 0) {
            try {
                Integer i = Integer.valueOf(factorValue);
                inputButton.setText(i.toString());
            }
            catch (NumberFormatException e) {
                inputButton.setBackground(ContextCompat.getDrawable(NotificationService.getContext(), R.drawable.checkmark_primary));
                inputButton.setText("");
            }
        }

        final View inputs;
        switch (factor.input) {
            case "tracked":
                inputs = inflater.inflate(R.layout.factorrow_input_range, null);
                generateTrackedSelect(container, inputs, inputButton, factor);
                container.addView(inputs);
                break;
            case "multiple":
                inputs = inflater.inflate(R.layout.factorrow_input_multiple, null);
                inputButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        generateMultipleSelect(container, inputs, inputButton, factor);
                    }
                });
                break;
        }

        //row.setBackground(ContextCompat.getDrawable(NotificationService.getUserContext(), R.drawable.roundedwhite));

        return row;
    }

    private void generateTrackedSelect(LinearLayout container, View inputs, Button input, Factor factor) {
        SeekBar rangeBar = (SeekBar) inputs.findViewById(R.id.factor_input_range);
        rangeBar.setMax(Integer.valueOf(factor.range_max));

        TextView rangeText = (TextView) inputs.findViewById(R.id.factor_input_range_value);

        TextView minValue = (TextView) inputs.findViewById(R.id.min_range);
        minValue.setText(factor.range_min);
        TextView maxValue = (TextView) inputs.findViewById(R.id.max_range);
        maxValue.setText(factor.range_max);

        rangeBar.setProgress(0);

        rangeBar.setOnSeekBarChangeListener(generateSeekBarListener(rangeText, factor.key));

        ImageButton okButton = (ImageButton) inputs.findViewById(R.id.input_ok);
        okButton.setOnClickListener(generateOkButtonListener(rangeBar, container, inputs, factor));

        try {
            int value = Integer.valueOf(Values.fetch(factor.key).getValue());
            //Log.d("popup", "factor value:" + value);
            rangeBar.setProgress(value);
            rangeText.setText(String.valueOf(value));
            input.setText(String.valueOf(value));

        } catch (NumberFormatException e) {
            Log.d("generatedTrackedSelect", "bad number format for value " + Values.fetch(factor.key).getValue());
        } catch (NullPointerException e2) {
            Log.d("generatedTrackedSelect", "some null pointer error");
        }

        if (factorValue.length() > 0) {
            try {
                rangeBar.setProgress(Integer.valueOf(factorValue));
                rangeText.setText(factorValue);
            } catch (NumberFormatException e) {
                Log.d("generatedTrackedSelect", "bad number format for value " + factorValue);
            }
        }
    }

    private SeekBar.OnSeekBarChangeListener generateSeekBarListener(final TextView rangeTextView, final String factorKey) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueViews.get(factorKey).setText(String.valueOf(progress));
                rangeTextView.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(NotificationService.getContext(), "Remember to click the checkmark to finalize inputting.", Toast.LENGTH_SHORT).show();
            }
        };
    }

    private View.OnClickListener generateOkButtonListener(final SeekBar bar, final LinearLayout container, final View inputs, final Factor factor) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                valueViews.get(factor.key).setText(String.valueOf(bar.getProgress()));
                anim = AnimationUtils.loadAnimation(NotificationService.getContext(), android.R.anim.slide_out_right);
                anim.setDuration(250);
                container.startAnimation(anim);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        NoSQLStorage.storeSingle(new Condition(factor.key), new ValueMap(String.valueOf(bar.getProgress())));
                        container.removeView(inputs);
                    }
                },350);
            }
        };
    }

    List<String> selected;
    View dialogView;
    Button dialogOk;
    Button dialogCancel;
    private void generateMultipleSelect(LinearLayout container, View inputs, final TextView valueView, final Factor factor) {
        selected = new ArrayList<>();
        FlowLayout selection;
        List<String> options;
        ValueButton button;
        List<String> oldSelected;

        oldSelected = Arrays.asList(factorValue.split(","));
        options = Arrays.asList(factor.values.split(","));

        dialogView = View.inflate(NotificationService.getContext(), R.layout.popup_factorrow_input_multiple, null);
        selection = (FlowLayout) dialogView.findViewById(R.id.factor_multiple_input);

        // zz... the values are a list.toString() so each new toString() adds more whitespace between commas
        ArrayList<String> strippedOld = new ArrayList<>();
        for (int i = 0; i < oldSelected.size(); i++) {
            String strippy = oldSelected.get(i).trim();
            strippedOld.add(strippy);
        }

        for (String option : options) {
            button = new ValueButton(NotificationService.getContext(), option);
            button.setText(option);
            if (strippedOld.contains(option)) {
                button.setChecked(true);
                selected.add(option);
            }
            selection.addView(button);
        }

        final WindowManager windowManager = (WindowManager) NotificationService.getContext().getSystemService(Context.WINDOW_SERVICE);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.OPAQUE);

        params.windowAnimations = android.R.style.Animation_Dialog;

        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL;

        dialogOk = (Button) dialogView.findViewById(R.id.dialog_ok);
        dialogCancel = (Button) dialogView.findViewById(R.id.dialog_cancel);

        dialogOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Values.addMultipleValues(new Condition(factor.key), selected);
                valueView.setBackground(ContextCompat.getDrawable(NotificationService.getContext(), R.drawable.checkmark_primary));
                valueView.setText("");
                valueView.invalidate();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        NoSQLStorage.storeSingle(new Condition(factor.key), new ValueMap(selected));
                    }
                }, 350);
                AnalyticsApplication.sendEvent("popup", "accepted", UserContextService.getUserContextString(), null);

                windowManager.removeView(dialogView);
            }
        });
        dialogCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnalyticsApplication.sendEvent("popup", "cancelled", UserContextService.getUserContextString(), null);
                windowManager.removeView(dialogView);
            }
        });

        dialogView.setBackgroundColor(ContextCompat.getColor(NotificationService.getContext(), R.color.white));

        windowManager.addView(dialogView, params);
    }

    private class ValueButton extends CheckBox {
        private ValueButton _this;
        public ValueButton(Context context, final String value) {
            super(context);
            this._this = this;
            this.setTextColor(ContextCompat.getColor(NotificationService.getContext(), R.color.black));
            this.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    //_this.setChecked(_this.isChecked());
                    if (selected.contains(value)) {
                        selected.remove(value);
                        _this.setChecked(false);
                    }
                    else {
                        selected.add(value);
                        _this.setChecked(true);
                    }
                    //Log.d("selected", selected.toString());
                }
            });
        }

    }

    private View.OnClickListener setSymptomButtonListener(final String value, final Button button, final String symptomKey, final LinearLayout container, final View inputs) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (value) {
                    case "none":
                        button.setBackgroundResource(R.drawable.checkmark_anim_none);
                        break;
                    case "mild":
                        button.setBackgroundResource(R.drawable.checkmark_anim_mild);
                        break;
                    case "severe":
                        button.setBackgroundResource(R.drawable.checkmark_anim_severe);
                        break;
                    case "low":
                        button.setBackgroundResource(R.drawable.checkmark_anim_none);
                        break;
                    case "medium":
                        button.setBackgroundResource(R.drawable.checkmark_anim_mild);
                        break;
                    case "high":
                        button.setBackgroundResource(R.drawable.checkmark_anim_severe);
                        break;
                }

                button.setText("");

                ObservedAnimation checkAnimation = new ObservedAnimation((AnimationDrawable) button.getBackground(),
                        new ButtonKey(symptomKey, value));
                UIManager.addAnim(new ButtonKey(symptomKey, value), checkAnimation);
                button.setBackground(checkAnimation);
                checkAnimation.start();
                button.invalidate();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        NoSQLStorage.storeSingle(new Condition(symptomKey), new ValueMap(value));
                        anim = AnimationUtils.loadAnimation(
                                NotificationService.getContext(), android.R.anim.slide_out_right
                        );
                        anim.setDuration(250);
                        container.startAnimation(anim);
                    }
                }, 250);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        container.removeView(inputs);
                        popupLayout.invalidate();
                    }
                }, 350);

            }
        };
    }

}
