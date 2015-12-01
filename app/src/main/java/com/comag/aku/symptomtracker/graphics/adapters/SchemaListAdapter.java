package com.comag.aku.symptomtracker.graphics.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.comag.aku.symptomtracker.Launch;
import com.comag.aku.symptomtracker.R;
import com.comag.aku.symptomtracker.Settings;
import com.comag.aku.symptomtracker.model.DatabaseStorage;
import com.comag.aku.symptomtracker.objects.Schema;

import java.util.List;

/**
 * Created by aku on 28/10/15.
 */
public class SchemaListAdapter extends ArrayAdapter<Schema> {
    TextView title;
    TextView desc;
    TextView author;
    TextView symptoms;
    TextView factors;
    LinearLayout row;

    Button join;

    View.OnClickListener buttClick;

    private List<Schema> schemas;
    public SchemaListAdapter(Context context, int resource, List<Schema> objects) {
        super(context, resource, objects);
        schemas = objects;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView;
        LayoutInflater inflater = (LayoutInflater) Settings.currentContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (Launch.visibleSchemas.values().contains(DatabaseStorage.schemaList.get(position))) {
            if (Launch.selectedSchemaIndex == null || position != Launch.selectedSchemaIndex) {
                rowView = inflater.inflate(R.layout.schemalist, parent, false);
                title = (TextView) rowView.findViewById(R.id.schema_title);
                title.setText(schemas.get(position).title);

                symptoms = (TextView) rowView.findViewById(R.id.schema_symptoms);
                symptoms.setText(schemas.get(position).symptoms.size() + " symptoms");

                factors = (TextView) rowView.findViewById(R.id.schema_factors);
                factors.setText(schemas.get(position).factors.size() + " factors");

                //row = (LinearLayout) rowView.findViewById(R.id.schema_row);
                //row.setBackgroundColor(Settings.randomizeListColor(position));

                return rowView;
            } else {
                rowView = inflater.inflate(R.layout.schemalist_selected, parent, false);
                title = (TextView) rowView.findViewById(R.id.schema_title);
                desc = (TextView) rowView.findViewById(R.id.schema_desc);
                title.setText(schemas.get(position).title);

                desc.setText(schemas.get(position).desc);

                symptoms = (TextView) rowView.findViewById(R.id.schema_symptoms);
                symptoms.setText(schemas.get(position).symptoms.size() + " symptoms");

                factors = (TextView) rowView.findViewById(R.id.schema_factors);
                factors.setText(schemas.get(position).factors.size() + " factors");
                //row = (LinearLayout) rowView.findViewById(R.id.schema_row);
                //row.setBackgroundColor(Settings.randomizeListColor(position));

                join = (Button) rowView.findViewById(R.id.schema_selected_join);
                if (buttClick == null) {
                    generateListener(position);
                }
                return rowView;
            }
        }
        else {
            return inflater.inflate(R.layout.emptylistitem, parent, false);
        }
    }

    private void generateListener(final int position) {
        join.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatabaseStorage.schema = DatabaseStorage.schemaList.get(position);
                Log.d("selected", DatabaseStorage.schema.toString());
                Launch.proceed();
            }
        });
    }
}
