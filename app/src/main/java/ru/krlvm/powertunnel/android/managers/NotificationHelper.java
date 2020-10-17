package ru.krlvm.powertunnel.android.managers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import ru.krlvm.powertunnel.android.MainActivity;
import ru.krlvm.powertunnel.android.R;
import ru.krlvm.powertunnel.android.receiver.ActionReceiver;

public class NotificationHelper {

    public static void prepareNotificationChannel(Context context, String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, context.getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW);
            android.app.NotificationManager notificationManager = context.getSystemService(android.app.NotificationManager.class);
            if(notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static Notification createMainActivityNotification(Service service, String channelId, String title, String text) {
        Intent notificationIntent = new Intent(service, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, notificationIntent, 0);

        Intent stopIntent = new Intent(service, ActionReceiver.class);
        stopIntent.setAction(ActionReceiver.ACTION_STOP_TUNNEL);

        return new NotificationCompat.Builder(service, channelId)
                .setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .addAction(R.drawable.ic_stop_tunnel, service.getString(R.string.server_stop), PendingIntent.getBroadcast(service, 0, stopIntent, 0))
                .setContentIntent(pendingIntent)
                .build();
    }
}
