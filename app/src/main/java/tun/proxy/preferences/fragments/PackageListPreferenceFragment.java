package tun.proxy.preferences.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.SearchView;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.krlvm.powertunnel.android.R;
import tun.proxy.MyApplication;
import tun.proxy.preferences.SimplePreferenceActivity;

public class PackageListPreferenceFragment extends PreferenceFragment implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {
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
        inflater.inflate(R.menu.menu_search, menu);

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
        Context context = MyApplication.getInstance().getApplicationContext();
        final PackageManager pm = context.getPackageManager();
        List<PackageInfo> installedPackages = pm.getInstalledPackages(PackageManager.GET_META_DATA);
        Collections.sort(installedPackages, new Comparator<PackageInfo>() {
            @Override
            public int compare(PackageInfo o1, PackageInfo o2) {
                String t1 = "";
                String t2 = "";
                switch (sortBy) {
                    case APPNAME: {
                        t1 = o1.applicationInfo.loadLabel(pm).toString();
                        t2 = o2.applicationInfo.loadLabel(pm).toString();
                        break;
                    }
                    case PKGNAME: {
                        t1 = o1.packageName;
                        t2 = o2.packageName;
                        break;
                    }
                }
                return t1.compareTo(t2);
            }
        });

        final Map<String, Boolean> installedPackageMap = new HashMap<>();
        for (final PackageInfo pi : installedPackages) {
            boolean checked = this.mAllPackageInfoMap.containsKey(pi.packageName) ?
                    this.mAllPackageInfoMap.get(pi.packageName) : false;
            installedPackageMap.put(pi.packageName, checked);
        }
        this.mAllPackageInfoMap.clear();
        this.mAllPackageInfoMap.putAll(installedPackageMap);

        for (final PackageInfo pi : installedPackages) {
            if (pi.packageName.equals(MyApplication.getInstance().getPackageName())) {
                continue;
            }
            String t1 = pi.applicationInfo.loadLabel(pm).toString();
            if (filter.trim().isEmpty() || t1.toLowerCase().contains(filter.toLowerCase())) {
                final Preference preference = buildPackagePreferences(pm, pi);
                this.mFilterPreferenceScreen.addPreference(preference);
            }
        }
    }

    private Preference buildPackagePreferences(PackageManager pm, PackageInfo pi) {
        final CheckBoxPreference prefCheck = new CheckBoxPreference(getActivity());
        prefCheck.setIcon(pi.applicationInfo.loadIcon(pm));
        prefCheck.setTitle(pi.applicationInfo.loadLabel(pm).toString());
        prefCheck.setSummary(pi.packageName);
        boolean checked = this.mAllPackageInfoMap.containsKey(pi.packageName) ?
                mAllPackageInfoMap.get(pi.packageName) : false;
        prefCheck.setChecked(checked);
        Preference.OnPreferenceClickListener click = new Preference.OnPreferenceClickListener() {
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
                if (selected.contains(prefCheck.getSummary())) {
                    prefCheck.setChecked(true);
                }
            }
        }
    }

    private void clearAllSelectedPackageSet() {
        for (Map.Entry<String, Boolean> value : this.mAllPackageInfoMap.entrySet()) {
            if (value.getValue()) {
                value.setValue(false);
            }
        }
    }

    private Set<String> getAllSelectedPackageSet() {
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
            case android.R.id.home: {
                startActivity(new Intent(getActivity(), SimplePreferenceActivity.class));
                return true;
            }
            case R.id.menu_sort_app_name: {
                item.setChecked(!item.isChecked());
                filter(null, MyApplication.AppSortBy.APPNAME);
                break;
            }
            case R.id.menu_sort_pkg_name: {
                item.setChecked(!item.isChecked());
                filter(null, MyApplication.AppSortBy.PKGNAME);
                break;
            }
            case R.id.menu_clear_all_selected: {
                clearAllSelectedPackageSet();
                filter(searchFilter);
                break;
            }
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