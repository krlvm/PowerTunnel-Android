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

package ru.krlvm.powertunnel.android.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import ru.krlvm.powertunnel.android.managers.PTManager;
import ru.krlvm.powertunnel.android.services.ProxyModeService;
import tun.proxy.service.Tun2HttpVpnService;

public class BootReceiver extends BroadcastReceiver {

    public static final String PREF_RUNNING = "pref_running";

    private static final String LOG_TAG = "PowerTunnel.Boot";

    @Override
    public void onReceive(Context context, Intent intent) {
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
            Log.i(LOG_TAG, "Starting proxy mode service...");
            Intent proxy = new Intent(context, ProxyModeService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(proxy);
            } else {
                context.startService(proxy);
            }
        }
    }
}