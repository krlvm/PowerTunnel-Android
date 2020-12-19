package ru.krlvm.powertunnel.android.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

import ru.krlvm.powertunnel.android.MainActivity;
import ru.krlvm.powertunnel.android.R;
import ru.krlvm.powertunnel.android.managers.PTManager;
import ru.krlvm.powertunnel.android.managers.PTManager.VPNMode;
import tun.proxy.preferences.fragments.PackageListPreferenceFragment;
import tun.proxy.preferences.preference.EditTextSummaryPreference;

import static android.preference.Preference.OnPreferenceChangeListener;
import static android.preference.Preference.OnPreferenceClickListener;

public class SettingsFragment extends PreferenceFragment
        implements OnPreferenceClickListener, OnPreferenceChangeListener {

    public static final String VPN_CONNECTION_MODE = "vpn_connection_mode";
    public static final String VPN_DISALLOWED_APPLICATION_LIST = "vpn_disallowed_application_list";
    public static final String VPN_ALLOWED_APPLICATION_LIST = "vpn_allowed_application_list";
    public static final String VPN_CLEAR_ALL_SELECTION = "vpn_clear_all_selection";
    public static final String DNS_PROVIDER = "dns_provider";
    public static final String SPECIFIED_DNS = "specified_dns_provider";
    public static final String RESET_CONNECTION_SETTINGS = "reset_connection_settings";
    public static final String PROXY_IP = "proxy_ip";
    public static final String PROXY_PORT = "proxy_port";

    private ListPreference prefPackage;
    private PreferenceScreen prefDisallow;
    private PreferenceScreen prefAllow;
    private PreferenceScreen clearAllSelection;

    private EditTextSummaryPreference prefSpecDns;

    private SharedPreferences prefs;
    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        setHasOptionsMenu(true);
        updateTitle();

        prefPackage = (ListPreference) this.findPreference(VPN_CONNECTION_MODE);
        prefDisallow = (PreferenceScreen) findPreference(VPN_DISALLOWED_APPLICATION_LIST);
        prefAllow = (PreferenceScreen) findPreference(VPN_ALLOWED_APPLICATION_LIST);

        clearAllSelection = (PreferenceScreen) findPreference(VPN_CLEAR_ALL_SELECTION);

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        prefSpecDns = ((EditTextSummaryPreference) findPreference(SPECIFIED_DNS));

        prefPackage.setOnPreferenceChangeListener(this);
        findPreference(DNS_PROVIDER).setOnPreferenceChangeListener(this);
        prefDisallow.setOnPreferenceClickListener(this);
        prefAllow.setOnPreferenceClickListener(this);
        clearAllSelection.setOnPreferenceClickListener(this);
        findPreference(RESET_CONNECTION_SETTINGS).setOnPreferenceClickListener(this);
        findPreference("theme").setOnPreferenceChangeListener(this);
        findPreference("sni_trick").setOnPreferenceChangeListener(this);

        findPreference("proxy_mode").setOnPreferenceChangeListener(this);
        findPreference("upstream_proxy").setOnPreferenceChangeListener(this);
        findPreference("upstream_auth").setOnPreferenceChangeListener(this);

        updateProxyVpn(!prefs.getBoolean("proxy_mode", false));
        updateUpstreamProxy(prefs.getBoolean("upstream_proxy", false));
        updateUpstreamProxyAuth(prefs.getBoolean("upstream_auth", false));
        updateFakeSniStatus(prefs.getString("sni_trick", "DISABLED"));
        updateSpecDnsStatus(prefs.getString(DNS_PROVIDER, "GOOGLE_DOH"));
        updateVPNModeItem();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity.getApplicationContext();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTitle();
        updateVPNModeItem();
    }

    private void updateTitle() {
        getActivity().setTitle(R.string.title_activity_settings);
    }

    private void updateVPNModeItem() {
        updateVPNModeItem(!prefs.getBoolean("proxy_mode", false));
    }

    private void updateVPNModeItem(boolean vpnMode) {
        if(!vpnMode) {
            return;
        }
        int countDisallow = PTManager.getVPNApplications(prefs, VPNMode.DISALLOW).size();
        prefDisallow.setTitle(getString(R.string.pref_disallowed_application_list) + (countDisallow > 0 ? " (" + countDisallow + ")" : ""));
        prefDisallow.setEnabled(VPNMode.DISALLOW.name().equals(prefPackage.getValue()));

        int countAllow = PTManager.getVPNApplications(prefs, VPNMode.ALLOW).size();
        prefAllow.setTitle(getString(R.string.pref_allowed_application_list) + (countAllow > 0 ? " (" + countAllow + ")" : ""));
        prefAllow.setEnabled(VPNMode.ALLOW.name().equals(prefPackage.getValue()));
    }

    private void updateProxyVpn(boolean vpnMode) {
        prefPackage.setEnabled(vpnMode);
        prefAllow.setEnabled(vpnMode);
        prefDisallow.setEnabled(vpnMode);
        clearAllSelection.setEnabled(vpnMode);
        updateVPNModeItem(vpnMode);
    }

    private void updateUpstreamProxy(boolean enabled) {
        findPreference("upstream_ip").setEnabled(enabled);
        findPreference("upstream_port").setEnabled(enabled);
        findPreference("upstream_cache").setEnabled(enabled);
        findPreference("upstream_auth").setEnabled(enabled);
        updateUpstreamProxyAuth(enabled);
    }

    private void updateUpstreamProxyAuth(boolean enabled) {
        findPreference("upstream_username").setEnabled(enabled);
        findPreference("upstream_password").setEnabled(enabled);
    }

    private void updateFakeSniStatus(String sniTrick) {
        findPreference("sni_fake_host").setEnabled(sniTrick.equals("FAKE"));
    }

    private void updateSpecDnsStatus(String provider) {
        prefSpecDns.setEnabled(provider.equals("SPECIFIED"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            startActivity(new Intent(getActivity(), MainActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case VPN_DISALLOWED_APPLICATION_LIST: {
                Toast.makeText(getActivity(), R.string.status_please_wait, Toast.LENGTH_LONG).show();
                transitionFragment(PackageListPreferenceFragment.create(VPNMode.DISALLOW));
                break;
            }
            case VPN_ALLOWED_APPLICATION_LIST: {
                Toast.makeText(getActivity(), R.string.status_please_wait, Toast.LENGTH_LONG).show();
                transitionFragment(PackageListPreferenceFragment.create(VPNMode.ALLOW));
                break;
            }
            case VPN_CLEAR_ALL_SELECTION: {
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.pref_clear_all))
                        .setMessage(getString(R.string.pref_dialog_clear_all))
                        .setPositiveButton(R.string.ok, (dialog, which) -> {
                            Set<String> emptySet = new HashSet<>();
                            PTManager.storeVPNApplications(prefs, VPNMode.ALLOW, emptySet);
                            PTManager.storeVPNApplications(prefs, VPNMode.DISALLOW, emptySet);
                            updateVPNModeItem();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                break;
            }
            case RESET_CONNECTION_SETTINGS: {
                ((EditTextSummaryPreference)findPreference(PROXY_IP)).setText(getString(R.string.proxy_ip));
                ((EditTextSummaryPreference)findPreference(PROXY_PORT)).setText(getString(R.string.proxy_port));
                break;
            }
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case "theme": {
                MainActivity.applyTheme(((String) newValue));
                break;
            }
            case "proxy_mode": {
                boolean vpnMode = !(boolean)newValue;
                if(!vpnMode && context != null) {
                    Toast.makeText(context, R.string.proxy_mode_warning, Toast.LENGTH_LONG).show();
                }
                updateProxyVpn(vpnMode);
                break;
            }
            case "upstream_proxy": {
                updateUpstreamProxy(((boolean) newValue));
                break;
            }
            case "upstream_auth": {
                updateUpstreamProxyAuth(((boolean) newValue));
                break;
            }
            case "sni_trick": {
                if(!newValue.equals("DISABLED") && context != null && prefs.getBoolean("chunking", true)) {
                    Toast.makeText(context, R.string.sni_warning, Toast.LENGTH_LONG).show();
                }
                updateFakeSniStatus(newValue.toString());
                break;
            }
            case DNS_PROVIDER: {
                updateSpecDnsStatus(((String) newValue));
                break;
            }
            case VPN_CONNECTION_MODE: {
                if (preference instanceof ListPreference) {
                    final ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue((String) newValue);
                    prefDisallow.setEnabled(index == VPNMode.DISALLOW.ordinal());
                    prefAllow.setEnabled(index == VPNMode.ALLOW.ordinal());
                    preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
                }
                break;
            }
        }
        return true;
    }

    private void transitionFragment(PreferenceFragment nextPreferenceFragment) {
        getFragmentManager().beginTransaction().addToBackStack(null)
                .replace(android.R.id.content, nextPreferenceFragment).commit();
    }
}
