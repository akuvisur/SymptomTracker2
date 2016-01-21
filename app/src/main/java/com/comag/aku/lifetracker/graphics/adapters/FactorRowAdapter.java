package com.comag.aku.lifetracker.graphics.adapters;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.comag.aku.lifetracker.AppHelpers;
import com.comag.aku.lifetracker.MainActivity;
import com.comag.aku.lifetracker.R;
import com.comag.aku.lifetracker.graphics.FlowLayout;
import com.comag.aku.lifetracker.graphics.UIManager;
import com.comag.aku.lifetracker.model.NoSQLStorage;
import com.comag.aku.lifetracker.model.data_storage.Values;
import com.comag.aku.lifetracker.objects.Factor;
import com.comag.aku.lifetracker.objects.ValueMap;
import com.comag.aku.lifetracker.objects.tracking.Condition;
import com.comag.aku.lifetracker.services.UserContextService;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by aku on 04/11/15.
 */
public class FactorRowAdapter extends ArrayAdapter<Factor> {

    private ImageButton extraCameraButton;

    View addElement;
    View inputElement;
    HashMap<String, View> rows;

    List<Factor> objects;

    HashMap<String, View.OnClickListener> stateButtonListeners;
    HashMap<String, View.OnTouchListener> containerTouchListeners;
    HashMap<String, View.OnClickListener> okButtonListeners;
    HashMap<String, View.OnClickListener> commentButtonListeners;
    HashMap<String, View.OnClickListener> cameraButtonListeners;
    HashMap<String, View.OnClickListener> viewButtonListeners;
    HashMap<String, SeekBar.OnSeekBarChangeListener> rangeInputListeners;

    HashMap<String, View> inputElements;
    HashMap<String, View> addElements;

    HashMap<String, ImageButton> okButtons;

