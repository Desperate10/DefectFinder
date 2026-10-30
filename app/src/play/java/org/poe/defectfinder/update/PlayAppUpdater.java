package org.poe.defectfinder.update;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

import org.poe.defectfinder.BuildConfig;
import org.poe.defectfinder.R;
import org.poe.defectfinder.network.AppRelease;
import org.poe.defectfinder.network.ClientApi;
import org.poe.defectfinder.network.MyApi;

import androidx.annotation.NonNull;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * The Play build: Google Play downloads and installs (In-App Updates). An optional update downloads in
 * the background (flexible) and is applied by a restart; a required one, below the server's minimum,
 * uses Play's full-screen flow. The server's release notes are shown when its versionCode is Play's.
 */
public class PlayAppUpdater extends BaseAppUpdater {

    private static final String TAG = "PlayAppUpdater";

    private static PlayAppUpdater instance;

    public static synchronized PlayAppUpdater getInstance(Context context) {
        if (instance == null) instance = new PlayAppUpdater(context.getApplicationContext());
        return instance;
    }

    private final Context context;
    private final MyApi api;
    private final AppUpdateManager manager;

    private AppUpdateInfo info;
    private AppRelease release;
    private long lastServerCheck;
    private int dismissedCode;
    private boolean busy;

    private PlayAppUpdater(Context context) {
        this.context = context;
        this.api = ClientApi.getInstance(context).getApi();
        this.manager = AppUpdateManagerFactory.create(context);
        // the updater lives as long as the process
        manager.registerListener(this::onInstallState);
    }

    private void onInstallState(InstallState st) {
        switch (st.installStatus()) {
            case InstallStatus.PENDING:
                update(s -> {
                    s.phase = UpdateState.Phase.DOWNLOADING;
                    s.progress = null;
                    s.error = null;
                });
                break;
            case InstallStatus.DOWNLOADING:
                long total = st.totalBytesToDownload();
                Float progress = total > 0 ? (float) st.bytesDownloaded() / total : null;
                update(s -> {
                    s.phase = UpdateState.Phase.DOWNLOADING;
                    s.progress = progress;
                    s.error = null;
                });
                break;
            case InstallStatus.DOWNLOADED:
                update(s -> {
                    s.phase = UpdateState.Phase.READY;
                    s.error = null;
                });
                break;
            case InstallStatus.INSTALLING:
                update(s -> s.phase = UpdateState.Phase.INSTALLING);
                break;
            case InstallStatus.FAILED:
                int code = st.installErrorCode();
                update(s -> {
                    s.phase = UpdateState.Phase.AVAILABLE;
                    s.error = context.getString(R.string.update_err_play_download, code);
                });
                break;
            case InstallStatus.CANCELED:
                update(s -> {
                    s.phase = UpdateState.Phase.AVAILABLE;
                    s.progress = null;
                });
                break;
            default:
        }
    }

    @Override
    public void check(boolean force) {
        if (busy) return;
        busy = true;
        long now = SystemClock.elapsedRealtime();
        if (force || lastServerCheck == 0 || now - lastServerCheck >= CHECK_INTERVAL_MS) {
            lastServerCheck = now;
            api.release().enqueue(new Callback<AppRelease>() {
                @Override
                public void onResponse(@NonNull Call<AppRelease> call, @NonNull Response<AppRelease> response) {
                    if (response.isSuccessful()) release = response.body();
                    else if (response.code() == 404) release = null;
                    askPlay();
                }

                @Override
                public void onFailure(@NonNull Call<AppRelease> call, @NonNull Throwable t) {
                    // offline: keep the last minimum
                    lastServerCheck = 0;
                    askPlay();
                }
            });
        } else {
            askPlay();
        }
    }

    /** Play's answer is local and quick: always fresh, so a download finished meanwhile shows up. */
    private void askPlay() {
        manager.getAppUpdateInfo()
                .addOnSuccessListener(i -> {
                    info = i;
                    checked();
                })
                .addOnFailureListener(e -> {
                    // not installed from Play, no Play Store, …
                    Log.i(TAG, "Play update info: " + e.getMessage());
                    info = null;
                    checked();
                });
    }

    private void checked() {
        busy = false;
        UpdateState s = getState();
        boolean keep = s != null && (s.phase == UpdateState.Phase.DOWNLOADING || s.phase == UpdateState.Phase.INSTALLING);
        if (!keep) publish(offer());
    }

    private UpdateState offer() {
        AppUpdateInfo i = info;
        AppRelease r = release;
        boolean required = r != null && BuildConfig.VERSION_CODE < r.minVersionCode;
        boolean inPlay = i != null && (
                i.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        || i.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                        || i.installStatus() == InstallStatus.DOWNLOADED);
        if (!inPlay && !required) return null;
        int code = inPlay ? i.availableVersionCode() : 0;
        if (!required && code == dismissedCode) return null;
        UpdateState s = new UpdateState();
        // the server describes the same version: its name and notes
        if (r != null && (r.versionCode == code || !inPlay)) {
            s.versionName = r.versionName;
            s.notes = r.notes != null ? r.notes : "";
        }
        s.required = required;
        if (!inPlay) {
            s.phase = UpdateState.Phase.AVAILABLE;
        } else if (i.installStatus() == InstallStatus.DOWNLOADED) {
            s.phase = UpdateState.Phase.READY;
        } else if (i.installStatus() == InstallStatus.DOWNLOADING || i.installStatus() == InstallStatus.PENDING) {
            s.phase = UpdateState.Phase.DOWNLOADING;
        } else {
            s.phase = UpdateState.Phase.AVAILABLE;
        }
        s.unavailable = !inPlay;
        s.playStore = true;
        return s;
    }

    @Override
    public void proceed(Activity activity) {
        UpdateState s = getState();
        if (s == null) return;
        switch (s.phase) {
            case READY:
                manager.completeUpdate();  // installs and restarts the app
                break;
            case AVAILABLE:
                if (s.unavailable) openStore(activity);
                else startFlow(activity, s.required);
                break;
            default:
        }
    }

    private void startFlow(Activity activity, boolean required) {
        AppUpdateInfo i = info;
        if (i == null) return;
        boolean immediate = (required || i.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)
                && i.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE);
        int type;
        if (immediate) type = AppUpdateType.IMMEDIATE;
        else if (i.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) type = AppUpdateType.FLEXIBLE;
        else if (i.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) type = AppUpdateType.IMMEDIATE;
        else {
            openStore(activity);
            return;
        }
        update(s -> s.error = null);
        manager.startUpdateFlow(i, activity, AppUpdateOptions.newBuilder(type).build())
                .addOnFailureListener(e -> {
                    Log.w(TAG, "update flow failed", e);
                    update(s -> s.error = context.getString(R.string.update_err_play_flow));
                });
        // the AppUpdateInfo is spent once a flow started: the next proceed needs a fresh one
        check(false);
    }

    /** Play has no newer version for this phone yet (or isn't the installer): its store page. */
    private void openStore(Activity activity) {
        String id = context.getPackageName();
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + id))
                    .setPackage("com.android.vending"));
        } catch (ActivityNotFoundException e) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + id)));
        }
    }

    @Override
    public void dismiss() {
        UpdateState s = getState();
        if (s == null || s.required || s.phase != UpdateState.Phase.AVAILABLE) return;
        dismissedCode = info != null ? info.availableVersionCode() : 0;
        publish(null);
    }
}
