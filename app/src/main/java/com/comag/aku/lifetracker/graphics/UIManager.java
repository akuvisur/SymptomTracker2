package com.comag.aku.lifetracker.graphics;

import com.comag.aku.lifetracker.graphics.elements.ObservedAnimation;
import com.comag.aku.lifetracker.objects.ButtonKey;

import java.util.HashMap;

/**
 * Created by aku on 03/11/15.
 */
public class UIManager {
    public static HashMap<String, String> symptomUIStates = new HashMap<>();
    public static void setSymptomState(String key, String state) {
        symptomUIStates.put(key, state);
        for (String k : symptomUIStates.keySet()) {
            if (!k.equals(key)) {
                //Log.e("ui", "set " + k + " to view, key was " + key);
                symptomUIStates.put(k, "view");
            }
        }
    }

    // switch single state
    public static void switchSymptomState(String key, String state) {
        symptomUIStates.put(key, state);
    }

    public static String getSymptomState(String key) {
        if (symptomUIStates.keySet().contains(key)) {
            return symptomUIStates.get(key);
        }
        else {
            return "view";
        }
    }

    public static HashMap<String, String> factorUIStates = new HashMap<>();
    public static void setFactorState(String key, String state) {
        factorUIStates.put(key, state);
        for (String k : factorUIStates.keySet()) {
            if (!k.equals(key)) {
                factorUIStates.put(k, "view");
            }
        }
    }
    public static String getFactorState(String key) {
        if (factorUIStates.keySet().contains(key)) {
            return factorUIStates.get(key);
        }
        else {
            return "view";
        }
    }

    public static void switchFactorState(String key, String state) {
        factorUIStates.put(key, state);
    }

    public static HashMap<ButtonKey, ObservedAnimation> anims = new HashMap<>();
    public static boolean addAnim(ButtonKey key, ObservedAnimation anim) {
        for (ButtonKey k : anims.keySet()) {
            // if we have an animation for this key but with different state
            if (k.key.equals(key.key) && !k.state.equals(key.state)) {
                anims.remove(k);
                anims.put(key, anim);
                return true;
            }
        }
        anims.put(key, anim);
        return false;
    }

    public static boolean hasFinishedAnim(ButtonKey key) {
        for (ButtonKey k : anims.keySet()) {
            if (k.key.equals(key.key) && k.state.equals(key.state) && anims.get(k).hasEnded) {
                // found an animation for element (symptom or factor) of this key and with an
                // already finished animation
                return true;
            }
        }
        return false;
    }


}
