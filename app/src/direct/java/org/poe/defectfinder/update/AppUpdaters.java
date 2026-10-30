package org.poe.defectfinder.update;

import android.content.Context;

/** The direct build updates itself from the DefectFinder server. */
public final class AppUpdaters {

    private AppUpdaters() {}

    public static AppUpdater get(Context context) {
        return DirectAppUpdater.getInstance(context);
    }
}
