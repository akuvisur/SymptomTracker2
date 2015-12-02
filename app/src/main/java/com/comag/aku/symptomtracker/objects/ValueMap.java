package com.comag.aku.symptomtracker.objects;

import android.util.Log;

import java.util.List;

/**
 * Created by aku on 04/11/15.
 */
public class ValueMap {
    private String value;
    private String comment;
    private String picturePath;

    public ValueMap(String value) {
        this.value = value;
    }

    public ValueMap(List<String> values) {
        setValues(values);
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setPicturePath(String path) {
        this.picturePath = path;
        Log.d("picturepath", "set to: " + picturePath);
    }

    public void setValues(List<String> values) {
        StringBuffer sb = new StringBuffer();
        String delim = "";
        for (String s : values) {
            sb.append(delim).append(s.toLowerCase());
            delim = ", ";
        }
        value = sb.toString();
    }

    public String getValue() {
        if (value != null) return value;
        else return "missing";
    }

    public Float getNumericValue() {
        if (value != null) {
            switch(value) {
                case "none":
                    return 1f;
                case "mild":
                    return 2f;
                case "severe":
                    return 3f;
                default:
                    try {
                        return Float.valueOf(value);
                    }
                    catch (NumberFormatException e){return 0f;}
            }
        }
        else return 0f;
    }

    public String getComment() {
        if (comment != null) return comment;
        else return "";
    }

    public String getPicturePath() {
        if (picturePath != null) return picturePath;
        else return "";
    }

    @Override
    public String toString() {
        return value;
    }

    public String toRenderableString() {
        return value + " : " + comment + " file: " + picturePath;
    }

    public boolean hasComment() {return (comment != null);}
    public boolean hasPicture() {
        if (picturePath != null && picturePath.length() > 0) Log.d("had picture", picturePath);
        return (picturePath != null);
    }
}