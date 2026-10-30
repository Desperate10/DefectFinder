package org.poe.defectfinder.update;

import android.app.Activity;

import androidx.annotation.Nullable;

/**
 * Offers app updates from inside the app. The play build asks Google Play (In-App Updates), the
 * direct build downloads the APK from the DefectFinder server. Both read the server's minimum
 * version: below it the update is required. Get it with AppUpdaters.get(), one per flavor.
 * All methods are called on the main thread.
 */
public interface AppUpdater {

    /** Checks closer together than this only repeat the last answer. */
    long CHECK_INTERVAL_MS = 15 * 60_000L;

    interface Listener {
        void onStateChanged(@Nullable UpdateState state);
    }

    /** null = nothing to offer (up to date, not checked yet, or offline). */
    @Nullable
    UpdateState getState();

    void setListener(@Nullable Listener listener);

    /** Asks the server (and Play); at most every CHECK_INTERVAL_MS unless force. */
    void check(boolean force);

    /** Available: download it (direct) or start Google Play's flow. Ready: install / restart. */
    void proceed(Activity activity);

    /** Hides an optional offer until the next launch. */
    void dismiss();
}
