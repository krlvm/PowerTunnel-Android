package tun.proxy.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.Set;

import ru.krlvm.powertunnel.PowerTunnel;
import ru.krlvm.powertunnel.android.MainActivity;
import ru.krlvm.powertunnel.android.R;
import ru.krlvm.powertunnel.android.managers.NotificationHelper;
import ru.krlvm.powertunnel.android.managers.PTManager;

public class Tun2HttpVpnService extends VpnService {

    private static final String TAG = "Tun2Http.Service";
    private static final int FOREGROUND_ID = 2;
    private static final String CHANNEL_ID = "PowerTunnelVPN";

    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";

    private ParcelFileDescriptor vpn = null;

    public static void start(Context context) {
        Intent intent = new Intent(context, Tun2HttpVpnService.class);
        intent.setAction(ACTION_START);
        context.startService(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, Tun2HttpVpnService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }


    @Override
    public void onCreate() {
        NotificationHelper.prepareNotificationChannel(this, CHANNEL_ID);
        jni_init();
        super.onCreate();
    }

    @Override
    public void onRevoke() {
        Log.d(TAG, "Revoke");
        stop();
        vpn = null;
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Destroy");
        if (vpn != null) {
            try {
                stopNative(vpn);
                stopVPN(vpn);
                vpn = null;
            } catch (Throwable ex) {
                Log.e(TAG, "Destroy / Failed to stop VPN: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
        jni_done();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_START: {
                    start();
                    break;
                }
                case ACTION_STOP: {
                    stop();
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Unknown action: " + intent.getAction());
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder();
    }

    public boolean isRunning() {
        return vpn != null;
    }

    private void start() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        PTManager.configure(this, prefs);

        Exception proxyFailure = PTManager.safeStartProxy(this);
        if(proxyFailure != null) {
            PTManager.serverStartupFailureBroadcast(this, proxyFailure);
            stop();
            return;
        }

        Log.i(TAG, "Waiting for VPN server to start...");
        if (vpn == null) {
            String error = null;
            boolean firmwareBug = false;
            try {
                vpn = getBuilder().establish();
            } catch (Throwable ex) {
                error = ex.getMessage();
                if(ex instanceof SecurityException) {
                    // Samsung broke VPN API on multi-user configuration with Android 10 August 2020 patch for some devices
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && (Build.MODEL != null && Build.MODEL.toLowerCase().startsWith("sm-"))) {
                        firmwareBug = true;
                    }
                }
                Log.e(TAG, "Failed to establish VPN: " + ex.getMessage() + "\n" + Log.getStackTraceString(ex));
            }
            if (vpn == null) {
                if(firmwareBug) {
                    sendBroadcast(new Intent(MainActivity.SAMSUNG_FIRMWARE_ERROR_BROADCAST));
                } else {
                    String cause = getString(R.string.startup_failed_vpn, error);
                    PTManager.serverStartupFailureBroadcast(this, cause);
                }
                stop();
                return;
            }
            startNative(vpn);
        }

        Notification notification = NotificationHelper.createMainActivityNotification(this,
                CHANNEL_ID, getString(R.string.app_name),
                getString(R.string.notification_vpn_background)
        );

        startForeground(FOREGROUND_ID, notification);
    }

    private void stop() {
        PTManager.stopProxy(this);
        if (vpn != null) {
            stopNative(vpn);
            stopVPN(vpn);
            vpn = null;
        }
        stopForeground(true);
    }


    private Builder getBuilder() {
        Builder builder = new Builder();
        builder.setSession(getString(R.string.app_name));
        builder.setConfigureIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0));

        /* NATIVE VPN CONFIGURATION */
        builder.addAddress("10.1.10.1", 32);
        builder.addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128);

        builder.addRoute("0.0.0.0", 0);
        builder.addRoute("0:0:0:0:0:0:0:0", 0);

        int mtu = jni_get_mtu();
        Log.i(TAG, "MTU=" + mtu);
        builder.setMtu(mtu);
        /* ----------------- */

        for (String dns : PTManager.DNS_SERVERS) {
            try {
                builder.addDnsServer(dns);
            } catch (IllegalArgumentException ignore) {}
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            PTManager.VPNMode vpnMode = PTManager.getVPNConnectionMode(prefs);
            Set<String> apps = PTManager.getVPNApplications(prefs, vpnMode);
            boolean appsModified = false;

            if (vpnMode == PTManager.VPNMode.DISALLOW) {
                for (String pkg : apps) {
                    try {
                        builder.addDisallowedApplication(pkg);
                    } catch (PackageManager.NameNotFoundException ignore) {
                        //this package is deleted by user
                        apps.remove(pkg);
                        appsModified = true;
                    }
                }
                try {
                    builder.addDisallowedApplication(getPackageName());
                } catch (PackageManager.NameNotFoundException ex) {
                    Log.d(TAG, "Unexpected error: PowerTunnel package cannot be found");
                }
            } else {
                for (String pkg : apps) {
                    if (pkg.equals(getPackageName())) {
                        continue;
                    }
                    try {
                        builder.addAllowedApplication(pkg);
                    } catch (PackageManager.NameNotFoundException ignore) {
                        //this package is deleted by user
                        apps.remove(pkg);
                        appsModified = true;
                    }
                }
            }

            if (appsModified) {
                PTManager.storeVPNApplications(prefs, vpnMode, apps);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // inherit the underlying network metered setting
            builder.setMetered(false);
        }

        return builder;
    }

    private void startNative(ParcelFileDescriptor vpn) {
        String proxyHost = PowerTunnel.SERVER_IP_ADDRESS;
        int proxyPort = PowerTunnel.SERVER_PORT;
        if (proxyPort != 0 && !TextUtils.isEmpty(proxyHost)) {
            jni_start(vpn.getFd(), false, 3, proxyHost, proxyPort, PowerTunnel.DOH_ADDRESS != null);
            PTManager.setRunningPref(this, true);
        }
    }

    private void stopNative(ParcelFileDescriptor vpn) {
        Log.d(TAG, "Stop native");
        try {
            jni_stop(vpn.getFd());
        } catch (Throwable ex) {
            // File descriptor might be closed
            Log.e(TAG, "Stop native / Error: " + ex.getMessage());
            ex.printStackTrace();
            jni_stop(-1);
        }
        PTManager.setRunningPref(this, false);
    }

    private void stopVPN(ParcelFileDescriptor pfd) {
        Log.d(TAG, "Stop");
        try {
            pfd.close();
        } catch (IOException ex) {
            Log.e(TAG, "Destroy / Failed to close PFD: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public class ServiceBinder extends Binder {
        @Override
        public boolean onTransact(int code, @androidx.annotation.NonNull Parcel data, Parcel reply, int flags) throws RemoteException {
            if (code == IBinder.LAST_CALL_TRANSACTION) {
                onRevoke();
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }

        public Tun2HttpVpnService getService() {
            return Tun2HttpVpnService.this;
        }
    }


    /* Native callbacks */
    private void nativeExit(String reason) {
        Log.w(TAG, "Native exit reason=" + reason);
        if (reason != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putBoolean("enabled", false).apply();
        }
    }
    private void nativeError(int error, String message) {
        Log.w(TAG, "Native error " + error + ": " + message);
    }
    private boolean isSupported(int protocol) {
        return (protocol == 1 /* ICMPv4 */ ||
                protocol == 59 /* ICMPv6 */ ||
                protocol == 6 /* TCP */ ||
                protocol == 17 /* UDP */);
    }

    /* Native methods */
    private native void jni_init();
    private native void jni_start(int tun, boolean fwd53, int rcode, String proxyIp, int proxyPort, boolean doh);
    private native void jni_stop(int tun);
    private native int jni_get_mtu();
    private native void jni_done();

    static {
        System.loadLibrary("tun2http");
    }
}
