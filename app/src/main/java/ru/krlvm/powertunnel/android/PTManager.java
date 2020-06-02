package ru.krlvm.powertunnel.android;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

import ru.krlvm.powertunnel.PowerTunnel;
import ru.krlvm.powertunnel.android.exceptions.ProxyStartFailureException;
import tun.utils.Util;

public class PTManager {

    public static List<String> DNS_SERVERS = new ArrayList<>();
    public static boolean DNS_OVERRIDE = false;

    public static void configure(Context context, SharedPreferences prefs) {
        PowerTunnel.USE_DNS_SEC = prefs.getBoolean("use_dns_sec", false);
        PowerTunnel.ALLOW_REQUESTS_TO_ORIGIN_SERVER = prefs.getBoolean("allow_req_to_oserv", true);
        PowerTunnel.FULL_CHUNKING = prefs.getBoolean("full_chunking", false);
        PowerTunnel.DEFAULT_CHUNK_SIZE = Integer.parseInt(prefs.getString("chunk_size", "2"));
        if (PowerTunnel.DEFAULT_CHUNK_SIZE < 1) {
            PowerTunnel.DEFAULT_CHUNK_SIZE = 2;
        }
        PowerTunnel.MIX_HOST_CASE = prefs.getBoolean("mix_host_case", false);
        PowerTunnel.PAYLOAD_LENGTH = prefs.getBoolean("send_payload", false) ? 21 : 0;
        DNS_SERVERS = Util.getDefaultDNS(context);
        DNS_OVERRIDE = false;
        PowerTunnel.DOH_ADDRESS = null;
        if (prefs.getBoolean("override_dns", false) || DNS_SERVERS.isEmpty()) {
            String provider = prefs.getString("dns_provider", "CLOUDFLARE");
            DNS_OVERRIDE = true;
            if(provider.equals("SPECIFIED")) {
                String specifiedDnsProvider = prefs.getString("specified_dns_provider", "");
                if(!specifiedDnsProvider.trim().isEmpty()) {
                    if(specifiedDnsProvider.startsWith("https://")) {
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
                    case "CLOUDFLARE": {
                        PowerTunnel.DOH_ADDRESS = "https://cloudflare-dns.com/dns-query";
                        break;
                    }
                    case "GOOGLE": {
                        PowerTunnel.DOH_ADDRESS = "https://dns.google/dns-query";
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
        try {
            PowerTunnel.DNS_PORT = Integer.parseInt(dnsPortVal);
        } catch (NumberFormatException ex) {}
    }

    public static Exception safeStartProxy() {
        try {
            startProxy();
            return null;
        } catch (ProxyStartFailureException ex) {
            return ex;
        }
    }

    public static void startProxy() throws ProxyStartFailureException {
        if (!PowerTunnel.isRunning()) {
            try {
                PowerTunnel.bootstrap();
            } catch (Exception ex) {
                throw new ProxyStartFailureException(ex.getMessage(), ex);
            }
        }
    }

    public static void stopProxy() {
        if (PowerTunnel.isRunning()) {
            PowerTunnel.stop();
        }
    }
}
