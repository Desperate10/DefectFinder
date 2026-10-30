package org.poe.defectfinder.update;

/** What the update dialog shows. Treated as immutable: changes go through copy(). */
public class UpdateState {

    public enum Phase { AVAILABLE, DOWNLOADING, READY, INSTALLING }

    /** The new version's name; null when Play doesn't tell it. */
    public String versionName;
    public String notes = "";
    /** Below the server's minimum: the dialog can't be dismissed. */
    public boolean required;
    public Long sizeBytes;
    public Phase phase = Phase.AVAILABLE;
    /** 0..1 while downloading, null when the size is unknown. */
    public Float progress;
    /** The last attempt failed: why, in words for the worker. Proceeding retries. */
    public String error;
    /** Required, but there is nothing to install here (Play hasn't got it yet, the server has no APK). */
    public boolean unavailable;
    /** Google Play does the work: other button words ("Відкрити Google Play", restart instead of install). */
    public boolean playStore;

    UpdateState copy() {
        UpdateState s = new UpdateState();
        s.versionName = versionName;
        s.notes = notes;
        s.required = required;
        s.sizeBytes = sizeBytes;
        s.phase = phase;
        s.progress = progress;
        s.error = error;
        s.unavailable = unavailable;
        s.playStore = playStore;
        return s;
    }
}
