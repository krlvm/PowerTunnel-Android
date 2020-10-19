package ru.krlvm.powertunnel.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.preference.PreferenceManager;
import android.util.Log;

import ru.krlvm.powertunnel.android.managers.PTManager;
import tun.proxy.service.Tun2HttpVpnService;

public class BootReceiver extends BroadcastReceiver {

    public static final String PREF_RUNNING = "pref_running";

    private static final String LOG_TAG = "PowerTunnel.Boot";

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) return;
        if (!action.equals(Intent.ACTION_BOOT_COMPLETED) && !action.equals(Intent.ACTION_REBOOT) && !action.equals("android.intent.action.QUICKBOOT_POWERON")) {
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isRunning = prefs.getBoolean(PREF_RUNNING, false);
        if (!isRunning) {
            Log.i(LOG_TAG, "We don't have to start the app on boot");
            return;
        }

        if (PTManager.isVPN(prefs)) {
            Intent prepare = VpnService.prepare(context);
            Log.i(LOG_TAG, "Starting VPN...");
            if (prepare == null) {
                Tun2HttpVpnService.start(context);
            } else {
                Log.e(LOG_TAG, "VPN is not prepared");
            }
        } else {
            Log.i(LOG_TAG, "Starting proxy...");
            Exception ex = PTManager.safeStartProxy(context);
            if (ex != null) {
                Log.e(LOG_TAG, "Failed to start proxy: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}