package com.comag.aku.lifetracker.model;

import com.comag.aku.lifetracker.objects.ValueMap;
import com.comag.aku.lifetracker.objects.tracking.Condition;

/**
 * Created by aku on 26/11/15.
 */
public class DataObject {
    public Condition c;
    public ValueMap v;

    DataObject(Condition c, ValueMap v) {
        this.c = c;
        this.v = v;
    }

    public String getId() {
        return String.valueOf(c.toString().hashCode());
    }

    public String toRenderableString() {
        return c.key + ": " + v.getValue();
    }
}
