package ru.krlvm.powertunnel.android.managers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Handler;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import ru.krlvm.powertunnel.PowerTunnel;
import ru.krlvm.powertunnel.android.MainActivity;
import ru.krlvm.powertunnel.android.R;
import ru.krlvm.powertunnel.android.receiver.BootReceiver;
import ru.krlvm.powertunnel.android.services.ProxyModeService;
import ru.krlvm.powertunnel.enums.SNITrick;
import tun.proxy.service.Tun2HttpVpnService;
import tun.utils.Util;

public class PTManager {

    public static List<String> DNS_SERVERS = new ArrayList<>();
    public static boolean DNS_OVERRIDE = false;

    public static void configure(Context context, SharedPreferences prefs) {
        String sniTrick = prefs.getString("sni_trick", "DISABLED");
        if(!sniTrick.equals("DISABLED")) {
            PowerTunnel.SNI_TRICK = SNITrick.valueOf(sniTrick);
            if(PowerTunnel.SNI_TRICK == SNITrick.FAKE) {
                PowerTunnel.SNI_TRICK_FAKE_HOST = prefs.getString("sni_fake_host", "example.com");
            }
            if(!prefs.contains("mitm_password")) {
                String newPassword = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("mitm_password", newPassword);
                editor.apply();
                PowerTunnel.MITM_PASSWORD = newPassword.toCharArray();
            } else {
                PowerTunnel.MITM_PASSWORD = prefs.getString("mitm_password", UUID.randomUUID().toString()).toCharArray();
            }
        } else {
            PowerTunnel.SNI_TRICK = null;
            PowerTunnel.SNI_TRICK_FAKE_HOST = null;
            PowerTunnel.MITM_PASSWORD = null;
        }
        PowerTunnel.DOT_AFTER_HOST_HEADER = prefs.getBoolean("dot_after_host", true);
        PowerTunnel.MIX_HOST_HEADER_CASE = prefs.getBoolean("mix_host_header_case", true);
        PowerTunnel.MIX_HOST_CASE = prefs.getBoolean("mix_host_case", false);
        PowerTunnel.COMPLETE_MIX_HOST_CASE = prefs.getBoolean("complete_mix_host_case", false);
        PowerTunnel.LINE_BREAK_BEFORE_GET = prefs.getBoolean("break_before_get", false);
        PowerTunnel.ADDITIONAL_SPACE_AFTER_GET = prefs.getBoolean("space_after_get", false);
        PowerTunnel.APPLY_HTTP_TRICKS_TO_HTTPS = prefs.getBoolean("apply_http_https", false);
        PowerTunnel.USE_DNS_SEC = prefs.getBoolean("use_dns_sec", false);
        PowerTunnel.ALLOW_REQUESTS_TO_ORIGIN_SERVER = prefs.getBoolean("allow_req_to_oserv", true);
        PowerTunnel.CHUNKING_ENABLED = prefs.getBoolean("chunking", true);
        PowerTunnel.FULL_CHUNKING = prefs.getBoolean("full_chunking", false);
        PowerTunnel.DEFAULT_CHUNK_SIZE = Integer.parseInt(prefs.getString("chunk_size", "2"));
        if (PowerTunnel.DEFAULT_CHUNK_SIZE < 1) {
            PowerTunnel.DEFAULT_CHUNK_SIZE = 2;
        }
        PowerTunnel.PAYLOAD_LENGTH = prefs.getBoolean("send_payload", false) ? 21 : 0;
        if(prefs.getBoolean("upstream_proxy", false)) {
            PowerTunnel.UPSTREAM_PROXY_CACHE = prefs.getBoolean("upstream_cache", true);
            PowerTunnel.UPSTREAM_PROXY_IP = prefs.getString("upstream_ip", null);
            PowerTunnel.UPSTREAM_PROXY_PORT = Integer.parseInt(prefs.getString("upstream_port", "-1"));
            PowerTunnel.UPSTREAM_PROXY_USERNAME = prefs.getString("upstream_username", null);
            PowerTunnel.UPSTREAM_PROXY_PASSWORD = prefs.getString("upstream_password", null);
        } else {
            PowerTunnel.UPSTREAM_PROXY_IP = null;
            PowerTunnel.UPSTREAM_PROXY_USERNAME = null;
            PowerTunnel.UPSTREAM_PROXY_AUTH_CODE = null;
        }
        DNS_SERVERS = Util.getDefaultDNS(context);
        DNS_OVERRIDE = false;
        PowerTunnel.DOH_ADDRESS = null;
        if (prefs.getBoolean("override_dns", false) || DNS_SERVERS.isEmpty()) {
            String provider = prefs.getString("dns_provider", "GOOGLE_DOH");
            DNS_OVERRIDE = true;
            if(provider.equals("SPECIFIED")) {
                String specifiedDnsProvider = prefs.getString("specified_dns_provider", "");
                if(!specifiedDnsProvider.trim().isEmpty()) {
                    if(specifiedDnsProvider.startsWith("https://") ||
                            (specifiedDnsProvider.startsWith("http://") && prefs.getBoolean("allow_insecure_doh", false))) {
                        PowerTunnel.DOH_ADDRESS = specifiedDnsProvider;
                    } else {
                        DNS_SERVERS.clear();
                        DNS_SERVERS.add(specifiedDnsProvider);
                    }
                }
            }
            if (!provider.contains("_DOH")) {
                DNS_SERVERS.clear();
                switch (provider) {
                    default:
                    case "CLOUDFLARE": {
                        DNS_SERVERS.add("1.1.1.1");
                        DNS_SERVERS.add("1.0.0.1");
                        break;
                    }
                    case "GOOGLE": {
                        DNS_SERVERS.add("8.8.8.8");
                        DNS_SERVERS.add("8.8.4.4");
                        break;
                    }
                    case "ADGUARD": {
                        DNS_SERVERS.add("176.103.130.130");
                        DNS_SERVERS.add("176.103.130.131");
                        break;
                    }
                }
            } else {
                switch (provider.replace("_DOH", "")) {
                    case "GOOGLE": {
                        PowerTunnel.DOH_ADDRESS = "https://dns.google/dns-query";
                        break;
                    }
                    case "CLOUDFLARE": {
                        PowerTunnel.DOH_ADDRESS = "https://cloudflare-dns.com/dns-query";
                        break;
                    }
                    case "ADGUARD": {
                        PowerTunnel.DOH_ADDRESS = "https://dns.adguard.com/dns-query";
                        break;
                    }
                    default: {
                        //Invalid setting, former SECDNS?
                        PowerTunnel.DOH_ADDRESS = null;
                        break;
                    }
                }
            }
        }
        String dnsPortVal = prefs.getString("dns_port", "");
        if(!dnsPortVal.isEmpty()) {
            try {
                PowerTunnel.DNS_PORT = Integer.parseInt(dnsPortVal);
            } catch (NumberFormatException ex) {
            }
        }
    }

