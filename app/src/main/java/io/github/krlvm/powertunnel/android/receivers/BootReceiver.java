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

package io.github.krlvm.powertunnel.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.util.Log;

import androidx.preference.PreferenceManager;

import io.github.krlvm.powertunnel.android.services.PowerTunnelService;
import io.github.krlvm.powertunnel.android.types.TunnelMode;

public class BootReceiver extends BroadcastReceiver {

    private static final String LOG_TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(action) &&
                !Intent.ACTION_REBOOT.equals(action) &&
                !"android.intent.action.QUICKBOOT_POWERON".equals(action)) return;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (!prefs.getBoolean("autostart", false)) return;
        if (!prefs.getBoolean("was_running", false)) return;

        Log.i(LOG_TAG, "Starting PowerTunnel...");

        if (PowerTunnelService.getTunnelMode(context) == TunnelMode.VPN &&
                VpnService.prepare(context) != null)
            return;

        PowerTunnelService.startTunnel(context);
    }

    public static void rememberState(Context context, boolean isRunning) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean("was_running", isRunning)
                .apply();
    }
}