    public FactorRowAdapter(Context context, int resource, List<Factor> objects) {
        super(context, resource, objects);
        rows = new HashMap<>();
        stateButtonListeners = new HashMap<>();
        containerTouchListeners = new HashMap<>();
        okButtonListeners = new HashMap<>();
        commentButtonListeners = new HashMap<>();
        cameraButtonListeners = new HashMap<>();
        viewButtonListeners = new HashMap<>();
        rangeInputListeners = new HashMap<>();

        inputElements = new HashMap<>();
        addElements = new HashMap<>();
        okButtons = new HashMap<>();

        this.objects = objects;
        createListeners();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView;
        LayoutInflater inflater = (LayoutInflater) AppHelpers.currentContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        int curPosition = position;
        Factor curFactor = objects.get(position);
        String state = UIManager.getFactorState(curFactor.key);

        if (MainActivity.onlyShowMissingFactors && !Values.fetch(curFactor.key).getValue().equals("missing")) {
            rowView = inflater.inflate(R.layout.emptylistitem, null);
            UIManager.setFactorState(curFactor.key, "view");
            return rowView;
        }

        rowView = inflater.inflate(R.layout.factorrow, parent, false);

        ValueMap value = Values.fetch(curFactor.key);

        LinearLayout container = (LinearLayout) rowView.findViewById(R.id.factorrow_container);
        container.setOnTouchListener(containerTouchListeners.get(curFactor.key));

        LinearLayout stateBar = (LinearLayout) rowView.findViewById(R.id.factor_row_color);

        TextView title = (TextView) rowView.findViewById(R.id.factor_title);
        title.setText(curFactor.name);
        title.setTextColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.black));

        TextView desc = (TextView) rowView.findViewById(R.id.factor_desc);
        desc.setText(curFactor.desc);

        Button stateButton = (Button) rowView.findViewById(R.id.factor_input);
        stateButton.setOnClickListener(stateButtonListeners.get(curFactor.key));

        View extraRow = inflater.inflate(R.layout.extrarow, parent, false);

        switch (value.getValue()) {
            case "missing":
                break;
            default:
                // jos ei missing niin sitt etämä
                switch(curFactor.input) {
                    case "tracked":
                        stateButton.setText(value.getValue());
                        stateButton.setTextSize(14);
                        stateButton.setTextColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.colorPrimaryDark));
                        stateBar.setBackgroundColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.colorPrimaryDark));
                    break;
                    case "multiple":
                        stateButton.setBackground(ContextCompat.getDrawable(AppHelpers.currentContext, R.drawable.checkmark_primary));
                        stateButton.setText("");
                        stateBar.setBackgroundColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.colorPrimaryDark));
                        TextView extraValueText = (TextView) extraRow.findViewById(R.id.value_text);
                        extraValueText.setText(value.getValue());
                        extraValueText.setVisibility(View.VISIBLE);
                        break;
                }
        }

        if (value.hasComment()) {
            ImageButton extraCommentButton = (ImageButton) extraRow.findViewById(R.id.has_comment);
            extraCommentButton.setVisibility(View.VISIBLE);
        }

        if (value.hasPicture()) {
            extraCameraButton = (ImageButton) extraRow.findViewById(R.id.has_image);
            extraCameraButton.setOnClickListener(viewButtonListeners.get(curFactor.key));
            extraCameraButton.setVisibility(View.VISIBLE);
        }

        if (value.hasComment() || value.hasPicture() || (!value.getValue().equals("missing") && curFactor.input.equals("multiple"))) {
            container.addView(extraRow);
        }

        Animation anim;
        if (state.equals("input")) {
            switch (curFactor.input) {
                case "tracked":
                    inputElement = AppHelpers.factory.inflate(R.layout.factorrow_input_range, null);

                    SeekBar rangeBar = (SeekBar) inputElement.findViewById(R.id.factor_input_range);
                    rangeBar.setOnSeekBarChangeListener(rangeInputListeners.get(curFactor.key));
                    rangeBar.setMax(Integer.valueOf(curFactor.range_max));

                    TextView rangeText = (TextView) inputElement.findViewById(R.id.factor_input_range_value);

                    ((RangeBarListener) rangeInputListeners.get(curFactor.key)).setTextView(rangeText);

                    TextView minValue = (TextView) inputElement.findViewById(R.id.min_range);
                    minValue.setText(curFactor.range_min);
                    TextView maxValue = (TextView) inputElement.findViewById(R.id.max_range);
                    maxValue.setText(curFactor.range_max);

                    try {
                        rangeBar.setProgress(Integer.valueOf(value.getValue()));
                        rangeText.setText(value.getValue());
                    } catch (NumberFormatException e) {}

                    ImageButton okButton = (ImageButton) inputElement.findViewById(R.id.input_ok);
                    ((OkButtonListener) okButtonListeners.get(curFactor.key)).setParent(inputElement);
                    okButton.setOnClickListener(okButtonListeners.get(curFactor.key));
                    okButtons.put(curFactor.key, okButton);

                    anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, R.anim.anim_in_right);
                    inputElement.startAnimation(anim);

                    inputElements.put(curFactor.key, inputElement);
                    container.addView(inputElement);
                    break;

                default:
                    break;
            }
        }

        else if (state.equals("done")) {
            addElement = AppHelpers.factory.inflate(R.layout.factorrow_add, null);
            addElements.put(curFactor.key, addElement);

            TextView commentButton = (TextView) addElement.findViewById(R.id.add_comment);
            commentButton.setOnClickListener(commentButtonListeners.get(curFactor.key));
            if (value.hasComment()) { commentButton.setText("Edit notes");}

            TextView cameraButton = (TextView) addElement.findViewById(R.id.add_picture);
            cameraButton.setOnClickListener(cameraButtonListeners.get(curFactor.key));
            if (value.hasPicture()) { cameraButton.setText("Replace picture");}

            anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, R.anim.anim_in_right);
            anim.setDuration(250);
            addElement.startAnimation(anim);

            container.addView(addElement);
        }

        rows.put(curFactor.key, rowView);
        return rowView;
    }


    private void createListeners() {
        for (Factor f : objects) {
            containerTouchListeners.put(f.key, new ContainerTouchListener(f));
            stateButtonListeners.put(f.key, new FactorStateListener(f));
            okButtonListeners.put(f.key, new OkButtonListener(f));
            commentButtonListeners.put(f.key, new CommentButtonListener(f));
            cameraButtonListeners.put(f.key, new CameraButtonListener(f));
            viewButtonListeners.put(f.key, new ViewButtonListener(f));
            rangeInputListeners.put(f.key, new RangeBarListener(f));
        }
    }

    private class ContainerTouchListener implements View.OnTouchListener {
        private Factor factor;
        private Animation anim;
        private String newState;

        private boolean noRefresh = false;

        public ContainerTouchListener(Factor f) {
            this.factor = f;
        }

        @Override
        public boolean onTouch(final View v, MotionEvent event) {
           if (event.getAction() == MotionEvent.ACTION_UP) {
                noRefresh = false;
                v.setBackgroundResource(R.color.hilight);
                v.invalidate();
                if (UIManager.getFactorState(factor.key).equals("input")) {
                    anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, android.R.anim.slide_out_right);
                    anim.setDuration(250);
                    inputElements.get(factor.key).startAnimation(anim);
                }
                if (UIManager.getFactorState(factor.key).equals("done")) {
                    anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, android.R.anim.slide_out_right);
                    anim.setDuration(250);
                    addElements.get(factor.key).startAnimation(anim);
                }
                if (UIManager.getFactorState(factor.key).equals("view") && Values.fetch(factor.key).getValue().equals("missing")) {
                    if (factor.input.equals("multiple")) {
                        noRefresh = true;
                        showMultipleInput();
                    }
                    else newState = "input";
                } else if (UIManager.getFactorState(factor.key).equals("view")) {
                    newState = "done";
                }
                else if (UIManager.getFactorState(factor.key).equals("input")) {
                    newState = "view";
                } else if (UIManager.getFactorState(factor.key).equals("done")) {
                    newState = "view";
                }
                if (!noRefresh) new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        v.setBackgroundResource(android.R.color.transparent);
                        UIManager.setFactorState(factor.key, newState);
                        ((FactorRowAdapter) MainActivity.factor_list.getAdapter()).notifyDataSetChanged();
                    }
                }, 250);
            }
            return true;
        }

        private FlowLayout selection;
        private View view;
        private AlertDialog.Builder alert;
        private AlertDialog dialog;

        private List<String> options;
        private LinearLayout optionRows;
        private ValueButton button;
        private List<String> oldSelected;
        private List<String> selected;
        private void showMultipleInput() {
            selected = new ArrayList<String>();
            oldSelected = Values.fetchMultipleValues(factor.key);
            options = Arrays.asList(factor.values.split(","));

            view = View.inflate(AppHelpers.currentContext, R.layout.factorrow_input_multiple, null);
            selection = (FlowLayout) view.findViewById(R.id.factor_multiple_input);

            for (String option : options) {
                button = new ValueButton(AppHelpers.currentContext, option);
                button.setId((factor.key + option).hashCode());
                button.setText(option);
                if (oldSelected.contains(option)) {
                    button.setChecked(true);
                    selected.add(option);
                }
                selection.addView(button);
            }

            alert = new AlertDialog.Builder(AppHelpers.currentActivity)
                    .setView(view)
                    .setIcon(null)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            //Values.addMultipleValues(new Condition(factor.key), selected);
                            anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, android.R.anim.slide_out_right);
                            anim.setDuration(350);
                            view.startAnimation(anim);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    UIManager.setFactorState(factor.key, "done");
                                    UserContextService.setInputSource("in_app");
                                    NoSQLStorage.storeSingle(new Condition(factor.key), new ValueMap(selected));
                                    ((FactorRowAdapter) MainActivity.factor_list.getAdapter()).notifyDataSetChanged();
                                }
                            }, 350);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, android.R.anim.slide_out_right);
                            anim.setDuration(350);
                            view.startAnimation(anim);
                            dialog.cancel();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    UIManager.setFactorState(factor.key, "view");
                                    ((FactorRowAdapter) MainActivity.factor_list.getAdapter()).notifyDataSetChanged();
                                }
                            }, 350);
                        }
                    });
            dialog = alert.create();
            dialog.show();

        }

        private class ValueButton extends CheckBox {
            private ValueButton _this;
            public ValueButton(Context context, final String value) {
                super(context);
                this._this = this;
                this.setTextColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.black));
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
                        Log.d("selected", selected.toString());
                    }
                });
            }

        }
    }

    private class FactorStateListener implements View.OnClickListener {

        private Factor factor;
        private Animation anim;
        private String newState;

        private boolean noRefresh = false;

        public FactorStateListener(Factor f) {
            this.factor = f;
        }

        @Override
        public void onClick(View v) {
            noRefresh = false;
            if (UIManager.getFactorState(factor.key).equals("input")) {
                anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, android.R.anim.slide_out_right);
                anim.setDuration(250);
                inputElements.get(factor.key).startAnimation(anim);
            }
            if (UIManager.getFactorState(factor.key).equals("done")) {
                anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, android.R.anim.slide_out_right);
                anim.setDuration(250);
                addElements.get(factor.key).startAnimation(anim);
            }
            if (UIManager.getFactorState(factor.key).equals("view")) {
                if (factor.input.equals("multiple")) {
                    showMultipleInput();
                }
                else newState = "input";
            }
            else if (UIManager.getFactorState(factor.key).equals("input")) {
                newState = "view";
            } else if (UIManager.getFactorState(factor.key).equals("done")) {
                if (factor.input.equals("multiple")) {
                    showMultipleInput();
                }
                else newState = "input";
            }
            if (!noRefresh) new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    UIManager.setFactorState(factor.key, newState);
                    ((FactorRowAdapter) MainActivity.factor_list.getAdapter()).notifyDataSetChanged();
                }
            }, 250);
        }

        private FlowLayout selection;
        private View view;
        private AlertDialog.Builder alert;
        private AlertDialog dialog;

        private List<String> options;
        private LinearLayout optionRows;
        private ValueButton button;
        private List<String> oldSelected;
        private List<String> selected;
        private void showMultipleInput() {
            noRefresh = true;
            selected = new ArrayList<>();
            oldSelected = Values.fetchMultipleValues(factor.key);
            options = Arrays.asList(factor.values.split(","));

            view = View.inflate(AppHelpers.currentContext, R.layout.factorrow_input_multiple, null);

            selection = (FlowLayout) view.findViewById(R.id.factor_multiple_input);

            optionRows = new LinearLayout(AppHelpers.currentContext);
            optionRows.setOrientation(LinearLayout.VERTICAL);

            for (String option : options) {
                button = new ValueButton(AppHelpers.currentContext, option);
                button.setId((factor.key + option).hashCode());
                button.setText(option);
                if (oldSelected.contains(option)) {
                    button.setChecked(true);
                    selected.add(option);
                }
                optionRows.addView(button);
            }

            selection.addView(optionRows);

            alert = new AlertDialog.Builder(AppHelpers.currentActivity)
                    .setView(view)
                    .setIcon(null)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            Values.addMultipleValues(new Condition(factor.key), selected);
                            anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, android.R.anim.slide_out_right);
                            anim.setDuration(325);
                            view.startAnimation(anim);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    UIManager.setFactorState(factor.key, "done");
                                    UserContextService.setInputSource("in_app");
                                    NoSQLStorage.storeSingle(new Condition(factor.key), new ValueMap(selected));
                                    ((FactorRowAdapter) MainActivity.factor_list.getAdapter()).notifyDataSetChanged();
                                }
                            }, 350);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, android.R.anim.slide_out_right);
                            anim.setDuration(325);
                            view.startAnimation(anim);
                            dialog.cancel();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    UIManager.setFactorState(factor.key, "view");
                                    ((FactorRowAdapter) MainActivity.factor_list.getAdapter()).notifyDataSetChanged();
                                }
                            }, 350);
                        }
                    });
            dialog = alert.create();
            dialog.show();

        }

        private class ValueButton extends CheckBox {
            private ValueButton _this;
            public ValueButton(Context context, final String value) {
                super(context);
                this._this = this;
                this.setTextColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.black));
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
                    }
                });
            }
        }
    }

    private class OkButtonListener implements View.OnClickListener {

        private Factor factor;
        private Animation anim;
        private View parent;

        public OkButtonListener(Factor f) {
            this.factor = f;
        }

        public void setParent(View v) {
            this.parent = v;
        }

        @Override
        public void onClick(View v) {
            anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, android.R.anim.slide_out_right);
            anim.setDuration(250);
            parent.startAnimation(anim);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    UIManager.setFactorState(factor.key, "done");
                    UserContextService.setInputSource("in_app");
                    NoSQLStorage.storeSingle(new Condition(factor.key), new ValueMap(
                            ((TextView) inputElements.get(factor.key).findViewById(R.id.factor_input_range_value)).getText().toString()));
                    ((FactorRowAdapter) MainActivity.factor_list.getAdapter()).notifyDataSetChanged();
                }
            }, 250);
        }
    }

    private class CommentButtonListener implements View.OnClickListener {
        View view;
        private Factor factor;
        private EditText text;
        private AlertDialog.Builder alert;
        private AlertDialog dialog;

        public CommentButtonListener(Factor f) {
            this.factor = f;
        }

        @Override
        public void onClick(View v) {
            view = View.inflate(AppHelpers.currentContext, R.layout.comment, null);
            text = (EditText) view.findViewById(R.id.comment_text);
            text.setText(Values.fetch(factor.key).getComment());
            alert = new AlertDialog.Builder(AppHelpers.currentActivity)
                    .setView(view)
                    .setIcon(null)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            ValueMap m = Values.fetch(factor.key);
                            m.setComment(text.getText().toString());
                            UserContextService.setInputSource("in_app");
                            NoSQLStorage.storeSingle(new Condition(factor.key), m);
                            ((FactorRowAdapter) MainActivity.factor_list.getAdapter()).notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            dialog = alert.create();

            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    text.requestFocus();
                }
            });
            dialog.show();
        }
    }

    private class RangeBarListener implements SeekBar.OnSeekBarChangeListener {

        private TextView text;
        private Factor factor;

        private int value;

        public RangeBarListener(Factor factor) {
            super();
            this.factor = factor;
        }

        public void setTextView(TextView v) {
            this.text = v;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                value = progress + Integer.valueOf(factor.range_min);
                text.setText(String.valueOf(value));
                text.invalidate();
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            text.setText(String.valueOf(value));
            text.invalidate();
        }
    }

    private class CameraButtonListener implements View.OnClickListener {
        private Factor factor;

        public CameraButtonListener(Factor f) { this.factor = f; }
        @Override
        public void onClick(View v) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(AppHelpers.currentContext.getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = AppHelpers.createImageFile(factor.key);
                } catch (IOException ex) {
                    Log.d("error creating file", ex.toString());
                    // Error occurred while creating the File
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    AppHelpers.curPicturePath = photoFile.getAbsolutePath();
                    AppHelpers.curPictureKey = factor.key;
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                    AppHelpers.currentActivity.startActivityForResult(takePictureIntent, AppHelpers.REQUEST_IMAGE_CAPTURE);
                }
                else {
                    Snackbar.make(extraCameraButton, "Could not access storage to save image", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                }

            }
        }
    }


    private class ViewButtonListener implements View.OnClickListener {
        private Factor factor;

        private ImageView image;
        private Bitmap bm;
        private View view;

        private File f;

        private AlertDialog.Builder alert;
        private Dialog dialog;

        public ViewButtonListener(Factor f) { this.factor = f;}

        @Override
        public void onClick(View v) {
            view = View.inflate(AppHelpers.currentContext, R.layout.imageviewer, null);
            f = new File(Values.fetch(factor.key).getPicturePath());

            if(f.exists()){
                image = (ImageView) view.findViewById(R.id.featureImage);

                alert = new AlertDialog.Builder(AppHelpers.currentActivity)
                        .setView(view)
                        .setIcon(null)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {}
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                int targetW = image.getMeasuredWidth();
                int targetH = image.getMeasuredHeight();

                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bmOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(Values.fetch(factor.key).getPicturePath(), bmOptions);

                int photoW = bmOptions.outWidth;
                int photoH = bmOptions.outHeight;

                Log.d("math.min", photoW + " / " + targetW + " : " + photoH + " / " + targetH);
                int scaleFactor = Math.min(photoW/600, photoH/800);

                bmOptions.inJustDecodeBounds = false;
                bmOptions.inSampleSize = scaleFactor;
                bmOptions.inPurgeable = true;
                bm = BitmapFactory.decodeFile(Values.fetch(factor.key).getPicturePath(), bmOptions);

                if (photoW > photoH) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
                }

                image.setImageBitmap(bm);

                dialog = alert.create();
                dialog.show();
            }
            else {
                Log.d("Image", "No image found!");
            }
        }

    }
}
