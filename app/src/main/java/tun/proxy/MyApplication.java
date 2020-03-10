package tun.proxy;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.HashSet;
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

    public enum VPNMode {
        DISALLOW,
        ALLOW
    }

    public enum AppSortBy {
        APPNAME,
        PKGNAME
    }

    private final String[] pref_key = {"vpn_disallowed_application", "vpn_allowed_application"};

    public VPNMode loadVPNMode() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String vpn_mode = sharedPreferences.getString("vpn_connection_mode", VPNMode.DISALLOW.name());
        return VPNMode.valueOf(vpn_mode);
    }

    public void storeVPNMode(VPNMode mode) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putString("vpn_connection_mode", mode.name());
        editor.apply();
    }

    public Set<String> loadVPNApplication(VPNMode mode) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return prefs.getStringSet(pref_key[mode.ordinal()], new HashSet<String>());
    }

    public void storeVPNApplication(VPNMode mode, Set<String> set) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        final SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(pref_key[mode.ordinal()], set);
        editor.apply();
    }

}
