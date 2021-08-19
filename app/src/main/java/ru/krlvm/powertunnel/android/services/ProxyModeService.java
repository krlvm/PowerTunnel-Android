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

package ru.krlvm.powertunnel.android.services;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import ru.krlvm.powertunnel.android.R;
import ru.krlvm.powertunnel.android.managers.NotificationHelper;
import ru.krlvm.powertunnel.android.managers.PTManager;

public class ProxyModeService extends Service {

    private static final String TAG = "PTProxyMode.Service";
    private static final String CHANNEL_ID = "PowerTunnelProxy";
    private static final int FOREGROUND_ID = NotificationHelper.ChannelIds.PROXY;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.prepareNotificationChannel(this, CHANNEL_ID);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = NotificationHelper.createMainActivityNotification(this,
                CHANNEL_ID, getString(R.string.app_name),
                getString(R.string.notification_proxy_background)
        );

        Log.i(TAG, "Starting proxy server...");
        PTManager.configure(this, PreferenceManager.getDefaultSharedPreferences(this));

        if(!PTManager.safeStartProxy(this)) {
            // failed to start
            stopForeground(true);
        } else {
            startForeground(FOREGROUND_ID, notification);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PTManager.stopProxy(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
