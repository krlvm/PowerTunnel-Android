package ru.krlvm.powertunnel.android.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

import ru.krlvm.powertunnel.PowerTunnel;
import ru.krlvm.powertunnel.android.MainActivity;
import ru.krlvm.powertunnel.android.managers.PTManager;

@RequiresApi(api = Build.VERSION_CODES.N)
public class QuickTileService extends TileService {

    @Override
    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MainActivity.SERVER_START_BROADCAST);
        filter.addAction(MainActivity.SERVER_STOP_BROADCAST);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null) {
                    updateState(intent.getAction().equals(MainActivity.SERVER_START_BROADCAST));
                } else {
                    updateState();
                }
            }
        }, filter);
    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        updateState();
    }

    @Override
    public void onClick() {
        super.onClick();
        boolean running = PowerTunnel.isRunning();
        setState(Tile.STATE_UNAVAILABLE);
        try {
            if (PowerTunnel.isRunning()) {
                PTManager.stopTunnel(this);
            } else {
                PTManager.startTunnel(this);
            }
        } catch (Exception ex) {
            openActivity();
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

    private void openActivity() {
        startActivity(new Intent(this, MainActivity.class));
    }
}
