package org.poe.defectfinder.network;

/** GET /app/release: the APK for phones that don't update through Google Play, and the oldest version still allowed. */
public class AppRelease {
    public int versionCode;
    public String versionName;
    /** Below it the update is required (and the server refuses uploads). */
    public int minVersionCode;
    public String notes;
    /** Relative to the API base URL; null when the server has the version guard only, no APK. */
    public String apk;
    public Long size;
    public String sha256;
}
