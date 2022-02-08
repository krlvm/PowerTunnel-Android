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

package io.github.krlvm.powertunnel.android.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import java.io.File;

import io.github.krlvm.powertunnel.android.R;
import io.github.krlvm.powertunnel.android.managers.ConfigurationManager;
import io.github.krlvm.powertunnel.android.utility.Utility;

public class SettingsActivity extends AppCompatActivity {

    private static final EditTextPreference.OnBindEditTextListener NUMERIC_EDIT_TEXT_LISTENER = editText ->
            editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    public static void makeNumeric(EditTextPreference editText) {
        editText.setOnBindEditTextListener(NUMERIC_EDIT_TEXT_LISTENER);
    }

    private static final int PERMISSION_STORAGE_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static void enableExternalConfigs(Context context) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit().putBoolean("external_configs", true).apply();
        new File(context.getFilesDir(), "configs").delete();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

            findPreference("theme").setOnPreferenceChangeListener((preference, newValue) -> {
                Utility.applyTheme(((String) newValue));
                return true;
            });

            findPreference("upstream_proxy_protocol").setEnabled(prefs.getBoolean("upstream_proxy_enabled", false));
            findPreference("upstream_proxy_enabled").setOnPreferenceChangeListener(((preference, newValue) -> {
                findPreference("upstream_proxy_protocol").setEnabled((boolean) newValue);
                return true;
            }));

            findPreference("external_configs").setOnPreferenceChangeListener(((preference, newValue) -> {
                if (!((boolean) newValue)) return true;
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.dialog_external_configs_title)
                        .setMessage(R.string.dialog_external_configs_message)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            dialog.dismiss();
                            if (ConfigurationManager.checkStorageAccess(getContext(), false)) {
                                enableExternalConfigs(getActivity());
                                ((SwitchPreferenceCompat) preference).setChecked(true);
                            } else {
                                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        PERMISSION_STORAGE_REQUEST_CODE);
                            }
                        })
                        .setNegativeButton(R.string.cancel, ((dialog, which) -> dialog.dismiss()))
                        .show();
                return false;
            }));

            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                findPreference("mode").setEnabled(false);
                findPreference("category_vpn").setEnabled(false);
            }

            makeNumeric(findPreference("proxy_port"));
            makeNumeric(findPreference("upstream_proxy_port"));

            findPreference("vpn_apps_mode").setOnPreferenceChangeListener(((preference, newValue) -> {
                updateVpnAppsDependents(((String) newValue));
                return true;
            }));
            updateVpnAppsDependents(prefs.getString("vpn_apps_mode", "exclude"));

            openAppListOnClick("vpn_apps_excluded", R.string.settings_pref_vpn_apps_excluded);
            openAppListOnClick("vpn_apps_allowed", R.string.settings_pref_vpn_apps_allowed);

            findPreference("external_configs").setOnPreferenceChangeListener(((preference, newValue) -> {
                if (!((boolean) newValue)) return true;
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.dialog_external_configs_title)
                        .setMessage(R.string.dialog_external_configs_message)
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            dialog.dismiss();
                            if (ConfigurationManager.checkStorageAccess(getContext(), false)) {
                                enableExternalConfigs(getActivity());
                                ((SwitchPreferenceCompat) preference).setChecked(true);
                            } else {
                                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        PERMISSION_STORAGE_REQUEST_CODE);
                            }
                        })
                        .setNegativeButton(R.string.cancel, ((dialog, which) -> dialog.dismiss()))
                        .show();
                return false;
            }));
        }

        private void updateVpnAppsDependents(String value) {
            findPreference("vpn_apps_excluded").setEnabled("exclude".equals(value));
            findPreference("vpn_apps_allowed").setEnabled("allow".equals(value));
        }

        private void openAppListOnClick(String key, @StringRes int titleId) {
            findPreference(key).setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getContext(), AppListActivity.class)
                        .putExtra("title", titleId)
                        .putExtra("key", preference.getKey()));
                return true;
            });
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            switch (requestCode) {
                case PERMISSION_STORAGE_REQUEST_CODE: {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        enableExternalConfigs(getContext());
                        ((SwitchPreferenceCompat) findPreference("external_configs")).setChecked(true);
                    }
                    break;
                }
            }
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}