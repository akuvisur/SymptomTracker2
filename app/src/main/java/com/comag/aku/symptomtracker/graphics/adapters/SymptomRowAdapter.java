package com.comag.aku.symptomtracker.graphics.adapters;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.comag.aku.symptomtracker.MainActivity;
import com.comag.aku.symptomtracker.R;
import com.comag.aku.symptomtracker.AppHelpers;
import com.comag.aku.symptomtracker.graphics.UIManager;
import com.comag.aku.symptomtracker.graphics.elements.ObservedAnimation;
import com.comag.aku.symptomtracker.model.NoSQLStorage;
import com.comag.aku.symptomtracker.model.data_storage.Values;
import com.comag.aku.symptomtracker.objects.ButtonKey;
import com.comag.aku.symptomtracker.objects.Symptom;
import com.comag.aku.symptomtracker.objects.ValueMap;
import com.comag.aku.symptomtracker.objects.tracking.Condition;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by aku on 03/11/15.
 */
public class SymptomRowAdapter extends ArrayAdapter<Symptom> {

    private final GestureDetector gestureDetector = new GestureDetector(new GestureDetector.SimpleOnGestureListener() {
        public void onLongPress(MotionEvent e) {Log.d("SymptomAdapter", "Longpress detected : " + curSymptom.toString());}});

    private ImageButton extraCameraButton;

    View addElement;
    View inputElement;
    HashMap<String, View> rows;

    private Symptom curSymptom;

    List<Symptom> objects;

    HashMap<String, View.OnClickListener> stateButtonListeners;
    HashMap<String, View.OnClickListener> noneButtonListeners;
    HashMap<String, View.OnClickListener> mildButtonListeners;
    HashMap<String, View.OnClickListener> severeButtonListeners;
    HashMap<String, View.OnTouchListener> containerTouchListeners;
    HashMap<String, View.OnClickListener> commentButtonListeners;
    HashMap<String, View.OnClickListener> cameraButtonListeners;
    HashMap<String, View.OnClickListener> viewButtonListeners;

    HashMap<String, Button> stateButtons;
    HashMap<String, View> inputElements;
    HashMap<String, View> addElements;

    private boolean firstVisibleElementFound = false;

