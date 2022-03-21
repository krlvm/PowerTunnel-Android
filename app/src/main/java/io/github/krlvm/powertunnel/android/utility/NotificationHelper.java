/*
 * This file is part of PowerTunnel-Android.
 *
 * PowerTunnel-Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PowerTunnel-Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerTunnel-Android.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.krlvm.powertunnel.android.utility;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;

import io.github.krlvm.powertunnel.android.R;
import io.github.krlvm.powertunnel.android.activities.MainActivity;
import io.github.krlvm.powertunnel.android.receivers.ActionReceiver;
import io.github.krlvm.powertunnel.android.services.PowerTunnelService;

public class NotificationHelper {

    public static NotificationChannelGroup createConnectionGroup(Context context) {
        return createGroup(
                context,
                "CONNECTION",
                R.string.notification_group_connection,
                R.string.notification_group_connection_description
        );
    }


    public static Notification createConnectionNotification(
            Context context, String channel, @StringRes int title, @StringRes int text, @StringRes int disconnectText) {
        return new NotificationCompat.Builder(context, channel)
                .setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.ic_notification)
                .setShowWhen(false)

                .setColor(context.getResources().getColor(R.color.accent))

                .setContentTitle(context.getString(title))
                .setContentText(context.getString(text))

                .addAction(
                        R.drawable.ic_stop, context.getString(disconnectText),
                        PendingIntent.getBroadcast(
                                context, 0,
                                new Intent(context, ActionReceiver.class).setAction(PowerTunnelService.ACTION_STOP),
                                0
                        )
                )

                .setContentIntent(PendingIntent.getActivity(
                        context, 0,
                        new Intent(context, MainActivity.class), 0)
                )
                .build();
    }


    public static NotificationChannelGroup createGroup(Context context, String id, @StringRes int name, @StringRes int description) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return null;

        final NotificationChannelGroup group = new NotificationChannelGroup(id, context.getString(name));
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            group.setDescription(context.getString(description));
        }

        final NotificationManager manager = context.getSystemService(NotificationManager.class);
        if(manager == null) return null;
        manager.createNotificationChannelGroup(group);

        return group;
    }

    public static void registerChannel(Context context, String id, @StringRes int name, @StringRes int description) {
        registerChannel(context, id, name, description, null);
    }

    public static void registerChannel(Context context, String id, @StringRes int name, @StringRes int description, NotificationChannelGroup group) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return;

        final NotificationManager manager = context.getSystemService(NotificationManager.class);
        if(manager == null) return;

        final NotificationChannel channel = new NotificationChannel(
                id, context.getString(name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        if(group != null) channel.setGroup(group.getId());
        channel.setDescription(context.getString(description));
        channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
        channel.setImportance(NotificationManager.IMPORTANCE_MIN);
        channel.enableVibration(false);
        channel.enableLights(false);
        channel.setShowBadge(false);

        manager.createNotificationChannel(channel);
    }
}
