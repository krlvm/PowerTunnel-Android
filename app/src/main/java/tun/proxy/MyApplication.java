package tun.proxy;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import tun.utils.CertificateUtil;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MyApplication extends Application {
    private static MyApplication instance;

    public static MyApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

//    public byte [] getTrustCA() {
//        try {
//            X509Certificate cert = CertificateUtil.getCACertificate("/sdcard/", "");
//            return cert.getEncoded();
//        } catch (CertificateEncodingException e) {
//            e.printStackTrace();
//        }
////        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
////        String trustca_preference = sharedPreferences.getString( "trusted_ca", null );
////        if (trustca_preference != null)  {
////            return CertificateUtil.decode(trustca_preference);
////        }
//        return null;
//    }

    public enum VPNMode {DISALLOW, ALLOW};

    public enum AppSortBy {APPNAME, PKGNAME};

    private final String pref_key[] = {"vpn_disallowed_application", "vpn_allowed_application"};

    public VPNMode loadVPNMode() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String vpn_mode = sharedPreferences.getString("vpn_connection_mode", VPNMode.DISALLOW.name());
        return VPNMode.valueOf(vpn_mode);
    }

    public void storeVPNMode(VPNMode mode) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString("vpn_connection_mode", mode.name());
        return;
    }

    public Set<String> loadVPNApplication(VPNMode mode) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Set<String> preference = prefs.getStringSet(pref_key[mode.ordinal()], new HashSet<String>());
        return preference;
    }

    public void storeVPNApplication(VPNMode mode, final Set<String> set) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(pref_key[mode.ordinal()], set);
        editor.commit();
        return;
    }

}
