package ru.krlvm.powertunnel.android.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import ru.krlvm.powertunnel.PowerTunnel;
import ru.krlvm.powertunnel.android.MainActivity;
import ru.krlvm.powertunnel.android.R;
import ru.krlvm.powertunnel.android.managers.NotificationHelper;
import ru.krlvm.powertunnel.android.managers.PTManager;
import ru.krlvm.powertunnel.android.updater.Updater;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QuickTileService extends TileService {

    private boolean pendingAction = false;
    private BroadcastReceiver receiver = null;

    @Override
    public void onStartListening() {
        super.onStartListening();
        updateState();
        registerReceiver();
    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        if(receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateState();
    }

    @Override
    public void onClick() {
        super.onClick();
        if(pendingAction) {
            return;
        }
        boolean running = PowerTunnel.isRunning();
        setState(Tile.STATE_UNAVAILABLE);
        pendingAction = true;
        try {
            if (PowerTunnel.isRunning()) {
                PTManager.stopTunnel(this);
            } else {
                PTManager.startTunnel(this);
                checkUpdates();
            }
        } catch (Exception ex) {
            // in the most cases, we just receiving broadcast with startup fail
            openActivityOnError(ex.getLocalizedMessage());
            ex.printStackTrace();
        }
        setState(!running ? Tile.STATE_INACTIVE : Tile.STATE_ACTIVE);
    }

    private void updateState() {
        updateState(PowerTunnel.isRunning());
    }

    private void updateState(boolean isRunning) {
        setState(isRunning ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
    }

    private void setState(int state) {
        Tile tile = getQsTile();
        tile.setState(state);
        tile.updateTile();
    }

    private void openActivityOnError(String cause) {
        pendingAction = false;
        Toast.makeText(this, getString(R.string.qs_startup_failed, cause), Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MainActivity.SERVER_START_BROADCAST);
        filter.addAction(MainActivity.SERVER_STOP_BROADCAST);
        filter.addAction(MainActivity.STARTUP_FAIL_BROADCAST);
        registerReceiver(receiver = new StatusReceiver(), filter);
    }

    protected class StatusReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction() == null) {
                // just in case
                updateState();
                return;
            }
            if(intent.getAction().equals(MainActivity.STARTUP_FAIL_BROADCAST)) {
                openActivityOnError(intent.getExtras() == null ? "unknown" : intent.getExtras().getString("cause"));
                return;
            }
            pendingAction = false;
            updateState(intent.getAction().equals(MainActivity.SERVER_START_BROADCAST));
        }
    }

    private void checkUpdates() {
        Updater.checkUpdates((info) -> {
            if(info == null || !info.isReady()) return;
            PendingIntent intent = PendingIntent.getActivity(this, 0, Updater.getDownloadIntent(this, info), 0);
            NotificationHelper.prepareNotificationChannel(this, Updater.NOTIFICATION_CHANNEL);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, Updater.NOTIFICATION_CHANNEL)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setContentTitle(getString(R.string.update_available_title))
                    .setContentText(getString(R.string.update_available, info.getVersion()))
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentIntent(intent)
                    .addAction(R.drawable.ic_download, getString(R.string.download), intent)
                    .setAutoCancel(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if(notificationManager != null) {
                notificationManager.notify(NotificationHelper.ChannelIds.UPDATE, builder.build());
            }
        });
    }
}
