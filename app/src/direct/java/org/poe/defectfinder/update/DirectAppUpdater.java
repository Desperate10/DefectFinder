package org.poe.defectfinder.update;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import org.poe.defectfinder.BuildConfig;
import org.poe.defectfinder.R;
import org.poe.defectfinder.network.AppRelease;
import org.poe.defectfinder.network.ClientApi;
import org.poe.defectfinder.network.MyApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.PackageInfoCompat;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * The sideloaded build updates itself from the DefectFinder server: GET /app/release, download the APK
 * into the cache (resuming a dropped download), check its SHA-256, package and signing key, then hand
 * it to PackageInstaller. Android asks the worker to confirm, except when this app installed itself last time.
 */
public class DirectAppUpdater extends BaseAppUpdater {

    private static final String TAG = "DirectAppUpdater";
    private static final int NOTIFICATIONS_REQUEST = 7002;

    private static DirectAppUpdater instance;

    public static synchronized DirectAppUpdater getInstance(Context context) {
        if (instance == null) instance = new DirectAppUpdater(context.getApplicationContext());
        return instance;
    }

    private final Context context;
    private final MyApi api;
    private final OkHttpClient http = new OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build();
    private final File dir;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private AppRelease release;
    /** The downloaded APK whose hash, package and key were checked. */
    private File verified;
    private long lastServerCheck;
    private int dismissedCode;
    /** A check or download is running. */
    private boolean busy;

    private DirectAppUpdater(Context context) {
        this.context = context;
        this.api = ClientApi.getInstance(context).getApi();
        this.dir = new File(context.getCacheDir(), "update");
    }

    @Override
    public void check(boolean force) {
        if (busy) return;
        long now = SystemClock.elapsedRealtime();
        if (!force && lastServerCheck != 0 && now - lastServerCheck < CHECK_INTERVAL_MS) return;
        lastServerCheck = now;
        busy = true;
        api.release().enqueue(new Callback<AppRelease>() {
            @Override
            public void onResponse(@NonNull Call<AppRelease> call, @NonNull Response<AppRelease> response) {
                busy = false;
                if (response.isSuccessful()) {
                    release = response.body();
                } else if (response.code() == 404) {
                    release = null;  // nothing released
                } else {
                    return;
                }
                // installed already (or withdrawn): the downloaded APK is dead weight
                if (release == null || release.versionCode <= BuildConfig.VERSION_CODE) {
                    verified = null;
                    executor.execute(() -> deleteAll(dir));
                }
                publish(offer());
            }

            @Override
            public void onFailure(@NonNull Call<AppRelease> call, @NonNull Throwable t) {
                // offline in the field is normal: keep whatever was offered
                busy = false;
                lastServerCheck = 0;
            }
        });
    }

    private UpdateState offer() {
        AppRelease r = release;
        if (r == null) return null;
        boolean required = BuildConfig.VERSION_CODE < r.minVersionCode;
        boolean newer = r.versionCode > BuildConfig.VERSION_CODE && r.apk != null && r.sha256 != null;
        if (!newer && !required) return null;
        if (!required && r.versionCode == dismissedCode) return null;
        boolean ready = newer && verified != null && verified.isFile() && verified.getName().equals(apkName(r));
        UpdateState s = new UpdateState();
        s.versionName = r.versionName;
        s.notes = r.notes != null ? r.notes : "";
        s.required = required;
        s.sizeBytes = r.size;
        s.phase = ready ? UpdateState.Phase.READY : UpdateState.Phase.AVAILABLE;
        s.unavailable = !newer;
        return s;
    }

    @Override
    public void proceed(Activity activity) {
        UpdateState s = getState();
        if (s == null) return;
        switch (s.phase) {
            case AVAILABLE:
                if (!s.unavailable) {
                    askNotifications(activity);
                    startDownload();
                }
                break;
            case READY:
                install(activity);
                break;
            default:
        }
    }

    @Override
    public void dismiss() {
        UpdateState s = getState();
        if (s == null || s.required || s.phase != UpdateState.Phase.AVAILABLE) return;
        dismissedCode = release != null ? release.versionCode : 0;
        publish(null);
    }

