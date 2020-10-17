package ru.krlvm.powertunnel.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import ru.krlvm.powertunnel.android.managers.PTManager;

public class ActionReceiver extends BroadcastReceiver {

    public static final String ACTION_START_TUNNEL = "ru.krlvm.powertunnel.android.action.START_TUNNEL";
    public static final String ACTION_STOP_TUNNEL = "ru.krlvm.powertunnel.android.action.STOP_TUNNEL";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction() == null) return;

        switch (intent.getAction()) {
            case ACTION_START_TUNNEL: {
                PTManager.startTunnel(context);
                break;
            }
            case ACTION_STOP_TUNNEL: {
                PTManager.stopWithToast(context);
                break;
            }
        }
    }
}
