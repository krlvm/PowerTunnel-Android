package tun.proxy.service;

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
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import ru.krlvm.powertunnel.PowerTunnel;
import ru.krlvm.powertunnel.android.R;
import tun.proxy.MyApplication;
import tun.utils.Util;

public class Tun2HttpVpnService extends VpnService {
    public static final String PREF_RUNNING = "pref_running";
    private static final String TAG = "Tun2Http.Service";
    private static final String ACTION_START = "start";
    private static final String ACTION_STOP = "stop";
    private static volatile PowerManager.WakeLock wlInstance = null;

    static {
        System.loadLibrary("tun2http");
    }

    private ParcelFileDescriptor vpn = null;

    synchronized private static PowerManager.WakeLock getLock(Context context) {
        if (wlInstance == null) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wlInstance = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, context.getString(R.string.app_name) + " wakelock");
            wlInstance.setReferenceCounted(true);
        }
        return wlInstance;
    }

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

    private native void jni_init();

    private native void jni_start(int tun, boolean fwd53, int rcode, String proxyIp, int proxyPort, boolean doh);

    private native void jni_stop(int tun);

    private native int jni_get_mtu();

    private native void jni_done();

    @Override
    public IBinder onBind(Intent intent) {
        return new ServiceBinder();
    }

    public boolean isRunning() {
        return vpn != null;
    }

    private void start() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        PowerTunnel.USE_DNS_SEC = prefs.getBoolean("use_dns_sec", false);
        PowerTunnel.FULL_CHUNKING = prefs.getBoolean("full_chunking", false);
        PowerTunnel.DEFAULT_CHUNK_SIZE = Integer.parseInt(prefs.getString("chunk_size", "2"));
        if(PowerTunnel.DEFAULT_CHUNK_SIZE < 1) {
            PowerTunnel.DEFAULT_CHUNK_SIZE = 2;
        }
        PowerTunnel.MIX_HOST_CASE = prefs.getBoolean("mix_host_case", false);
        PowerTunnel.PAYLOAD_LENGTH = prefs.getBoolean("send_payload", false) ? 21 : 0;
        Log.d("Tun2Boot", "Waiting for native start...");
        if(!PowerTunnel.isRunning()) {
            try {
                PowerTunnel.bootstrap();
            } catch (Exception ex) {
                throw new IllegalStateException(getString((R.string.startup_failed_proxy)));
            }
        }
        if (vpn == null) {
            vpn = startVPN(getBuilder());
            if (vpn == null) {
                throw new IllegalStateException(getString((R.string.startup_failed_vpn)));
            }
            startNative(vpn);
        }
    }

    private void stop() {
        if(PowerTunnel.isRunning()) {
            PowerTunnel.stop();
        }
        if (vpn != null) {
            stopNative(vpn);
            stopVPN(vpn);
            vpn = null;
        }
        stopForeground(true);
    }

    @Override
    public void onRevoke() {
        Log.i(TAG, "Revoke");

        stop();
        vpn = null;

        super.onRevoke();
    }

    private ParcelFileDescriptor startVPN(Builder builder) throws SecurityException {
        try {
            return builder.establish();
        } catch (SecurityException ex) {
            throw ex;
        } catch (Throwable ex) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
            return null;
        }
    }

    private Builder getBuilder() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Builder builder = new Builder();
        builder.setSession(getString(R.string.app_name));

        /* NATIVE VPN CONFIGURATION */
        builder.addAddress("10.1.10.1", 32);
        builder.addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128);

        builder.addRoute("0.0.0.0", 0);
        builder.addRoute("0:0:0:0:0:0:0:0", 0);
        /* ----------------- */

        List<String> dnsServers = Util.getDefaultDNS(this);
        PowerTunnel.DOH_ADDRESS = null;
        if(prefs.getBoolean("override_dns", false) || dnsServers.isEmpty()) {
            String provider = prefs.getString("dns_provider", "CLOUDFLARE");
            if(!provider.contains("_DOH")) {
                dnsServers.clear();
                switch (provider) {
                    default:
                    case "CLOUDFLARE": {
                        dnsServers.add("1.1.1.1");
                        dnsServers.add("1.0.0.1");
                        break;
                    }
                    case "GOOGLE": {
                        dnsServers.add("8.8.8.8");
                        dnsServers.add("8.8.4.4");
                        break;
                    }
                }
            } else {
                dnsServers.clear();
                switch (provider.replace("_DOH", "")) {
                    default:
                    case "CLOUDFLARE": {
                        PowerTunnel.DOH_ADDRESS = "https://cloudflare-dns.com/dns-query";
                        break;
                    }
                    case "GOOGLE": {
                        PowerTunnel.DOH_ADDRESS = "https://dns.google/dns-query";
                        break;
                    }
                }
            }
        }
        for (String dns : dnsServers) {
            builder.addDnsServer(dns);
        }

        // MTU
        int mtu = jni_get_mtu();
        Log.i(TAG, "MTU=" + mtu);
        builder.setMtu(mtu);

        // Add list of allowed applications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            MyApplication app = MyApplication.getInstance();
            if (app.loadVPNMode() == MyApplication.VPNMode.DISALLOW) {
                Set<String> disallow = app.loadVPNApplication(MyApplication.VPNMode.DISALLOW);
                boolean disallowChanged = false;
                for (String pkg : disallow) {
                    try {
                        builder.addDisallowedApplication(pkg);
                    } catch (PackageManager.NameNotFoundException ignore) {
                        //this package is deleted by user
                        disallow.remove(pkg);
                        disallowChanged = true;
                    }
                }
                if(disallowChanged) {
                    MyApplication.getInstance().storeVPNApplication(MyApplication.VPNMode.DISALLOW, disallow);
                    //these deleted packages are now removed from the registry
                }
                try {
                    builder.addDisallowedApplication(getPackageName());
                } catch (PackageManager.NameNotFoundException ex) {
                    Log.d(TAG, "Unexpected error: PowerTunnel package cannot be found");
                }
            } else {
                Set<String> allow = app.loadVPNApplication(MyApplication.VPNMode.ALLOW);
                boolean allowChanged = false;
                for (String pkg : allow) {
                    if (pkg.equals(getPackageName())) {
                        continue;
                    }
                    try {
                        builder.addAllowedApplication(pkg);
                    } catch (PackageManager.NameNotFoundException ignore) {
                        //this package is deleted by user
                        allow.remove(pkg);
                        allowChanged = true;
                    }
                }
                if(allowChanged) {
                    MyApplication.getInstance().storeVPNApplication(MyApplication.VPNMode.ALLOW, allow);
                    //these deleted packages are now removed from the registry
                }
            }
        }
        return builder;
    }

    private void startNative(ParcelFileDescriptor vpn) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String proxyHost = PowerTunnel.SERVER_IP_ADDRESS;
        int proxyPort = PowerTunnel.SERVER_PORT;
        if (proxyPort != 0 && !TextUtils.isEmpty(proxyHost)) {
            jni_start(vpn.getFd(), false, 3, proxyHost, proxyPort, PowerTunnel.DOH_ADDRESS != null);

            prefs.edit().putBoolean(PREF_RUNNING, true).apply();
        }
    }

    private void stopNative(ParcelFileDescriptor vpn) {
        try {
            jni_stop(vpn.getFd());

        } catch (Throwable ex) {
            // File descriptor might be closed
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
            jni_stop(-1);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean(PREF_RUNNING, false).apply();
    }

    private void stopVPN(ParcelFileDescriptor pfd) {
        Log.i(TAG, "Stopping");
        try {
            pfd.close();
        } catch (IOException ex) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
        }
    }

    // Called from native code
    private void nativeExit(String reason) {
        Log.w(TAG, "Native exit reason=" + reason);
        if (reason != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefs.edit().putBoolean("enabled", false).apply();
        }
    }

    // Called from native code
    private void nativeError(int error, String message) {
        Log.w(TAG, "Native error " + error + ": " + message);
    }

    private boolean isSupported(int protocol) {
        return (protocol == 1 /* ICMPv4 */ ||
                protocol == 59 /* ICMPv6 */ ||
                protocol == 6 /* TCP */ ||
                protocol == 17 /* UDP */);
    }

    @Override
    public void onCreate() {
        jni_init();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Received " + intent);
        // Handle service restart
        if (intent == null) {
            return START_STICKY;
        }

        if (ACTION_START.equals(intent.getAction())) {
            start();
        }
        if (ACTION_STOP.equals(intent.getAction())) {
            stop();
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Destroy");
        try {
            if (vpn != null) {
                stopNative(vpn);
                stopVPN(vpn);
                vpn = null;
            }
        } catch (Throwable ex) {
            Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
        }
        jni_done();
        super.onDestroy();
    }

    public class ServiceBinder extends Binder {
        @Override
        public boolean onTransact(int code, @androidx.annotation.NonNull Parcel data, Parcel reply, int flags)
                throws RemoteException {
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
}
