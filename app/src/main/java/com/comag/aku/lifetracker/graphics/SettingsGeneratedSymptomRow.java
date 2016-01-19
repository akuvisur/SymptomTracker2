package com.comag.aku.lifetracker.graphics;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.comag.aku.lifetracker.AppHelpers;
import com.comag.aku.lifetracker.MainActivity;
import com.comag.aku.lifetracker.R;
import com.comag.aku.lifetracker.app_settings.AppPreferences;
import com.comag.aku.lifetracker.objects.Symptom;

/**
 * Created by aku on 02/12/15.
 */
public class SettingsGeneratedSymptomRow {
    private String key;

    private AlertDialog dialog;

    public SettingsGeneratedSymptomRow(String key) {
        this.key = key;
    }

    public View get() {
        View result = AppHelpers.factory.inflate(R.layout.generated_row, null);
        Button removeButton = (Button) result.findViewById(R.id.settings_remove_generated);
        TextView name = (TextView) result.findViewById(R.id.settings_generated_name);
        name.setText(Symptom.keyToName(key));
        removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(AppHelpers.currentActivity);
                builder.setTitle("Are you sure?");
                builder.setMessage("Removing a user generated symptom means you will no longer track it.");
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        AppPreferences.symptoms.remove(key);
                        AppPreferences.generatedSymptoms.remove(key);
                        AppPreferences.storeSymptomsToSharedPrefs();
                        MainActivity.updateGeneratedSymptoms();
                        dialog.dismiss();
                    }
                });
                dialog = builder.create();
                dialog.show();
            }
        });
        return result;
    }
}
