package com.comag.aku.symptomtracker;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;

import com.comag.aku.symptomtracker.app_settings.AppPreferences;
import com.comag.aku.symptomtracker.graphics.adapters.SchemaListAdapter;
import com.comag.aku.symptomtracker.model.ApiManager;
import com.comag.aku.symptomtracker.model.DatabaseStorage;
import com.comag.aku.symptomtracker.objects.Schema;
import com.comag.aku.symptomtracker.services.ApplicationMonitor;

import java.util.TreeMap;


public class Launch extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppHelpers.currentContext = getApplicationContext();
        AppHelpers.currentActivity = this;
    }

    @Override
    public void onResume() {
        super.onResume();
        AppHelpers.currentContext = getApplicationContext();
        AppHelpers.currentActivity = this;

        if (AppHelpers.DEBUG) AppPreferences.clear();
        else {
            AppPreferences.load();
            if (AppPreferences.getSchema() == null) launch(null);
            else {
                ApiManager.getFactorsForSchema();
                ApiManager.getSymptomsForSchema();
                AppHelpers.currentActivity.startActivity(new Intent(AppHelpers.currentActivity, MainActivity.class));
            }
        }
    }

    public static ListView schemaView;
    public static SearchView searchSchema;
    public static TreeMap<String, Schema> allSchemas = new TreeMap<>();
    public static TreeMap<String, Schema> visibleSchemas = new TreeMap<>();
    public void launch(View view) {
        setContentView(R.layout.activity_launch);
        Toolbar toolbar = (Toolbar) findViewById(R.id.launchtoolbar);
        toolbar.setTitle("Select study schema");
        setSupportActionBar(toolbar);

        schemaView = (ListView) findViewById(R.id.schemaList);
        schemaView.setAdapter(new SchemaListAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, DatabaseStorage.schemaList));
        addSchemaListListener(schemaView);

        searchSchema = (SearchView) findViewById(R.id.searchSchemas);
        addSearchListener(searchSchema);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Select a study to join by clicking on the title and then clicking the 'Join' button.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        Snackbar.make(fab, "Loading schemas from the repository..", Snackbar.LENGTH_SHORT).setAction("Action", null).show();

        ApiManager.getAllSchemas();
    }

    public static Integer selectedSchemaIndex;
    public void addSchemaListListener(ListView l) {
        l.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d("click", String.valueOf(position));
                if (selectedSchemaIndex != null && position == selectedSchemaIndex)
                    selectedSchemaIndex = null;
                else selectedSchemaIndex = position;
                ((SchemaListAdapter) schemaView.getAdapter()).notifyDataSetChanged();
            }
        });
    }

    public void addSearchListener(SearchView s) {
        s.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query.length() > 1) search(query.toLowerCase());
                else {
                    showAll();
                }
                ((SchemaListAdapter) schemaView.getAdapter()).notifyDataSetChanged();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d("search", newText);
                if (newText.length() > 1) search(newText.toLowerCase());
                else {
                    showAll();
                }
                ((SchemaListAdapter) schemaView.getAdapter()).notifyDataSetChanged();
                return false;
            }
        });
    }

    public void showAll() {
        visibleSchemas.clear();
        for (Schema s : DatabaseStorage.schemaList) {
            visibleSchemas.put(s.key, s);
        }
    }

    public void search(String searchString) {
        visibleSchemas.clear();
        for (Schema s : DatabaseStorage.schemaList) {
            if (s.title.toLowerCase().contains(searchString) ||
                    s.desc.toLowerCase().contains(searchString) ||
                    s.author.toLowerCase().contains(searchString)) {
                visibleSchemas.put(s.key, s);
            }
        }
    }

    public static void proceed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(AppHelpers.currentActivity);
        builder.setTitle("Give your user ID:");
        builder.setMessage(AppHelpers.parseSchema(DatabaseStorage.schemaList.get(selectedSchemaIndex)));
        // Set up the input
        final EditText input = new EditText(AppHelpers.currentActivity);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // proceed to new action etc
                AppPreferences.join(DatabaseStorage.schemaList.get(selectedSchemaIndex));
                ApiManager.getSymptomsForSchema();
                ApiManager.getFactorsForSchema();
                AppHelpers.currentActivity.startActivity(new Intent(AppHelpers.currentActivity, MainActivity.class));
                ApplicationMonitor.sendAccessibilityServiceVerification(AppHelpers.currentContext);
                Snackbar.make(input, "Please enable accessibility services for this application in your settings", Snackbar.LENGTH_INDEFINITE).setAction("Go to settings", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent accessibilitySettings = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                        AppHelpers.currentContext.startService(accessibilitySettings);
                    }
                }).show();
            }
        });
        builder.setNegativeButton(android.R.string.no, null).show();
    }

}
