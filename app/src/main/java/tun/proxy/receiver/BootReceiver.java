package tun.proxy.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.preference.PreferenceManager;
import android.util.Log;

import ru.krlvm.powertunnel.android.MainActivity;
import tun.proxy.service.Tun2HttpVpnService;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent != null && !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(!MainActivity.isVPN(prefs)) {
            //We're in proxy mode
            return;
        }
        boolean isRunning = prefs.getBoolean(Tun2HttpVpnService.PREF_RUNNING, false);
        if (isRunning) {
            Intent prepare = VpnService.prepare(context);
            if (prepare == null) {
                Log.d("Tun2Http.Boot", "Starting vpn");
                Tun2HttpVpnService.start(context);
            } else {
                Log.d("Tun2Http.Boot", "Not prepared");
            }
        }
    }
}