    /**
     * Android closes the app to install the update and won't let it reopen itself: the "DefectFinder оновлено"
     * notification is the way back. Asked while the download runs; a refusal only loses the notification.
     */
    private void askNotifications(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATIONS_REQUEST);
        }
    }

    private void startDownload() {
        AppRelease r = release;
        if (r == null || busy) return;
        busy = true;
        update(s -> {
            s.phase = UpdateState.Phase.DOWNLOADING;
            s.progress = null;
            s.error = null;
        });
        executor.execute(() -> {
            try {
                File file = download(r);
                main.post(() -> {
                    busy = false;
                    verified = file;
                    publish(offer());
                });
            } catch (ApkProblem e) {
                fail(e.getMessage());
            } catch (IOException e) {
                Log.w(TAG, "download failed", e);
                fail(context.getString(R.string.update_err_interrupted));
            } catch (Exception e) {
                Log.w(TAG, "download failed", e);
                String why = e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.getClass().getSimpleName();
                fail(context.getString(R.string.update_err_download, why));
            }
        });
    }

    /** From the background thread. */
    private void fail(String message) {
        main.post(() -> {
            busy = false;
            UpdateState s = offer();
            if (s != null) s.error = message;
            publish(s);
        });
    }

    private static String apkName(AppRelease r) {
        return r.sha256.toLowerCase() + ".apk";
    }

    /** On the background thread. */
    private File download(AppRelease r) throws IOException, ApkProblem {
        dir.mkdirs();
        File file = new File(dir, apkName(r));
        // earlier releases' downloads
        File[] old = dir.listFiles();
        if (old != null) {
            for (File f : old) if (!f.equals(file)) f.delete();
        }

        Long size = r.size;
        long have = file.isFile() ? file.length() : 0;
        if (size != null && have > size) {
            file.delete();
            have = 0;
        }
        if (size == null || have < size) {
            HttpUrl base = HttpUrl.get(ClientApi.BASE_URL);
            HttpUrl url = base.resolve(r.apk);
            if (url == null) throw new ApkProblem(context.getString(R.string.update_err_url));
            Request.Builder request = new Request.Builder().url(url);
            if (have > 0) request.header("Range", "bytes=" + have + "-");
            try (okhttp3.Response response = http.newCall(request.build()).execute()) {
                if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());
                // 200 instead of 206: the server sent the whole file again
                if (response.code() != 206) have = 0;
                ResponseBody body = response.body();
                if (body == null) throw new IOException("empty body");
                Long total = size != null ? size : (body.contentLength() >= 0 ? body.contentLength() + have : null);
                try (InputStream in = body.byteStream(); OutputStream out = new FileOutputStream(file, have > 0)) {
                    byte[] buf = new byte[64 * 1024];
                    long done = have;
                    int shown = -1;
                    int n;
                    while ((n = in.read(buf)) >= 0) {
                        out.write(buf, 0, n);
                        done += n;
                        if (total != null && total > 0) {
                            int pct = (int) (done * 100 / total);
                            if (pct != shown) {
                                shown = pct;
                                float progress = (float) done / total;
                                main.post(() -> update(s -> {
                                    if (s.phase == UpdateState.Phase.DOWNLOADING) s.progress = progress;
                                }));
                            }
                        }
                    }
                }
            }
        }

        if (!sha256(file).equalsIgnoreCase(r.sha256)) {
            file.delete();
            throw new ApkProblem(context.getString(R.string.update_err_corrupt));
        }
        checkApk(file);
        return file;
    }

    /** What PackageInstaller would refuse anyway, but in words the worker understands, before they confirm. */
    private void checkApk(File file) throws ApkProblem {
        PackageManager pm = context.getPackageManager();
        PackageInfo archive = pm.getPackageArchiveInfo(file.getPath(), signatureFlags());
        if (archive == null) {
            file.delete();
            throw new ApkProblem(context.getString(R.string.update_err_not_app));
        }
        if (!context.getPackageName().equals(archive.packageName)) {
            file.delete();
            throw new ApkProblem(context.getString(R.string.update_err_other_app, archive.packageName));
        }
        if (PackageInfoCompat.getLongVersionCode(archive) <= BuildConfig.VERSION_CODE) {
            file.delete();
            throw new ApkProblem(context.getString(R.string.update_err_old_version));
        }
        Set<String> installed;
        try {
            installed = signers(pm.getPackageInfo(context.getPackageName(), signatureFlags()));
        } catch (PackageManager.NameNotFoundException e) {
            installed = new HashSet<>();
        }
        Set<String> incoming = signers(archive);
        if (!installed.isEmpty() && !incoming.isEmpty() && !incoming.containsAll(installed)) {
            file.delete();
            throw new ApkProblem(context.getString(R.string.update_err_other_key));
        }
    }

    @SuppressLint("RequestInstallPackagesPolicy")
    private void install(Activity activity) {
        File file = verified;
        if (file == null || !file.isFile()) {
            publish(offer());
            return;
        }
        PackageManager pm = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !pm.canRequestPackageInstalls()) {
            update(s -> s.error = context.getString(R.string.update_err_permission));
            activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + context.getPackageName())));
            return;
        }
        update(s -> {
            s.phase = UpdateState.Phase.INSTALLING;
            s.error = null;
        });
        executor.execute(() -> {
            try {
                PackageInstaller installer = pm.getPackageInstaller();
                PackageInstaller.SessionParams params =
                        new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                params.setAppPackageName(context.getPackageName());
                params.setSize(file.length());
                // no confirmation when this app was also the one that installed itself last time
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
                }
                int id = installer.createSession(params);
                try (PackageInstaller.Session session = installer.openSession(id)) {
                    try (OutputStream out = session.openWrite("base.apk", 0, file.length());
                         InputStream in = new FileInputStream(file)) {
                        byte[] buf = new byte[64 * 1024];
                        int n;
                        while ((n = in.read(buf)) >= 0) out.write(buf, 0, n);
                        session.fsync(out);
                    }
                    // the installer adds the status to the intent, so it must stay mutable
                    int flags = PendingIntent.FLAG_UPDATE_CURRENT
                            | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0);
                    PendingIntent callback = PendingIntent.getBroadcast(
                            context, id, new Intent(context, UpdateInstallReceiver.class), flags);
                    session.commit(callback.getIntentSender());
                }
            } catch (Exception e) {
                Log.w(TAG, "install failed", e);
                main.post(() -> installFailed(PackageInstaller.STATUS_FAILURE, e.getLocalizedMessage()));
            }
        });
    }

    /** Android shows its confirmation; if the worker backs out of it, the button must work again. */
    void awaitingConfirmation() {
        update(s -> s.phase = UpdateState.Phase.READY);
    }

    void installFailed(int status, String message) {
        Log.w(TAG, "install status " + status + ": " + message);
        String text;
        switch (status) {
            case PackageInstaller.STATUS_FAILURE_ABORTED:
                text = null;  // the worker cancelled
                break;
            case PackageInstaller.STATUS_FAILURE_STORAGE:
                text = context.getString(R.string.update_err_storage);
                break;
            case PackageInstaller.STATUS_FAILURE_CONFLICT:
            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                text = context.getString(R.string.update_err_conflict);
                break;
            case PackageInstaller.STATUS_FAILURE_BLOCKED:
                text = context.getString(R.string.update_err_blocked);
                break;
            default:
                text = context.getString(R.string.update_err_install, message != null ? ": " + message : "");
        }
        update(s -> {
            s.phase = UpdateState.Phase.READY;
            s.error = text;
        });
    }

    private static class ApkProblem extends Exception {
        ApkProblem(String message) {
            super(message);
        }
    }

    @SuppressWarnings("deprecation")
    private static int signatureFlags() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                ? PackageManager.GET_SIGNING_CERTIFICATES
                : PackageManager.GET_SIGNATURES;
    }

    /** SHA-256 of every certificate the package is or was signed with (key rotation keeps the old ones). */
    @SuppressWarnings("deprecation")
    private static Set<String> signers(PackageInfo info) {
        Signature[] certs;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            SigningInfo s = info.signingInfo;
            if (s == null) return new HashSet<>();
            certs = s.hasMultipleSigners() ? s.getApkContentsSigners() : s.getSigningCertificateHistory();
        } else {
            certs = info.signatures;
        }
        Set<String> result = new HashSet<>();
        if (certs == null) return result;
        for (Signature c : certs) {
            try {
                result.add(hex(MessageDigest.getInstance("SHA-256").digest(c.toByteArray())));
            } catch (Exception ignored) {
                // SHA-256 is always there
            }
        }
        return result;
    }

    private static String sha256(File file) throws IOException {
        try (InputStream in = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) >= 0) md.update(buf, 0, n);
            return hex(md.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static void deleteAll(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) f.delete();
        }
        dir.delete();
    }
}
