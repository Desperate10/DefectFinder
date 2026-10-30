package org.poe.defectfinder.update;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;

import org.poe.defectfinder.BuildConfig;
import org.poe.defectfinder.R;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/** PackageInstaller's answer to DirectAppUpdater's install session. */
public class UpdateInstallReceiver extends BroadcastReceiver {

    private static final String CHANNEL = "app_update";
    private static final int NOTIFICATION_ID = 7001;

    @Override
    public void onReceive(Context context, Intent intent) {
        DirectAppUpdater updater = DirectAppUpdater.getInstance(context);
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            // Android's "Update this app?" dialog; the app is in front, the worker just tapped "Встановити"
            Intent confirm = confirmIntent(intent);
            if (confirm != null) context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            updater.awaitingConfirmation();
        } else if (status == PackageInstaller.STATUS_SUCCESS) {
            // this runs in the new version's process; Android closed the app to replace it and won't let
            // it reopen itself from the background
            notifyUpdated(context);
        } else {
            updater.installFailed(status, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE));
        }
    }

    private static void notifyUpdated(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;  // refused: the install dialog already said to reopen the app
        }
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        manager.createNotificationChannel(new NotificationChannelCompat.Builder(
                        CHANNEL, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(context.getString(R.string.update_done_channel))
                .build());
        Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launch == null) return;
        PendingIntent open = PendingIntent.getActivity(context, 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        manager.notify(NOTIFICATION_ID, new NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_updated)
                .setContentTitle(context.getString(R.string.update_done_title))
                .setContentText(context.getString(R.string.update_done_text, BuildConfig.VERSION_NAME))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(open)
                .setAutoCancel(true)
                .build());
    }

    @SuppressWarnings("deprecation")
    private static Intent confirmIntent(Intent intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class);
        }
        return intent.getParcelableExtra(Intent.EXTRA_INTENT);
    }
}
