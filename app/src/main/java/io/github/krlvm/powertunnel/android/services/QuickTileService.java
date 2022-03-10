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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.VpnService;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import io.github.krlvm.powertunnel.android.R;
import io.github.krlvm.powertunnel.android.activities.MainActivity;
import io.github.krlvm.powertunnel.android.managers.ConfigurationManager;
import io.github.krlvm.powertunnel.android.types.TunnelMode;
import io.github.krlvm.powertunnel.android.updater.Updater;
import io.github.krlvm.powertunnel.android.utility.NotificationHelper;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QuickTileService extends TileService {

    private static final String LOG_TAG = "QSTile";
    private BroadcastReceiver receiver = null;

    private void setState(int state) {
        final Tile tile = getQsTile();
        tile.setState(state);
        tile.updateTile();
    }

    private void setState(boolean isRunning) {
        setState(isRunning ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
    }

    private void updateState() {
        setState(PowerTunnelService.isRunning());
    }

    private boolean isPending() {
        return getQsTile().getState() == Tile.STATE_UNAVAILABLE;
    }

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateState();
        registerReceiver();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        if (receiver == null) return;
        unregisterReceiver(receiver);
        receiver = null;
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateState();
    }

    @Override
    public void onClick() {
        super.onClick();
        if(isPending()) return;

        unlockAndRun(this::handleClick);
    }

    public void handleClick() {
        setState(Tile.STATE_ACTIVE);
        setState(Tile.STATE_UNAVAILABLE);

        try {
            if(!ConfigurationManager.checkStorageAccess(this)) {
                launchActivity();
                return;
            }
            if (PowerTunnelService.isRunning()) {
                Toast.makeText(this, R.string.qs_stopping, Toast.LENGTH_SHORT).show();
                PowerTunnelService.stopTunnel(this);
            } else {
                if(PowerTunnelService.getTunnelMode(this) == TunnelMode.VPN && VpnService.prepare(this) != null) {
                    launchActivity();
                    return;
                }
                Toast.makeText(this, R.string.qs_starting, Toast.LENGTH_SHORT).show();
                PowerTunnelService.startTunnel(this);
                checkUpdates();
            }
        } catch (Exception ex) {
            Log.e(LOG_TAG, "Failed to change PowerTunnel state: " + ex.getMessage(), ex);
            updateState();
            showErrorToast();
            launchActivity();
        }
    }

    private void showErrorToast() {
        Toast.makeText(this, R.string.qs_startup_failed, Toast.LENGTH_LONG).show();
    }

    private void launchActivity() {
        startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private void registerReceiver() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(PowerTunnelService.BROADCAST_STARTED);
        filter.addAction(PowerTunnelService.BROADCAST_STOPPED);
        filter.addAction(PowerTunnelService.BROADCAST_FAILURE);
        registerReceiver(receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateState();
                if (PowerTunnelService.BROADCAST_FAILURE.equals(intent.getAction())) {
                    showErrorToast();
                    launchActivity();
                }
            }
        }, filter);
    }

    private void checkUpdates() {
        Updater.checkUpdatesIfNecessary(this, (info) -> {
            if (info == null || !info.isReady()) return;
            NotificationHelper.registerChannel(this, Updater.NOTIFICATION_CHANNEL_ID,
                    R.string.notification_channel_updater, R.string.notification_channel_updater_description);

            final NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager == null) return;
            notificationManager.notify(5, new NotificationCompat.Builder(this, Updater.NOTIFICATION_CHANNEL_ID)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setContentTitle(getString(R.string.dialog_update_available_title))
                    .setContentText(getString(R.string.dialog_update_available_message, info.getVersion()))
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(PendingIntent.getActivity(this, 0, Updater.getDownloadIntent(info), 0))
                    .addAction(
                            R.drawable.ic_download, getString(R.string.download),
                            PendingIntent.getActivity(this, 0, Updater.getDownloadIntent(info), 0)
                    )
                    .setAutoCancel(true)
                    .build()
            );
        });
    }
}
