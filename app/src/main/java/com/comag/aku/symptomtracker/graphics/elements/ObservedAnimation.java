package com.comag.aku.symptomtracker.graphics.elements;

import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.util.Log;
import android.view.animation.Animation;

import com.comag.aku.symptomtracker.graphics.UIManager;
import com.comag.aku.symptomtracker.objects.ButtonKey;

/**
 * Created by aku on 04/11/15.
 */
public class ObservedAnimation extends AnimationDrawable {
    public boolean hasEnded = false;
    Handler mAnimationHandler;
    public ButtonKey key;


    public ObservedAnimation(AnimationDrawable aniDrawable, ButtonKey key) {
        super();
        this.key = key;
        for (int i = 0; i < aniDrawable.getNumberOfFrames(); i++) {
            this.addFrame(aniDrawable.getFrame(i), aniDrawable.getDuration(i));
        }
    }

    @Override
    public void start() {
        if(!hasEnded) {
            super.start();
            mAnimationHandler = new Handler();
            mAnimationHandler.postDelayed(new Runnable() {
                public void run() {
                    hasEnded = true;
                    //Log.d("animation", "has ended");
                }
            }, getTotalDuration());
        }
    }

    /**
     * Gets the total duration of all frames.
     *
     * @return The total duration.
     */
    public int getTotalDuration() {

        int iDuration = 0;

        for (int i = 0; i < this.getNumberOfFrames(); i++) {
            iDuration += this.getDuration(i);
        }

        return iDuration;
    }

}
