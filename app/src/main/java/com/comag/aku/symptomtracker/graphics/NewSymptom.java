package com.comag.aku.symptomtracker.graphics;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.comag.aku.symptomtracker.AppHelpers;
import com.comag.aku.symptomtracker.R;
import com.comag.aku.symptomtracker.app_settings.AppPreferences;
import com.comag.aku.symptomtracker.objects.Symptom;

/**
 * Created by aku on 01/12/15.
 */
public class NewSymptom {
    EditText name;
    EditText desc;
    RadioGroup rep_window;
    Button ok;
    Button cancel;

    AlertDialog dialog;

    public void show(Activity a) {
        View view = AppHelpers.factory.inflate(R.layout.new_symptom, null);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(a);

        name = (EditText) view.findViewById(R.id.newsymptom_name);
        desc = (EditText) view.findViewById(R.id.newsymptom_desc);
        rep_window = (RadioGroup) view.findViewById(R.id.newsymptom_input_freq);
        ok = (Button) view.findViewById(R.id.newsymptom_ok);
        cancel = (Button) view.findViewById(R.id.newsymptom_cancel);

        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String window = "day";
                switch (rep_window.getCheckedRadioButtonId()) {
                    case R.id.newsymptom_day:
                        break;
                    case R.id.newsymptom_hour:
                        window = "hour";
                        break;
                    default:
                        window = "day";
                        break;
                }
                AppPreferences.addUserSymptom(new Symptom(
                        name.getText().toString(),
                        desc.getText().toString(),
                        window
                ));
                // API call to sync this to server as well?
                Toast.makeText(AppHelpers.currentContext, "Added new symptom to track: " + name.getText().toString(), Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialogBuilder.setView(view);
        dialog = dialogBuilder.create();

        dialog.show();
    }
}
