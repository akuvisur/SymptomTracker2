package com.comag.aku.symptomtracker;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.comag.aku.symptomtracker.graphics.adapters.SchemaListAdapter;
import com.comag.aku.symptomtracker.model.ApiManager;
import com.comag.aku.symptomtracker.app_settings.AppPreferences;
import com.comag.aku.symptomtracker.model.DatabaseStorage;
import com.comag.aku.symptomtracker.objects.Schema;
import com.comag.aku.symptomtracker.services.NotificationService;

import java.util.TreeMap;


public class Launch extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Settings.currentContext = getApplicationContext();
        Settings.currentActivity = this;

        startService(new Intent(this, NotificationService.class));

        if (Settings.DEBUG) AppPreferences.clear();
        else {
            AppPreferences.load();
            if (AppPreferences.getSchema() == null) launch(null);
            else {
                ApiManager.getFactorsForSchema();
                ApiManager.getSymptomsForSchema();
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        Settings.currentContext = getApplicationContext();
        Settings.currentActivity = this;

        if (Settings.DEBUG) {
            setContentView(R.layout.debug_launch);
            //Settings.currentActivity.startActivity(new Intent(Settings.currentActivity, MainActivity.class));
        }
        else {
            if(AppPreferences.getSchema() == null) {
                launch(null);
            }
            else {
                Settings.currentActivity.startActivity(new Intent(Settings.currentActivity, MainActivity.class));
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

    private AlertDialog.Builder builder;
    public void emitNotification(View view) {
        builder = new AlertDialog.Builder(Settings.currentActivity);
        builder.setView(R.layout.symptomrow);
        builder.setTitle("Add a symptom");
        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d("alert", "ok");
            }
        });
        builder.setNegativeButton("Dont bother me", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d("alert", "dont bother");
            }
        });
        AlertDialog d = builder.create();
        d.show();
    }

    public void emitPopup(View view) {

    }

    public static void proceed() {
        new AlertDialog.Builder(Settings.currentActivity)
                .setTitle("Join study")
                .setMessage(Settings.parseSchema(DatabaseStorage.schemaList.get(selectedSchemaIndex)))
                .setIcon(R.drawable.info_color)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Toast.makeText(Settings.currentActivity, "Loading schema information..", Toast.LENGTH_SHORT).show();
                        // proceed to new action etc
                        AppPreferences.join(DatabaseStorage.schemaList.get(selectedSchemaIndex));
                        ApiManager.getSymptomsForSchema();
                        ApiManager.getFactorsForSchema();
                        Settings.currentActivity.startActivity(new Intent(Settings.currentActivity, MainActivity.class));
                    }})
                .setNegativeButton(android.R.string.no, null).show();
    }

}
