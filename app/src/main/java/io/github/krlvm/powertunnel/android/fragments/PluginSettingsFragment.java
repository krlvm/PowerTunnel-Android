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

package io.github.krlvm.powertunnel.android.fragments;


import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.preference.CheckBoxPreference;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import java.util.List;

import io.github.krlvm.powertunnel.android.activities.SettingsActivity;
import io.github.krlvm.powertunnel.android.preferences.AndroidConfigurationStoreWrapper;
import io.github.krlvm.powertunnel.preferences.Preference;
import io.github.krlvm.powertunnel.preferences.PreferenceGroup;
import io.github.krlvm.powertunnel.preferences.PreferenceType;
import io.github.krlvm.powertunnel.sdk.plugin.PluginInfo;

public class PluginSettingsFragment extends PreferenceFragmentCompat {

    private final Handler handler = new Handler();
    private PreferenceScreen screen;
    private PreferenceDataStore store;

    private PluginInfo plugin;
    private List<PreferenceGroup> preferences;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        plugin = ((PluginInfo) getArguments().getSerializable("plugin"));
        preferences = ((List<PreferenceGroup>) getArguments().getSerializable("preferences"));

        getActivity().setTitle(plugin.getName());
        getPreferenceManager().setPreferenceDataStore(store = new AndroidConfigurationStoreWrapper(
                getActivity(), plugin.getId(), (errorStringRes) -> {

            Toast.makeText(getActivity(), errorStringRes, Toast.LENGTH_LONG).show();
            getActivity().finish();
        }));
        screen = getPreferenceManager().createPreferenceScreen(getActivity());
        setPreferenceScreen(screen);

        for (PreferenceGroup group : preferences) {
            final PreferenceCategory category = group.getTitle() != null ?
                    new PreferenceCategory(getActivity()) : null;
            if (category != null) {
                category.setTitle(group.getTitle());
                screen.addPreference(category);
            }
            for (Preference preference : group.getPreferences()) {
                androidx.preference.Preference pref;
                switch (preference.getType()) {
                    case CHECKBOX: {
                        pref = new CheckBoxPreference(getActivity());
                        break;
                    }
                    case SWITCH: {
                        pref = new SwitchPreferenceCompat(getActivity());
                        break;
                    }
                    case NUMBER:
                    case STRING: {
                        pref = new EditTextPreference(getActivity());
                        if (preference.getType() == PreferenceType.NUMBER) {
                            SettingsActivity.makeNumeric(((EditTextPreference) pref));
                        }
                        ((EditTextPreference) pref).setDialogTitle(preference.getTitle());
                        pref.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
                        break;
                    }
                    case SELECT: {
                        pref = new ListPreference(getActivity());
                        pref.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
                        ((ListPreference) pref).setDialogTitle(preference.getTitle());
                        ((ListPreference) pref).setEntries(preference.getItems().values().toArray(new String[0]));
                        ((ListPreference) pref).setEntryValues(preference.getItems().keySet().toArray(new String[0]));
                        break;
                    }
                    default:
                        throw new UnsupportedOperationException("Unsupported preference type");
                }
                pref.setKey(preference.getKey());
                pref.setTitle(preference.getTitle());
                pref.setOnPreferenceChangeListener((p, newValue) -> true);
                if (preference.getDefaultValue() != null) {
                    if (preference.getType() == PreferenceType.CHECKBOX || preference.getType() == PreferenceType.SWITCH) {
                        pref.setDefaultValue(Boolean.parseBoolean(preference.getDefaultValue()));
                        pref.setSingleLineTitle(false);
                    } else {
                        pref.setDefaultValue(preference.getDefaultValue());
                    }
                }
                preference.binding = pref;
                pref.setOnPreferenceChangeListener((preference1, newValue) -> {
                    handler.postDelayed(this::updateDependencies, 100);
                    return true;
                });
                if (category == null) {
                    screen.addPreference(pref);
                } else {
                    category.addPreference(pref);
                }
            }
        }
        updateDependencies();
    }

    private void updateDependencies() {
        for (PreferenceGroup group : preferences) {
            for (Preference preference : group.getPreferences()) {
                if (preference.binding == null) continue;
                ((androidx.preference.Preference) preference.binding).setEnabled(isSatisfied(preference));
            }
        }
    }

    private boolean isSatisfied(Preference preference) {
        if (preference.binding == null) return true;
        final String dependency = preference.getDependency();
        if (dependency == null) return true;
        final Preference target = PreferenceGroup.findPreference(preferences, dependency);
        if (target == null) return true;
        if (target.binding == null) return true;

        return preference.getDependencyValue()
                .equals(store.getString(target.getKey(), target.getDefaultValue())) &&
                isSatisfied(target);
    }
}
