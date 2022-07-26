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

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.github.krlvm.powertunnel.android.R;
import io.github.krlvm.powertunnel.android.adapters.PluginAdapter;
import io.github.krlvm.powertunnel.android.databinding.ListActivityBinding;
import io.github.krlvm.powertunnel.android.plugin.AndroidPluginLoader;
import io.github.krlvm.powertunnel.android.services.PowerTunnelService;
import io.github.krlvm.powertunnel.android.utility.FileUtility;
import io.github.krlvm.powertunnel.android.utility.Utility;
import io.github.krlvm.powertunnel.plugin.PluginLoader;
import io.github.krlvm.powertunnel.sdk.exceptions.PluginLoadException;
import io.github.krlvm.powertunnel.sdk.plugin.PluginInfo;
import io.github.krlvm.powertunnel.utilities.JarLoader;

public class PluginsActivity extends AppCompatActivity {

    private static final String LOG_TAG = "PlugMan";

    private static final int REQUEST_CODE_UPLOAD = 1;

    private ListActivityBinding binding;
    private PluginAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ListActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        binding.appList.setLayoutManager(new LinearLayoutManager(this));
        binding.appList.addItemDecoration(new DividerItemDecoration(
                binding.appList.getContext(),
                RecyclerView.VERTICAL
        ));
        resetAdapter();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!prefs.getBoolean("plugins_activity_tip", false)) {
            Toast.makeText(this, R.string.plugins_activity_tip, Toast.LENGTH_LONG).show();
            prefs.edit().putBoolean("plugins_activity_tip", true).apply();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_plugins, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == android.R.id.home) {
            super.onBackPressed();
        } else if(id == R.id.action_install) {
            if(PowerTunnelService.isRunning()) {
                Toast.makeText(this, R.string.toast_plugin_stop_server_to_act, Toast.LENGTH_SHORT).show();
            } else {
                final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? "application/java-archive" : "*/*");
                startActivityForResult(intent, REQUEST_CODE_UPLOAD);
            }
        } else if(id == R.id.action_registry) {
            Utility.launchUri(this, "https://github.com/krlvm/PowerTunnel-Plugins/blob/master/README.md");
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_UPLOAD: {
                if(resultCode != RESULT_OK) return;
                try {
                    FileUtility.copy(getContentResolver().openInputStream(data.getData()),
                            PluginLoader.getPluginFile(
                                    getFilesDir(), FileUtility.getFileName(this, data.getData())
                            )
                    );
                    resetAdapter();
                    Toast.makeText(this, R.string.toast_plugin_installed, Toast.LENGTH_LONG).show();
                } catch (IOException ex) {
                    Log.e(LOG_TAG, "Failed to copy plugin file: " + ex.getMessage(), ex);
                    Toast.makeText(this, R.string.toast_failed_to_install_plugin, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void resetAdapter() {
        binding.appList.setVisibility(View.GONE);
        binding.progressCircular.setVisibility(View.VISIBLE);

        final List<PluginInfo> plugins = new ArrayList<>();
        for (File plugin : AndroidPluginLoader.enumeratePlugins(this)) {
            try {
                JarLoader.open(plugin, PluginLoader.PLUGIN_MANIFEST, (in) -> {
                    try {
                        plugins.add(PluginLoader.parsePluginInfo(plugin.getName(), in));
                    } catch (IOException ex) {
                        Log.e(LOG_TAG, String.format("Failed to read manifest of '%s': %s", plugin.getName(), ex.getMessage()), ex);
                    } catch (PluginLoadException ex) {
                        Log.e(LOG_TAG, String.format("Failed to parse manifest of '%s': %s", plugin.getName(), ex.getMessage()), ex);
                    }
                });
            } catch (IOException ex) {
                Log.e(LOG_TAG, String.format("Failed to open plugin '%s' jar file: %s", plugin.getName(), ex.getMessage()), ex);
            }
        }
        Collections.sort(plugins, Comparator.comparing(PluginInfo::getName));
        new Thread(() -> runOnUiThread(() -> {
            adapter = new PluginAdapter(this, plugins);
            binding.appList.setAdapter(adapter);
            binding.progressCircular.setVisibility(View.GONE);
            binding.appList.setVisibility(View.VISIBLE);
        })).start();
    }
}
