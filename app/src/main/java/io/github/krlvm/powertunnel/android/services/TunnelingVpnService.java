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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import io.github.krlvm.powertunnel.android.R;
import io.github.krlvm.powertunnel.android.activities.MainActivity;
import io.github.krlvm.powertunnel.android.managers.ProxyManager;
import io.github.krlvm.powertunnel.android.receivers.BootReceiver;
import io.github.krlvm.powertunnel.android.types.GlobalStatus;
import io.github.krlvm.powertunnel.android.types.TunnelMode;
import io.github.krlvm.powertunnel.android.utility.NotificationHelper;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyAddress;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyStatus;

/**
 * VPN Interceptor is powered by NetGuard
 * NetGuard is licensed under the GNU GPLv3
 * Copyright 2015-2017 by Marcel Bokhorst (M66B)
 */
public class TunnelingVpnService extends VpnService {

    private static final String LOG_TAG = "TunnelingVpnService";

    private static final String NOTIFICATION_CHANNEL_ID = TunnelMode.VPN.name();
    private static final int FOREGROUND_ID = TunnelMode.VPN.id();

    private ProxyManager proxy;
    private ParcelFileDescriptor vpn = null;
    private Intent disconnectBroadcast = null;

    private static final boolean vpnResolveHosts = true;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.registerChannel(
                this,
                NOTIFICATION_CHANNEL_ID, R.string.notification_channel_vpn,
                R.string.notification_channel_vpn_description,
                NotificationHelper.createConnectionGroup(this)
        );
        jni_init();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
        disconnect();
        jni_done();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && PowerTunnelService.ACTION_STOP.equals(intent.getAction())) {
            disconnect();
            return START_NOT_STICKY;
        } else {
            proxy = new ProxyManager(
                    this,
                    (status) -> {
                        if (status == ProxyStatus.RUNNING) {
                            connect();
                        } else if (status == ProxyStatus.NOT_RUNNING) {
                            disconnect();
                        }
                    },
                    (error) -> {
                        Log.w(LOG_TAG, "Proxy Server could not start, stopping...");
                        disconnect(new Intent(PowerTunnelService.BROADCAST_FAILURE)
                                .putExtra(PowerTunnelService.EXTRAS_MODE, TunnelMode.PROXY)
                                .putExtra(PowerTunnelService.EXTRAS_ERROR, error)
                        );
                    },
                    getDefaultDNS(this)
            );
            //vpnResolveHosts = PreferenceManager.getDefaultSharedPreferences(this)
            //        .getBoolean("vpn_resolve_hosts", false);
            //proxy.setHostnamesAvailability(!vpnResolveHosts);
            proxy.start();
            return START_STICKY;
        }
    }

    @Override
    public void onRevoke() {
        super.onRevoke();
        disconnect();
    }

    private final IBinder mBinder = new Binder() {
        @Override
        public boolean onTransact(int code, @NonNull Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == IBinder.LAST_CALL_TRANSACTION) {
                onRevoke();
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }
    };
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void connect() {
        if(vpn != null) return;
        Log.i(LOG_TAG, "Establishing VPN Service...");

        String error = null;
        boolean isFirmwareBug = false;

        try {
            vpn = getBuilder().establish();
        } catch (Throwable t) {
            Log.e(LOG_TAG, "Failed to establish VPN Service: " + t.getMessage(), t);
            error = t.getMessage();
            if(t instanceof SecurityException &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    (Build.MODEL != null && Build.MODEL.toLowerCase().startsWith("sm-"))
            ) {
                Log.e(LOG_TAG, "Most likely VPN Service establishing failure was caused by firmware bug");
                isFirmwareBug = true;
            }
        }

        if(vpn == null) {
            Log.w(LOG_TAG, "VPN Service is still not established, stopping...");
            disconnect(new Intent(PowerTunnelService.BROADCAST_FAILURE)
                    .putExtra(PowerTunnelService.EXTRAS_MODE, TunnelMode.VPN)
                    .putExtra(PowerTunnelService.EXTRAS_ERROR, error)
                    .putExtra(PowerTunnelService.EXTRAS_ERROR_FW, isFirmwareBug)
            );
            return;
        }

        final ProxyAddress address = proxy.getAddress();
        String host = address.getHost();
        if ("0.0.0.0".equals(host)) {
            host = "127.0.0.1";
        }

        Log.i(LOG_TAG, "Starting native VPN Interceptor...");
        jni_start(vpn.getFd(), false, 3, host, address.getPort(), vpnResolveHosts);
        Log.i(LOG_TAG, "Native VPN Interceptor has been started");

        startForeground(FOREGROUND_ID, NotificationHelper.createConnectionNotification(
                this, NOTIFICATION_CHANNEL_ID,
                R.string.notification_title_vpn,
                R.string.notification_message_vpn,
                R.string.action_disconnect
        ));

        PowerTunnelService.STATUS = GlobalStatus.VPN;
        sendBroadcast(new Intent(PowerTunnelService.BROADCAST_STARTED));
        BootReceiver.rememberState(this, true);
    }

    private void disconnect() {
        disconnect(new Intent(PowerTunnelService.BROADCAST_STOPPED));
    }

    private void disconnect(Intent broadcast) {
        if (disconnectBroadcast == null) {
            disconnectBroadcast = broadcast;
        }
        if(proxy != null) {
            if(proxy.isRunning()) {
                proxy.stop();
            }
            proxy = null;
            return;
        }
        // We need to wait until the proxy has stopped
        if (vpn != null) {
            try {
                jni_stop(vpn.getFd());
            } catch (Throwable ex) {
                Log.e(LOG_TAG, "Failed to stop native VPN Interceptor: " + ex.getMessage(), ex);
                jni_stop(-1);
            }
            try {
                vpn.close();
                Log.d(LOG_TAG, "Closed VPN PFD");
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Failed to stop VPN PFD: " + ex.getMessage(), ex);
            }
            vpn = null;
        }

        PowerTunnelService.STATUS = GlobalStatus.NOT_RUNNING;
        stopForeground(true);

        sendBroadcast(disconnectBroadcast);
        disconnectBroadcast = null;
        BootReceiver.rememberState(this, false);
    }

    private Builder getBuilder() {
        final Builder builder = new Builder();
        builder.setSession(getString(R.string.app_name));
        builder.setConfigureIntent(PendingIntent.getActivity(
                this, 0,
                new Intent(this, MainActivity.class), 0
        ));

        /* NATIVE VPN CONFIGURATION */
        builder.addAddress("10.1.10.1", 32);
        builder.addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128);

        builder.addRoute("0.0.0.0", 0);
        builder.addRoute("0:0:0:0:0:0:0:0", 0);

        builder.setMtu(jni_get_mtu());
        /* ------------------------ */

        final List<String> defaultDns = getDefaultDNS(this);
        if(!defaultDns.isEmpty()) {
            for (String dns : defaultDns) {
                builder.addDnsServer(dns);
            }
        } else {
            builder.addDnsServer("8.8.8.8");
            builder.addDnsServer("8.8.4.4");
            builder.addDnsServer("1.1.1.1");
            builder.addDnsServer("1.0.0.1");
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            if("exclude".equals(prefs.getString("vpn_apps_mode", "exclude"))) {
                for (String pkg : prefs.getStringSet("vpn_apps_excluded", new HashSet<>())) {
                    try {
                        builder.addDisallowedApplication(pkg);
                    } catch (PackageManager.NameNotFoundException ignore) {}
                }
                try {
                    builder.addDisallowedApplication(getPackageName());
                } catch (PackageManager.NameNotFoundException ex) {
                    Log.e(LOG_TAG, "Unexpected error: can't disallow own package: " + ex.getMessage(), ex);
                    throw new RuntimeException("Can't exclude own package");
                }
            } else {
                for (String pkg : prefs.getStringSet("vpn_apps_allowed", new HashSet<>())) {
                    if(pkg.equals(getPackageName())) continue;
                    try {
                        builder.addAllowedApplication(pkg);
                    } catch (PackageManager.NameNotFoundException ignore) {}
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false);
        }

        return builder;
    }


    static List<String> getDefaultDNS(Context context) {
        final List<String> servers = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final Network network = cm.getActiveNetwork();
            if (network != null) {
                final LinkProperties properties = cm.getLinkProperties(network);
                if (properties != null) {
                    final List<InetAddress> addresses = properties.getDnsServers();
                    for (InetAddress address : addresses) {
                        final String host = address.getHostAddress();
                        if (TextUtils.isEmpty(host)) continue;
                        servers.add(host);
                    }
                }
            }
        } else {
            for(int i = 1; i <= 4; i++) {
                final String dns = jni_getprop("net.dns" + i);
                if(!TextUtils.isEmpty(dns)) servers.add(dns);
            }
        }

        return servers;
    }

    /* Native methods */
    private native void jni_init();
    private native void jni_start(int tun, boolean fwd53, int rcode, String proxyIp, int proxyPort, boolean resolveHosts);
    private native void jni_stop(int tun);
    private native void jni_done();
    private native int jni_get_mtu();
    private static native String jni_getprop(String name);
    /* -------------- */

    static {
        System.loadLibrary("tun2http");
    }
}