    public SymptomRowAdapter(Context context, int resource, List<Symptom> objects) {
        super(context, resource, objects);

        rows = new HashMap<>();
        stateButtonListeners = new HashMap<>();
        noneButtonListeners = new HashMap<>();
        mildButtonListeners = new HashMap<>();
        severeButtonListeners = new HashMap<>();
        containerTouchListeners = new HashMap<>();
        commentButtonListeners = new HashMap<>();
        cameraButtonListeners = new HashMap<>();
        viewButtonListeners = new HashMap<>();
        stateButtons = new HashMap<>();
        inputElements = new HashMap<>();
        addElements = new HashMap<>();
        this.objects = objects;
        createListeners();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (position == 0) firstVisibleElementFound = false;

        View rowView;
        LayoutInflater inflater = (LayoutInflater) AppHelpers.currentContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        curSymptom = objects.get(position);
        String state = UIManager.getSymptomState(curSymptom.key);
        if (MainActivity.onlyShowMissingSymptoms && !Values.fetch(curSymptom.key).getValue().equals("missing")) {
            rowView = inflater.inflate(R.layout.emptylistitem, null);
            UIManager.switchSymptomState(curSymptom.key, "view");
            return rowView;
        }

        if (MainActivity.onlyShowMissingSymptoms && !firstVisibleElementFound) {
            state = "input";
            firstVisibleElementFound = true;
        }

        ValueMap value = Values.fetch(curSymptom.key);

        rowView = inflater.inflate(R.layout.symptomrow, null);

        LinearLayout container = (LinearLayout) rowView.findViewById(R.id.symptomrow_container);
        container.setOnTouchListener(containerTouchListeners.get(curSymptom.key));

        LinearLayout stateBar = (LinearLayout) rowView.findViewById(R.id.symptom_row_color);

        TextView title = (TextView) rowView.findViewById(R.id.symptom_title);
        title.setText(curSymptom.name);
        title.setTextColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.black));

        TextView desc = (TextView) rowView.findViewById(R.id.symptom_desc);
        desc.setText(curSymptom.desc);

        Button stateButton = (Button) rowView.findViewById(R.id.symptom_input);
        stateButton.setOnClickListener(stateButtonListeners.get(curSymptom.key));
        stateButtons.put(curSymptom.key, stateButton);

        /*
        // doublecheck
        if (value.getValue().equals("missing")) {
            ValueMap v = NoSQLStorage.fetchOne(curSymptom.key);
            if (v != null && !v.getValue().equals("missing")) value = v;
        }
        */

        switch (value.getValue()) {
            case "missing":
                stateButton.setBackground(ContextCompat.getDrawable(AppHelpers.currentContext, R.drawable.roundgrey));
                stateButton.setText("Missing");
                break;
            case "none":
                if (UIManager.hasFinishedAnim(new ButtonKey(curSymptom.key, "none"))) {
                    // set drawable to static
                    stateButton.setBackground(ContextCompat.getDrawable(AppHelpers.currentContext, R.drawable.round_none_checkmark));
                }
                else {
                    stateButton.setBackgroundResource(R.drawable.checkmark_anim_none);
                    ObservedAnimation checkAnimation = new ObservedAnimation((AnimationDrawable) stateButton.getBackground(),
                            new ButtonKey(curSymptom.key, "none"));
                    UIManager.addAnim(new ButtonKey(curSymptom.key, "none"), checkAnimation);
                    stateButton.setBackground(checkAnimation);
                    checkAnimation.start();
                }
                stateBar.setBackgroundColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.None));
                stateButton.setText("");
                break;
            case "mild":
                // if an animation already finished for this symptom and this state, use a static instead
                if (UIManager.hasFinishedAnim(new ButtonKey(curSymptom.key, "mild"))) {
                    // set drawable to static
                    stateButton.setBackground(ContextCompat.getDrawable(AppHelpers.currentContext, R.drawable.round_mild_checkmark));
                }
                else {
                    stateButton.setBackgroundResource(R.drawable.checkmark_anim_mild);
                    ObservedAnimation checkAnimation = new ObservedAnimation((AnimationDrawable) stateButton.getBackground(),
                            new ButtonKey(curSymptom.key, "mild"));
                    UIManager.addAnim(new ButtonKey(curSymptom.key, "mild"), checkAnimation);
                    stateButton.setBackground(checkAnimation);
                    checkAnimation.start();
                }
                stateBar.setBackgroundColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.Mild));
                stateButton.setText("");
                break;
            case "severe":
                if (UIManager.hasFinishedAnim(new ButtonKey(curSymptom.key, "severe"))) {
                    // set drawable to static
                    stateButton.setBackground(ContextCompat.getDrawable(AppHelpers.currentContext, R.drawable.round_severe_checkmark));
                }
                else {
                    stateButton.setBackgroundResource(R.drawable.checkmark_anim_severe);
                    ObservedAnimation checkAnimation = new ObservedAnimation((AnimationDrawable) stateButton.getBackground(),
                            new ButtonKey(curSymptom.key, "severe"));
                    UIManager.addAnim(new ButtonKey(curSymptom.key, "severe"), checkAnimation);
                    stateButton.setBackground(checkAnimation);
                    checkAnimation.start();
                }
                stateBar.setBackgroundColor(ContextCompat.getColor(AppHelpers.currentContext, R.color.Severe));
                stateButton.setText("");
                break;
            default:
                stateButton.setBackground(ContextCompat.getDrawable(AppHelpers.currentContext, R.drawable.roundgrey));
        }

        View extraRow = inflater.inflate(R.layout.extrarow, parent, false);

        if (value.hasComment()) {
            ImageButton extraCommentButton = (ImageButton) extraRow.findViewById(R.id.has_comment);
            extraCommentButton.setVisibility(View.VISIBLE);
        }

        if (value.hasPicture()) {
            extraCameraButton = (ImageButton) extraRow.findViewById(R.id.has_image);
            extraCameraButton.setOnClickListener(viewButtonListeners.get(curSymptom.key));
            extraCameraButton.setVisibility(View.VISIBLE);
        }

        if (value.hasComment() || value.hasPicture()) {
            container.addView(extraRow);
        }

        if (state.equals("input")) {
            inputElement = AppHelpers.factory.inflate(R.layout.symptomrow_input, null);

            Button noneButton = (Button) inputElement.findViewById(R.id.symptom_none);
            noneButton.setOnClickListener(noneButtonListeners.get(curSymptom.key));

            Button mildButton = (Button) inputElement.findViewById(R.id.symptom_mild);
            mildButton.setOnClickListener(mildButtonListeners.get(curSymptom.key));

            Button severeButton = (Button) inputElement.findViewById(R.id.symptom_severe);
            severeButton.setOnClickListener(severeButtonListeners.get(curSymptom.key));

            Animation anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, R.anim.anim_in_right);
            inputElement.startAnimation(anim);

            inputElements.put(curSymptom.key, inputElement);
            container.addView(inputElement);
        }
        else if (UIManager.getSymptomState(curSymptom.key).equals("done")) {
            addElement = AppHelpers.factory.inflate(R.layout.symptomrow_add, null);

            TextView commentButton = (TextView) addElement.findViewById(R.id.add_comment);
            commentButton.setOnClickListener(commentButtonListeners.get(curSymptom.key));
            if (value.hasComment()) { commentButton.setText("Edit notes");}

            TextView cameraButton = (TextView) addElement.findViewById(R.id.add_picture);
            cameraButton.setOnClickListener(cameraButtonListeners.get(curSymptom.key));
            if (value.hasPicture()) { cameraButton.setText("Replace picture");}

            Animation anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, R.anim.anim_in_right);
            addElement.startAnimation(anim);
            addElements.put(curSymptom.key, addElement);
            container.addView(addElement);
        }

        // set the complete row View as a member so it can be invalidated to force refresh
        rows.put(objects.get(position).key, rowView);
        return rowView;

    }

    private void createListeners() {
        for (Symptom s : objects) {
            stateButtonListeners.put(s.key, new SymptomStateListener(s));
            noneButtonListeners.put(s.key, new SymptomNoneListener(s));
            mildButtonListeners.put(s.key, new SymptomMildListener(s));
            severeButtonListeners.put(s.key, new SymptomSevereListener(s));
            containerTouchListeners.put(s.key, new ContainerTouchListener(s));
            commentButtonListeners.put(s.key, new CommentButtonListener(s));
            cameraButtonListeners.put(s.key, new CameraButtonListener(s));
            viewButtonListeners.put(s.key, new ViewButtonListener(s));
        }
    }

    private class ContainerTouchListener implements View.OnTouchListener {
        private Symptom symptom;
        private Animation anim;
        private String newState;

        public ContainerTouchListener(Symptom s) {
            this.symptom = s;
        }

        @Override
        public boolean onTouch(final View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                v.setBackgroundResource(R.color.hilight);
                v.invalidate();
                if (UIManager.getSymptomState(symptom.key).equals("input")) {
                    anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, android.R.anim.slide_out_right);
                    anim.setDuration(250);
                    inputElements.get(symptom.key).startAnimation(anim);
                }
                if (UIManager.getSymptomState(symptom.key).equals("done")) {
                    anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, android.R.anim.slide_out_right);
                    anim.setDuration(250);
                    addElements.get(symptom.key).startAnimation(anim);
                }
                if (UIManager.getSymptomState(symptom.key).equals("view") && Values.fetch(symptom.key).getValue().equals("missing")) {
                    newState = "input";
                } else if (UIManager.getSymptomState(symptom.key).equals("view")) {
                    newState = "done";
                }
                else if (UIManager.getSymptomState(symptom.key).equals("input")) {
                    newState = "view";
                } else if (UIManager.getSymptomState(symptom.key).equals("done")) {
                    newState = "view";
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        v.setBackgroundResource(android.R.color.transparent);
                        UIManager.setSymptomState(symptom.key, newState);
                        ((SymptomRowAdapter) MainActivity.symptom_list.getAdapter()).notifyDataSetChanged();
                    }
                }, 250);
            }
            return true;
        }
    }

    private class SymptomStateListener implements View.OnClickListener {

        private Symptom symptom;
        private Animation anim;
        private String newState;

        public SymptomStateListener(Symptom s) {
            this.symptom = s;
        }

        @Override
        public void onClick(View v) {
            if (UIManager.getSymptomState(symptom.key).equals("input")) {
                anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, android.R.anim.slide_out_right);
                anim.setDuration(250);
                inputElements.get(symptom.key).startAnimation(anim);
            }
            if (UIManager.getSymptomState(symptom.key).equals("done")) {
                anim = AnimationUtils.loadAnimation(AppHelpers.currentContext, android.R.anim.slide_out_right);
                anim.setDuration(250);
                addElements.get(symptom.key).startAnimation(anim);
            }
            if (UIManager.getSymptomState(symptom.key).equals("view")) {
                newState = "input";
            }
            else if (UIManager.getSymptomState(symptom.key).equals("input")) {
                newState = "view";
            } else if (UIManager.getSymptomState(symptom.key).equals("done")) {
                newState = "input";
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    UIManager.setSymptomState(symptom.key, newState);
                    ((SymptomRowAdapter) MainActivity.symptom_list.getAdapter()).notifyDataSetChanged();
                }
            }, 250);
        }
    }

    private class SymptomNoneListener implements View.OnClickListener {
        private Symptom symptom;
        private Animation anim;

        public SymptomNoneListener(Symptom s) {
            this.symptom = s;
        }

        @Override
        public void onClick(View v) {
            UIManager.setSymptomState(symptom.key, "done");
            if (MainActivity.onlyShowMissingSymptoms) {
                Button b = stateButtons.get(symptom.key);
                b.setBackgroundResource(R.drawable.checkmark_anim_none);
                b.setText("");

                ObservedAnimation checkAnimation = new ObservedAnimation((AnimationDrawable) b.getBackground(),
                        new ButtonKey(curSymptom.key, "none"));
                UIManager.addAnim(new ButtonKey(curSymptom.key, "none"), checkAnimation);
                b.setBackground(checkAnimation);
                checkAnimation.start();
                b.invalidate();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        NoSQLStorage.storeSingle(new Condition(symptom.key), new ValueMap("none"));
                        anim = AnimationUtils.loadAnimation(
                                AppHelpers.currentContext, android.R.anim.slide_out_right
                        );
                        anim.setDuration(250);
                        rows.get(symptom.key).startAnimation(anim);

                    }
                }, 250);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ((SymptomRowAdapter) MainActivity.symptom_list.getAdapter()).notifyDataSetChanged();
                    }
                }, 600);
            }
            else {
                NoSQLStorage.storeSingle(new Condition(symptom.key), new ValueMap("none"));
                ((SymptomRowAdapter) MainActivity.symptom_list.getAdapter()).notifyDataSetChanged();
            }

        }
    }

    private class SymptomMildListener implements View.OnClickListener {
        private Symptom symptom;
        private Animation anim;

        public SymptomMildListener(Symptom s) {
            this.symptom = s;
        }

        @Override
        public void onClick(View v) {
            UIManager.setSymptomState(symptom.key, "done");

            if (MainActivity.onlyShowMissingSymptoms) {
                Button b = stateButtons.get(symptom.key);
                b.setBackgroundResource(R.drawable.checkmark_anim_mild);
                b.setText("");

                ObservedAnimation checkAnimation = new ObservedAnimation((AnimationDrawable) b.getBackground(),
                        new ButtonKey(curSymptom.key, "mild"));
                UIManager.addAnim(new ButtonKey(curSymptom.key, "mild"), checkAnimation);
                b.setBackground(checkAnimation);
                checkAnimation.start();
                b.invalidate();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        NoSQLStorage.storeSingle(new Condition(symptom.key), new ValueMap("mild"));
                        anim = AnimationUtils.loadAnimation(
                                AppHelpers.currentContext, android.R.anim.slide_out_right
                        );
                        anim.setDuration(250);
                        rows.get(symptom.key).startAnimation(anim);

                    }
                }, 250);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ((SymptomRowAdapter) MainActivity.symptom_list.getAdapter()).notifyDataSetChanged();
                    }
                }, 600);
            }
            else {
                NoSQLStorage.storeSingle(new Condition(symptom.key), new ValueMap("mild"));
                ((SymptomRowAdapter) MainActivity.symptom_list.getAdapter()).notifyDataSetChanged();
            }
        }
    }

    private class SymptomSevereListener implements View.OnClickListener {
        private Symptom symptom;
        private Animation anim;

        public SymptomSevereListener(Symptom s) {
            this.symptom = s;
        }

        @Override
        public void onClick(View v) {
            UIManager.setSymptomState(symptom.key, "done");

            if (MainActivity.onlyShowMissingSymptoms) {
                Button b = stateButtons.get(symptom.key);
                b.setBackgroundResource(R.drawable.checkmark_anim_severe);
                b.setText("");
                ObservedAnimation checkAnimation = new ObservedAnimation((AnimationDrawable) b.getBackground(),
                        new ButtonKey(curSymptom.key, "severe"));
                UIManager.addAnim(new ButtonKey(curSymptom.key, "severe"), checkAnimation);
                b.setBackground(checkAnimation);
                checkAnimation.start();
                b.invalidate();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        NoSQLStorage.storeSingle(new Condition(symptom.key), new ValueMap("severe"));
                        anim = AnimationUtils.loadAnimation(
                                AppHelpers.currentContext, android.R.anim.slide_out_right
                        );
                        anim.setDuration(250);
                        rows.get(symptom.key).startAnimation(anim);

                    }
                }, 250);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ((SymptomRowAdapter) MainActivity.symptom_list.getAdapter()).notifyDataSetChanged();
                    }
                }, 600);
            }
            else {
                NoSQLStorage.storeSingle(new Condition(symptom.key), new ValueMap("severe"));
                ((SymptomRowAdapter) MainActivity.symptom_list.getAdapter()).notifyDataSetChanged();
            }
        }
    }

    private class CommentButtonListener implements View.OnClickListener {
        View view;
        private Symptom symptom;
        private EditText text;
        private AlertDialog.Builder alert;
        private AlertDialog dialog;

        public CommentButtonListener(Symptom s) {
            this.symptom = s;
        }

        @Override
        public void onClick(View v) {
            view = View.inflate(AppHelpers.currentContext, R.layout.comment, null);
            text = (EditText) view.findViewById(R.id.comment_text);
            text.setText(Values.fetch(symptom.key).getComment());
            alert = new AlertDialog.Builder(AppHelpers.currentActivity)
                    .setView(view)
                    .setIcon(null)
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            Values.addComment(new Condition(symptom.key), text.getText().toString());
                            ((SymptomRowAdapter) MainActivity.symptom_list.getAdapter()).notifyDataSetChanged();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            dialog = alert.create();

            dialog.setOnShowListener(new DialogInterface.OnShowListener(){
                @Override
                public void onShow(DialogInterface dialog) {
                    text.requestFocus();
                }
            });
            dialog.show();
        }
    }

    private class CameraButtonListener implements View.OnClickListener {
        private Symptom symptom;

        public CameraButtonListener(Symptom s) { this.symptom = s; }
        @Override
        public void onClick(View v) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(AppHelpers.currentContext.getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = AppHelpers.createImageFile(symptom.key);
                } catch (IOException ex) {
                    Log.d("error creating file", ex.toString());
                    // Error occurred while creating the File
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    AppHelpers.curPicturePath = photoFile.getAbsolutePath();
                    AppHelpers.curPictureKey = symptom.key;
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
        private Symptom symptom;

        private ImageView image;
        private Bitmap bm;
        private View view;

        private File f;

        private AlertDialog.Builder alert;
        private Dialog dialog;

        public ViewButtonListener(Symptom s) { this.symptom = s;}

        @Override
        public void onClick(View v) {
            view = View.inflate(AppHelpers.currentContext, R.layout.imageviewer, null);
            f = new File(Values.fetch(symptom.key).getPicturePath());

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
                BitmapFactory.decodeFile(Values.fetch(symptom.key).getPicturePath(), bmOptions);

                int photoW = bmOptions.outWidth;
                int photoH = bmOptions.outHeight;

                Log.d("math.min", photoW + " / " + targetW + " : " + photoH + " / " + targetH);
                int scaleFactor = Math.min(photoW/600, photoH/800);

                bmOptions.inJustDecodeBounds = false;
                bmOptions.inSampleSize = scaleFactor;
                bmOptions.inPurgeable = true;
                bm = BitmapFactory.decodeFile(Values.fetch(symptom.key).getPicturePath(), bmOptions);

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
