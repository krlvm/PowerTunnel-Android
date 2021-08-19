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

package tun.proxy.preferences.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import androidx.preference.PreferenceManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ru.krlvm.powertunnel.android.R;
import ru.krlvm.powertunnel.android.managers.PTManager;
import ru.krlvm.powertunnel.android.managers.PTManager.VPNMode;
import tun.proxy.preferences.SimplePreferenceActivity;

public class PackageListPreferenceFragment extends PreferenceFragment
        implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    private final Map<String, Boolean> mAllPackageInfoMap = new HashMap<>();
    private VPNMode mode;

    private AppSortBy sortBy = AppSortBy.APPLICATION_NAME;
    private String searchFilter = "";
    private SearchView searchView;
    private PreferenceScreen mFilterPreferenceScreen;

    public static PackageListPreferenceFragment create(VPNMode mode) {
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
        getActivity().setTitle(getString(mode.getDisplayName()));
    }

    protected void filter(String filter) {
        filter(filter, this.sortBy);
    }

    protected void filter(String filter, AppSortBy sortBy) {
        if (filter == null) {
            filter = searchFilter;
        } else {
            searchFilter = filter;
        }
        this.sortBy = sortBy;

        storeSelectedPackageSet(getAllSelectedPackageSet());

        removeAllPreferenceScreen();
        filterPackagesPreferences(filter, sortBy);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_search, menu);

        this.searchView = (SearchView) menu.findItem(R.id.menu_search_item).getActionView();
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
        Set<String> loadMap = PTManager.getVPNApplications(PreferenceManager.getDefaultSharedPreferences(getActivity()), mode);
        for (String pkgName : loadMap) {
            mAllPackageInfoMap.put(pkgName, loadMap.contains(pkgName));
        }
        filter(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                startActivity(new Intent(getActivity(), SimplePreferenceActivity.class));
                return true;
            }
            case R.id.menu_sort_app_name: {
                item.setChecked(!item.isChecked());
                filter(null, AppSortBy.APPLICATION_NAME);
                break;
            }
            case R.id.menu_sort_pkg_name: {
                item.setChecked(!item.isChecked());
                filter(null, AppSortBy.PACKAGE_NAME);
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
        } else {
            filter("");
        }
        return true;
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

    private void removeAllPreferenceScreen() {
        mFilterPreferenceScreen.removeAll();
    }

    private void filterPackagesPreferences(String filter, AppSortBy sortBy) {
        Context context = getActivity();
        final PackageManager pm = context.getPackageManager();
        List<PackageInfo> installedPackages = pm.getInstalledPackages(PackageManager.GET_META_DATA);
        Collections.sort(installedPackages, (o1, o2) -> {
            String t1 = "";
            String t2 = "";
            switch (sortBy) {
                case APPLICATION_NAME: {
                    t1 = o1.applicationInfo.loadLabel(pm).toString();
                    t2 = o2.applicationInfo.loadLabel(pm).toString();
                    break;
                }
                case PACKAGE_NAME: {
                    t1 = o1.packageName;
                    t2 = o2.packageName;
                    break;
                }
            }
            return t1.compareTo(t2);
        });

        final Map<String, Boolean> installedPackageMap = new HashMap<>();
        for (final PackageInfo pkgInfo : installedPackages) {
            if(pkgInfo.packageName == null) continue;
            Boolean b = mAllPackageInfoMap.get(pkgInfo.packageName);
            boolean checked = b != null && b;
            installedPackageMap.put(pkgInfo.packageName, checked);
        }
        this.mAllPackageInfoMap.clear();
        this.mAllPackageInfoMap.putAll(installedPackageMap);

        for (PackageInfo pkgInfo : installedPackages) {
            if (pkgInfo.packageName.equals(getActivity().getPackageName())) {
                continue;
            }
            String label = pkgInfo.applicationInfo.loadLabel(pm).toString();
            if (filter.trim().isEmpty() || label.toLowerCase().contains(filter.toLowerCase())) {
                final Preference preference = buildPackagePreferences(pm, pkgInfo);
                this.mFilterPreferenceScreen.addPreference(preference);
            }
        }
    }

    private Preference buildPackagePreferences(PackageManager pm, PackageInfo pkgInfo) {
        final CheckBoxPreference prefCheck = new CheckBoxPreference(getActivity());
        prefCheck.setIcon(pkgInfo.applicationInfo.loadIcon(pm));
        prefCheck.setTitle(pkgInfo.applicationInfo.loadLabel(pm).toString());
        prefCheck.setSummary(pkgInfo.packageName);

        Boolean b = mAllPackageInfoMap.get(pkgInfo.packageName);
        boolean checked = b != null && b;
        prefCheck.setChecked(checked);
        Preference.OnPreferenceClickListener click = preference -> {
            mAllPackageInfoMap.put(prefCheck.getSummary().toString(), prefCheck.isChecked());
            return false;
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

    private Set<String> getAllSelectedPackageSet() {
        Set<String> selected = this.getFilterSelectedPackageSet();
        for (Map.Entry<String, Boolean> value : this.mAllPackageInfoMap.entrySet()) {
            if (value.getValue()) {
                selected.add(value.getKey());
            }
        }
        return selected;
    }

    private void clearAllSelectedPackageSet() {
        for (Map.Entry<String, Boolean> value : this.mAllPackageInfoMap.entrySet()) {
            if (value.getValue()) {
                value.setValue(false);
            }
        }
    }

    private void storeSelectedPackageSet(Set<String> set) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        //PTManager.storeVPNMode(prefs, mode);
        PTManager.storeVPNApplications(prefs, mode, set);
    }

    public enum AppSortBy {
        APPLICATION_NAME,
        PACKAGE_NAME
    }
}