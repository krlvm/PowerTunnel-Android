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

package io.github.krlvm.powertunnel.android.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import io.github.krlvm.powertunnel.android.R;
import io.github.krlvm.powertunnel.android.managers.ProxyManager;
import io.github.krlvm.powertunnel.android.receivers.BootReceiver;
import io.github.krlvm.powertunnel.android.types.GlobalStatus;
import io.github.krlvm.powertunnel.android.types.TunnelMode;
import io.github.krlvm.powertunnel.android.utility.NotificationHelper;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyStatus;

public class ProxyService extends Service {

    private static final String LOG_TAG = "ProxyService";

    private static final String NOTIFICATION_CHANNEL_ID = TunnelMode.PROXY.name();
    private static final int FOREGROUND_ID = TunnelMode.PROXY.id();

    private ProxyManager proxy;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.registerChannel(
                this,
                NOTIFICATION_CHANNEL_ID, R.string.notification_channel_proxy,
                R.string.notification_channel_proxy_description,
                NotificationHelper.createConnectionGroup(this)
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        proxy = new ProxyManager(
                this,
                (status) -> {
                    if (status == ProxyStatus.RUNNING) {
                        startForeground(FOREGROUND_ID, NotificationHelper.createConnectionNotification(
                                this, NOTIFICATION_CHANNEL_ID,
                                R.string.notification_title_proxy,
                                R.string.notification_message_proxy,
                                R.string.action_stop
                        ));
                        PowerTunnelService.STATUS = GlobalStatus.PROXY;
                        sendBroadcast(new Intent(PowerTunnelService.BROADCAST_STARTED));
                        BootReceiver.rememberState(this, true);
                    } else if (status == ProxyStatus.NOT_RUNNING) {
                        stopSelf();
                    }
                },
                (error) -> {
                    Log.w(LOG_TAG, "Proxy Server could not start, stopping...");
                    sendBroadcast(new Intent(PowerTunnelService.BROADCAST_FAILURE)
                            .putExtra(PowerTunnelService.EXTRAS_MODE, TunnelMode.PROXY)
                            .putExtra(PowerTunnelService.EXTRAS_ERROR, error)
                    );
                    stopSelf();
                },
                TunnelingVpnService.getDefaultDNS(this)
        );
        proxy.start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        proxy.stop();
        proxy = null;

        stopForeground(true);

        PowerTunnelService.STATUS = GlobalStatus.NOT_RUNNING;
        sendBroadcast(new Intent(PowerTunnelService.BROADCAST_STOPPED));
        BootReceiver.rememberState(this, false);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
