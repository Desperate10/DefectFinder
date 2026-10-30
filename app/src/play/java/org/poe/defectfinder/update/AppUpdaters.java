package org.poe.defectfinder.update;

import android.content.Context;

/** The Play build is updated by Google Play. */
public final class AppUpdaters {

    private AppUpdaters() {}

    public static AppUpdater get(Context context) {
        return PlayAppUpdater.getInstance(context);
    }
}
