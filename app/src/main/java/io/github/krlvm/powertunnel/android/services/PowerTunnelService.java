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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.preference.PreferenceManager;

import io.github.krlvm.powertunnel.android.types.GlobalStatus;
import io.github.krlvm.powertunnel.android.types.TunnelMode;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyAddress;

public abstract class PowerTunnelService {

    private static final String LOG_TAG = "SvcManager";
    protected static GlobalStatus STATUS = GlobalStatus.NOT_RUNNING;

    public static final String ACTION_START = "io.github.krlvm.powertunnel.android.action.START";
    public static final String ACTION_STOP =  "io.github.krlvm.powertunnel.android.action.STOP";

    public static final String BROADCAST_STARTED = "io.github.krlvm.powertunnel.android.broadcast.STARTED";
    public static final String BROADCAST_STOPPED = "io.github.krlvm.powertunnel.android.broadcast.STOPPED";
    public static final String BROADCAST_FAILURE = "io.github.krlvm.powertunnel.android.broadcast.FAILURE";

    public static final String BROADCAST_CERT = "io.github.krlvm.powertunnel.android.broadcast.CERT";

    public static final String EXTRAS_MODE = "mode";
    public static final String EXTRAS_ERROR = "error";
    public static final String EXTRAS_ERROR_FW = "isFWError";


    public static void startTunnel(Context context) {
        startService(context, getTunnelMode(context));
    }

    public static void startService(Context context, TunnelMode mode) {
        PowerTunnelService.connect(context, mode.getServiceClass());
    }

    public static void stopTunnel(Context context) {
        final TunnelMode mode = PowerTunnelService.getStatus().getMode();
        if(mode == null) {
            Log.w(LOG_TAG, "Attempted to stop service when it is not running");
            return;
        }
        PowerTunnelService.disconnect(context, mode.getServiceClass());
    }

    public static TunnelMode getTunnelMode(Context context) {
        return TunnelMode.valueOf(PreferenceManager.getDefaultSharedPreferences(context)
                .getString("mode", TunnelMode.VPN.name()).toUpperCase()
        );
    }

    public static ProxyAddress getAddress(SharedPreferences prefs) {
        return new ProxyAddress(
                prefs.getString("proxy_ip", "127.0.0.1"),
                Integer.parseInt(prefs.getString("proxy_port", "8085"))
        );
    }


    public static void connect(Context context, Class<? extends Service> service) {
        final Intent intent = getServiceIntent(context, service).setAction(ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public static void disconnect(Context context, Class<? extends Service> service) {
        if(service == TunnelingVpnService.class) {
            context.startService(getServiceIntent(context, service).setAction(ACTION_STOP));
        } else {
            context.stopService(getServiceIntent(context, service));
        }
    }

    private static Intent getServiceIntent(Context context, Class<? extends Service> service) {
        return new Intent(context, service);
    }

    public static GlobalStatus getStatus() {
        return STATUS;
    }
    public static boolean isRunning() {
        return STATUS == GlobalStatus.PROXY || STATUS == GlobalStatus.VPN;
    }
}
