package org.poe.defectfinder.update;

import android.app.Activity;
import android.view.View;
import android.widget.Button;

import org.poe.defectfinder.R;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

/**
 * The update dialog on the main screen. Call start() from onStart and stop() from onStop: the
 * dialog follows the updater's state while the screen is visible and comes back on the next start.
 */
public class UpdatePrompt implements AppUpdater.Listener {

    private final Activity activity;
    private final AppUpdater updater;
    private AlertDialog dialog;

    public UpdatePrompt(Activity activity) {
        this.activity = activity;
        this.updater = AppUpdaters.get(activity.getApplicationContext());
    }

    public void start() {
        updater.setListener(this);
        onStateChanged(updater.getState());
        updater.check(false);
    }

    public void stop() {
        updater.setListener(null);
        close();
    }

    private void close() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    @Override
    public void onStateChanged(@Nullable UpdateState s) {
        if (s == null) {
            close();
            return;
        }
        if (dialog == null) {
            AlertDialog created = new AlertDialog.Builder(activity)
                    .setTitle(" ")
                    .setMessage(" ")
                    .setPositiveButton(" ", null)
                    .setNegativeButton(R.string.update_later, (d, w) -> updater.dismiss())
                    .setCancelable(false)
                    .create();
            // "Пізніше" closes it through the button; the listener runs later, so only forget this same dialog
            created.setOnDismissListener(d -> {
                if (dialog == created) dialog = null;
            });
            dialog = created;
            dialog.show();
            // replaces the default listener, which would close the dialog on every press
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> updater.proceed(activity));
        }

        String title = activity.getString(s.required ? R.string.update_required_title : R.string.update_title);
        dialog.setTitle(s.versionName != null ? title + " " + s.versionName : title);
        dialog.setMessage(message(s));

        String positive = null;
        boolean enabled = true;
        switch (s.phase) {
            case AVAILABLE:
                if (!s.unavailable) {
                    positive = activity.getString(s.playStore ? R.string.update_start_play : R.string.update_download);
                } else if (s.playStore) {
                    positive = activity.getString(R.string.update_open_play);
                }
                break;
            case READY:
                positive = activity.getString(s.playStore ? R.string.update_restart : R.string.update_install);
                break;
            case DOWNLOADING:
            case INSTALLING:
                positive = activity.getString(R.string.update_wait);
                enabled = false;
                break;
        }
        Button pos = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        pos.setVisibility(positive != null ? View.VISIBLE : View.GONE);
        if (positive != null) pos.setText(positive);
        pos.setEnabled(enabled);
        // a required update can't be put off, unless there is nothing to install yet
        boolean later = s.phase == UpdateState.Phase.AVAILABLE && (!s.required || s.unavailable);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setVisibility(later ? View.VISIBLE : View.GONE);
    }

    private String message(UpdateState s) {
        StringBuilder m = new StringBuilder();
        if (s.required) paragraph(m, activity.getString(R.string.update_required_text));
        if (s.unavailable) {
            paragraph(m, activity.getString(s.playStore ? R.string.update_unavailable_play : R.string.update_unavailable_direct));
        }
        if (!s.notes.isEmpty()) paragraph(m, s.notes);
        if (s.sizeBytes != null && s.phase == UpdateState.Phase.AVAILABLE && !s.unavailable) {
            paragraph(m, activity.getString(R.string.update_size, s.sizeBytes / (1024f * 1024f)));
        }
        switch (s.phase) {
            case DOWNLOADING:
                paragraph(m, s.progress != null
                        ? activity.getString(R.string.update_downloading_pct, Math.round(s.progress * 100))
                        : activity.getString(R.string.update_downloading));
                break;
            case READY:
                paragraph(m, activity.getString(s.playStore ? R.string.update_ready_play : R.string.update_ready_direct));
                break;
            case INSTALLING:
                paragraph(m, activity.getString(R.string.update_installing));
                break;
            default:
        }
        if (s.error != null) paragraph(m, s.error);
        return m.toString();
    }

    private static void paragraph(StringBuilder m, String text) {
        if (m.length() > 0) m.append("\n\n");
        m.append(text);
    }
}