    public static void setRunningPref(Context context, boolean isRunning) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(BootReceiver.PREF_RUNNING, isRunning).apply();
    }

    public static boolean safeStartProxy(Context context) {
        try {
            startProxy(context);
            return true;
        } catch (Exception ex) {
            serverStartupFailureBroadcast(context, ex);
            return false;
        }
    }

    public static void startProxy(Context context) throws Exception {
        PowerTunnel.bootstrap();
        setRunningPref(context, true);
        context.sendBroadcast(new Intent(MainActivity.SERVER_START_BROADCAST));
    }

    public static void stopProxy(Context context) {
        if (PowerTunnel.isRunning()) {
            PowerTunnel.stop();
            setRunningPref(context, false);
            context.sendBroadcast(new Intent(MainActivity.SERVER_STOP_BROADCAST));
        }
    }

    public static void serverStartupFailureBroadcast(Context context, Exception cause) {
        cause.printStackTrace();
        serverStartupFailureBroadcast(context, cause.getMessage());
    }

    public static void serverStartupFailureBroadcast(Context context, String cause) {
        Intent intent = new Intent(MainActivity.STARTUP_FAIL_BROADCAST);
        intent.putExtra("cause", cause);
        context.sendBroadcast(intent);
    }


    public static boolean isVPN(Context context) {
        return isVPN(PreferenceManager.getDefaultSharedPreferences(context));
    }
    public static boolean isVPN(SharedPreferences prefs) {
        return !prefs.getBoolean("proxy_mode", false);
    }

    public static void startTunnel(Context context) {
        if(isVPN(context)) {
            VpnService.prepare(context);
            startVpn(context);
        } else {
            startProxyService(context);
        }
    }

    public static void startVpn(Context context) {
        Tun2HttpVpnService.start(context);
    }

    public static void startProxyService(Context context) {
        context.startService(new Intent(context, ProxyModeService.class));
    }

    public static void stopTunnel(Context context) {
        if(isVPN(context)) {
            stopVpn(context);
        } else {
            stopProxyService(context);
        }
    }

    public static void stopVpn(Context context) {
        Tun2HttpVpnService.stop(context);
    }

    public static void stopProxyService(Context context) {
        context.stopService(new Intent(context, ProxyModeService.class));
    }

    public static void stopWithToast(Context context) {
        Toast.makeText(context, R.string.stopping_powertunnel, Toast.LENGTH_LONG).show();
        new Handler().postDelayed(() -> PTManager.stopTunnel(context), 5);
    }


    public static VPNMode getVPNConnectionMode(SharedPreferences prefs) {
        return VPNMode.valueOf(prefs.getString("vpn_connection_mode", VPNMode.DISALLOW.name()));
    }

    public static Set<String> getVPNApplications(SharedPreferences prefs, VPNMode mode) {
        return prefs.getStringSet(mode.getPreference(), new HashSet<>());
    }

    public static void storeVPNMode(SharedPreferences prefs, VPNMode mode) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("vpn_connection_mode", mode.name());
        editor.apply();
    }

    public static void storeVPNApplications(SharedPreferences prefs, VPNMode mode, Set<String> apps) {
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(mode.getPreference(), apps);
        editor.apply();
    }

    public enum VPNMode {

        DISALLOW(R.string.pref_header_disallowed_application_list, "vpn_disallowed_application"),
        ALLOW(R.string.pref_header_allowed_application_list, "vpn_allowed_application");

        private int displayName;
        private String preference;

        VPNMode(int displayName, String preference) {
            this.displayName = displayName;
            this.preference = preference;
        }

        public int getDisplayName() {
            return displayName;
        }

        public String getPreference() {
            return preference;
        }
    }
}
