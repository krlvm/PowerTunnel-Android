package tun.proxy;

import android.app.AlertDialog;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.*;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.krlvm.powertunnel.android.MainActivity;
import ru.krlvm.powertunnel.android.R;

import static android.preference.Preference.*;

public class SimplePreferenceFragment extends PreferenceFragment
        implements OnPreferenceClickListener {
    public static final String VPN_CONNECTION_MODE = "vpn_connection_mode";
    public static final String VPN_DISALLOWED_APPLICATION_LIST = "vpn_disallowed_application_list";
    public static final String VPN_ALLOWED_APPLICATION_LIST = "vpn_allowed_application_list";
    public static final String VPN_CLEAR_ALL_SELECTION = "vpn_clear_all_selection";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        setHasOptionsMenu(true);

        /* Allowed / Disallowed Application */
        final ListPreference prefPackage = (ListPreference) this.findPreference(VPN_CONNECTION_MODE);
        final PreferenceScreen prefDisallow = (PreferenceScreen) findPreference(VPN_DISALLOWED_APPLICATION_LIST);
        final PreferenceScreen prefAllow = (PreferenceScreen) findPreference(VPN_ALLOWED_APPLICATION_LIST);
        final PreferenceScreen clearAllSelection = (PreferenceScreen) findPreference(VPN_CLEAR_ALL_SELECTION);
        prefDisallow.setOnPreferenceClickListener(this);
        prefAllow.setOnPreferenceClickListener(this);
        clearAllSelection.setOnPreferenceClickListener(this);

        prefPackage.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                if (preference instanceof ListPreference) {
                    final ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue((String) value);
                    prefDisallow.setEnabled(index == MyApplication.VPNMode.DISALLOW.ordinal());
                    prefAllow.setEnabled(index == MyApplication.VPNMode.ALLOW.ordinal());

                    // Set the summary to reflect the new value.
                    preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

                }
                return true;
            }
        });
        prefPackage.setSummary(prefPackage.getEntry());
        prefDisallow.setEnabled(MyApplication.VPNMode.DISALLOW.name().equals(prefPackage.getValue()));
        prefAllow.setEnabled(MyApplication.VPNMode.ALLOW.name().equals(prefPackage.getValue()));

        updateMenuItem();

    }

    private void updateMenuItem() {
        final PreferenceScreen prefDisallow = (PreferenceScreen) findPreference(VPN_DISALLOWED_APPLICATION_LIST);
        final PreferenceScreen prefAllow = (PreferenceScreen) findPreference(VPN_ALLOWED_APPLICATION_LIST);

        int countDisallow = MyApplication.getInstance().loadVPNApplication(MyApplication.VPNMode.DISALLOW).size();
        int countAllow = MyApplication.getInstance().loadVPNApplication(MyApplication.VPNMode.ALLOW).size();
        prefDisallow.setTitle(getString(R.string.pref_disallowed_application_list) + String.format(" (%d)", countDisallow));
        prefAllow.setTitle(getString(R.string.pref_allowed_application_list) + String.format(" (%d)",countAllow));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                startActivity(new Intent(getActivity(), MainActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // リスナー部分
    @Override
    public boolean onPreferenceClick(Preference preference) {
        // keyを見てクリックされたPreferenceを特定
        switch (preference.getKey()) {
            case VPN_DISALLOWED_APPLICATION_LIST:
                transitionFragment(PackageListPreferenceFragment.newInstance(MyApplication.VPNMode.DISALLOW));
                break;
            case VPN_ALLOWED_APPLICATION_LIST:
                transitionFragment(PackageListPreferenceFragment.newInstance(MyApplication.VPNMode.ALLOW));
                break;
            case VPN_CLEAR_ALL_SELECTION:
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.pref_clear_all))
                        .setMessage(getString(R.string.pref_dialog_clear_all))
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Set set = new HashSet();
                                MyApplication.getInstance().storeVPNApplication(MyApplication.VPNMode.ALLOW, set);
                                MyApplication.getInstance().storeVPNApplication(MyApplication.VPNMode.DISALLOW, set);
                                updateMenuItem();
                            }
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();

            break;
        }
        return false;
    }



    private void transitionFragment(PreferenceFragment nextPreferenceFragment) {
        // replaceによるFragmentの切り替えと、addToBackStackで戻るボタンを押した時に前のFragmentに戻るようにする
        getFragmentManager()
            .beginTransaction()
            .addToBackStack(null)
            .replace(android.R.id.content, nextPreferenceFragment)
            .commit();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static class PackageListPreferenceFragment extends PreferenceFragment
            implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {
        final private Map<String, Boolean> mAllPackageInfoMap = new HashMap<>();

        private MyApplication.VPNMode mode = MyApplication.VPNMode.DISALLOW;
        private MyApplication.AppSortBy appSortBy = MyApplication.AppSortBy.APPNAME;
        private PreferenceScreen mFilterPreferenceScreen;

        public static PackageListPreferenceFragment newInstance(MyApplication.VPNMode mode) {
            final PackageListPreferenceFragment fragment = new PackageListPreferenceFragment();
            fragment.mode = mode;
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
            mFilterPreferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
            setPreferenceScreen(mFilterPreferenceScreen);
        }

        private String searchFilter = "";
        private SearchView searchView;

        protected void filter(String filter) {
            this.filter(filter, this.appSortBy);
        }

        protected void filter(String filter, final MyApplication.AppSortBy sortBy) {
            if (filter == null) {
                filter = searchFilter;
            } else {
                searchFilter = filter;
            }
            this.appSortBy = sortBy;

            Set<String> selected = this.getAllSelectedPackageSet();
            storeSelectedPackageSet(selected);

            this.removeAllPreferenceScreen();
            this.filterPackagesPreferences(filter, sortBy);
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            // Menuの設定
            inflater.inflate(R.menu.menu_search, menu);

            //MenuCompat.setGroupDividerEnabled(menu, true);

            final MenuItem menuItem = menu.findItem(R.id.menu_search_item);

            this.searchView = (SearchView) menuItem.getActionView();
            this.searchView.setOnQueryTextListener(this);
            this.searchView.setOnCloseListener(this);
            this.searchView.setSubmitButtonEnabled(false);

        }

        @Override
        public void onPause() {
            super.onPause();
            Set<String> selected = this.getAllSelectedPackageSet();
            storeSelectedPackageSet(selected);
        }

        @Override
        public void onResume() {
            super.onResume();
            Set<String> loadMap = MyApplication.getInstance().loadVPNApplication(this.mode);
            for (String pkgName : loadMap) {
                this.mAllPackageInfoMap.put(pkgName, loadMap.contains(pkgName));
            }
            filter(null);
        }

        private void removeAllPreferenceScreen() {
            mFilterPreferenceScreen.removeAll();
        }

        private void filterPackagesPreferences(String filter, final MyApplication.AppSortBy sortBy) {
            final Context context = MyApplication.getInstance().getApplicationContext();
            final PackageManager pm = context.getPackageManager();
            final List<PackageInfo> installedPackages = pm.getInstalledPackages(PackageManager.GET_META_DATA);
            Collections.sort(installedPackages, new Comparator<PackageInfo>() {
                @Override
                public int compare(PackageInfo o1, PackageInfo o2) {
                    String t1 = "";
                    String t2 = "";
                    switch (sortBy) {
                        case APPNAME:
                            t1 = o1.applicationInfo.loadLabel(pm).toString();
                            t2 = o2.applicationInfo.loadLabel(pm).toString();
                            break;
                        case PKGNAME:
                            t1 = o1.packageName;
                            t2 = o2.packageName;
                            break;
                    }
                    return t1.compareTo(t2);
                }
            });

            final Map<String, Boolean> installedPackageMap = new HashMap<>();
            for (final PackageInfo pi : installedPackages) {
                boolean checked = this.mAllPackageInfoMap.containsKey(pi.packageName) ? this.mAllPackageInfoMap.get(pi.packageName) : false;
                installedPackageMap.put(pi.packageName, checked);
            }
            this.mAllPackageInfoMap.clear();
            this.mAllPackageInfoMap.putAll(installedPackageMap);

            for (final PackageInfo pi : installedPackages) {
                if(pi.packageName.equals(MyApplication.getInstance().getPackageName())) {
                    continue;
                }
                String t1 = pi.applicationInfo.loadLabel(pm).toString();
                if (filter.trim().isEmpty() || t1.toLowerCase().contains(filter.toLowerCase())) {
                    final Preference preference = buildPackagePreferences(pm, pi);
                    this.mFilterPreferenceScreen.addPreference(preference);
                }
            }
        }

        private Preference buildPackagePreferences(final PackageManager pm, final PackageInfo pi) {
            final CheckBoxPreference prefCheck = new CheckBoxPreference(getActivity());
            prefCheck.setIcon(pi.applicationInfo.loadIcon(pm));
            prefCheck.setTitle(pi.applicationInfo.loadLabel(pm).toString());
            prefCheck.setSummary(pi.packageName);
            boolean checked = this.mAllPackageInfoMap.containsKey(pi.packageName) ? this.mAllPackageInfoMap.get(pi.packageName) : false;
            prefCheck.setChecked(checked);
            OnPreferenceClickListener click = new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    mAllPackageInfoMap.put(prefCheck.getSummary().toString(), prefCheck.isChecked());
                    return false;
                }
            };
            prefCheck.setOnPreferenceClickListener(click);
            return prefCheck;
        }

        private Set<String> getFilterSelectedPackageSet() {
            final Set<String> selected = new HashSet<>();
            for (int i = 0; i < this.mFilterPreferenceScreen.getPreferenceCount(); i++) {
                Preference pref = this.mFilterPreferenceScreen.getPreference(i);
                if ((pref instanceof CheckBoxPreference)) {
                    CheckBoxPreference prefCheck = (CheckBoxPreference) pref;
                    if (prefCheck.isChecked()) {
                        selected.add(prefCheck.getSummary().toString());
                    }
                }
            }
            return selected;
        }

        private void setSelectedPackageSet(Set<String> selected) {
            for (int i = 0; i < this.mFilterPreferenceScreen.getPreferenceCount(); i++) {
                Preference pref = this.mFilterPreferenceScreen.getPreference(i);
                if ((pref instanceof CheckBoxPreference)) {
                    CheckBoxPreference prefCheck = (CheckBoxPreference) pref;
                    if (selected.contains((prefCheck.getSummary()))) {
                        prefCheck.setChecked(true);
                    }
                }
            }
        }

        private void  clearAllSelectedPackageSet() {
            Set<String> selected = this.getFilterSelectedPackageSet();
            for (Map.Entry<String, Boolean> value : this.mAllPackageInfoMap
                    .entrySet()) {
                if (value.getValue()) {
                    value.setValue(false);
                }
            }
        }

        private Set<String>  getAllSelectedPackageSet() {
            Set<String> selected = this.getFilterSelectedPackageSet();
            for (Map.Entry<String, Boolean> value : this.mAllPackageInfoMap.entrySet()) {
                if (value.getValue()) {
                    selected.add(value.getKey());
                }
            }
            return selected;
        }

        private void storeSelectedPackageSet(final Set<String> set) {
            MyApplication.getInstance().storeVPNMode(this.mode);
            MyApplication.getInstance().storeVPNApplication(this.mode, set);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            switch (id) {
                case android.R.id.home:
                    startActivity(new Intent(getActivity(), SimplePreferenceActivity.class));
                    return true;
//                case R.id.menu_clear_all_selected:
//                    clearAllSelectedPackageSet();
//                    filter(null);
//                    break;
                case R.id.menu_sort_app_name:
                    item.setChecked(!item.isChecked());
                    filter(null, MyApplication.AppSortBy.APPNAME);
                    break;
                case R.id.menu_sort_pkg_name:
                    item.setChecked(!item.isChecked());
                    filter(null, MyApplication.AppSortBy.PKGNAME);
                    break;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public boolean onQueryTextSubmit(String query) {
            this.searchView.clearFocus();
            if (!query.trim().isEmpty()) {
                filter(query);
                return true;
            } else {
                filter("");
                return true;
            }
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }

        @Override
        public boolean onClose() {
            Set<String> selected = this.getAllSelectedPackageSet();
            storeSelectedPackageSet(selected);
            filter("");
            return false;
        }
    }

}
