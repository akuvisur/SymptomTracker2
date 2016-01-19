package com.comag.aku.lifetracker.objects;

/**
 * Created by aku on 04/11/15.
 */
public class ButtonKey {
    public String key;
    public String state;
    public ButtonKey (String key, String state) {
        this.key = key;
        this.state = state;
    }
    @Override
    public int hashCode() {
        String s = key+state;
        return s.hashCode();
    }
    @Override
    public boolean equals(Object o) {
        ButtonKey k = (ButtonKey) o;
        return (k.key.equals(this.key) && k.state.equals(this.state));
    }

    public String toString() {
        return key + " : " + state;
    }
}