package io.github.krlvm.powertunnel.android.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.view.MenuCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.krlvm.powertunnel.android.R;
import io.github.krlvm.powertunnel.android.activities.ConfigEditActivity;
import io.github.krlvm.powertunnel.android.activities.PluginSettingsActivity;
import io.github.krlvm.powertunnel.android.activities.PluginsActivity;
import io.github.krlvm.powertunnel.android.databinding.PluginItemBinding;
import io.github.krlvm.powertunnel.android.managers.PluginManager;
import io.github.krlvm.powertunnel.android.plugin.AndroidPluginLoader;
import io.github.krlvm.powertunnel.android.preferences.AndroidPluginPreferenceParser;
import io.github.krlvm.powertunnel.android.services.PowerTunnelService;
import io.github.krlvm.powertunnel.android.utility.Utility;
import io.github.krlvm.powertunnel.sdk.plugin.PluginInfo;

public class PluginAdapter extends RecyclerView.Adapter<PluginAdapter.ViewHolder> {

    private final Context context;
    private final LayoutInflater inflater;
    private final List<PluginInfo> plugins;

    private final Set<String> disabledSources;

    public PluginAdapter(Context context, List<PluginInfo> plugins) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.plugins = plugins;

        disabledSources = PluginManager.getDisabledPlugins(context);
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(PluginItemBinding.inflate(inflater));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final PluginInfo plugin = plugins.get(position);

        holder.binding.pluginName.setText(plugin.getName());

        holder.binding.pluginDetails.setText(plugin.getDescription());
        holder.binding.pluginVersion.setText("Version " + plugin.getVersion());
        holder.binding.pluginAuthor.setText("by " + plugin.getAuthor());
        holder.binding.stateCheckbox.setChecked(!disabledSources.contains(plugin.getSource()));

        holder.binding.stateCheckbox.setOnClickListener(v -> {
            if (disabledSources.contains(plugin.getSource())) {
                disabledSources.remove(plugin.getSource());
            } else {
                disabledSources.add(plugin.getSource());
            }
            holder.binding.stateCheckbox.setChecked(!disabledSources.contains(plugin.getSource()));
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putStringSet("disabled_plugins", new HashSet<>(disabledSources)).apply();
        });

        TooltipCompat.setTooltipText(holder.binding.pluginSettings, holder.binding.pluginSettings.getContentDescription());
        holder.binding.pluginSettings.setOnClickListener(v -> {
            new Thread(() -> {
                AndroidPluginPreferenceParser.loadPreferences(((PluginsActivity) context), plugin, preferences -> {
                    if (preferences == null) {
                        ((PluginsActivity) context).runOnUiThread(() -> {
                            if (!showAdditionalConfigurationMenu(v, plugin)) {
                                Toast.makeText(context, R.string.toast_plugin_not_configurable, Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                    context.startActivity(new Intent(context, PluginSettingsActivity.class)
                            .putExtra("plugin", plugin)
                            .putExtra("preferences", (Serializable) preferences)
                    );
                });
            }).start();
        });
        holder.binding.pluginSettings.setOnLongClickListener(v -> showAdditionalConfigurationMenu(v, plugin));

        holder.binding.pluginUninstall.setOnClickListener(v -> {
            if (PowerTunnelService.isRunning()) {
                Toast.makeText(context, R.string.toast_plugin_stop_server_to_act, Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(context)
                    .setTitle(R.string.dialog_plugin_uninstall_title)
                    .setMessage(R.string.dialog_plugin_uninstall_message)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        dialog.dismiss();

                        if (new File(AndroidPluginLoader.getPluginsDir(context), plugin.getSource()).delete()) {
                            AndroidPluginLoader.deleteOatCache(context);
                            plugins.remove(plugin);
                            notifyDataSetChanged();
                            Toast.makeText(context, context.getString(R.string.toast_plugin_uninstalled, plugin.getName()), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(context, R.string.toast_failed_to_uninstall_plugin, Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(R.string.cancel, ((dialog, which) -> dialog.dismiss()))
                    .show();
        });

        TooltipCompat.setTooltipText(holder.binding.pluginInfo, holder.binding.pluginInfo.getContentDescription());
        holder.binding.pluginInfo.setOnClickListener(v -> {
            final AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle(plugin.getName())
                    .setMessage(plugin.getDescription() + "\n" +
                            "\nVersion " + plugin.getVersion() +
                            "\nAuthor: " + plugin.getAuthor() + "\n" +
                            "\nID: " + plugin.getId() +
                            "\nSource: " + plugin.getSource()
                    );
            if (plugin.getHomepage() != null) {
                builder.setPositiveButton(R.string.visit_homepage, (dialog, which) ->
                        Utility.launchUri(context, plugin.getHomepage()));
            }
            builder.show();
        });

        if (hasDuplicates(plugin)) {
            holder.binding.getRoot().setBackgroundColor(Color.argb(128, 255, 0, 0));
        } else {
            holder.binding.getRoot().setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return plugins.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final PluginItemBinding binding;

        ViewHolder(PluginItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private boolean hasDuplicates(PluginInfo plugin) {
        return countPluginsById(plugin.getId()) > 1;
    }
    private int countPluginsById(String id) {
        int i = 0;
        for (PluginInfo plugin : this.plugins) {
            if (plugin.getId().equals(id)) {
                i++;
            }
        }
        return i;
    }

    private boolean showAdditionalConfigurationMenu(View view, PluginInfo plugin) {
        final String[] files = plugin.getConfigurationFiles();
        if (files.length == 0) return false;

        PopupMenu menu = new PopupMenu(context, view);
        MenuCompat.setGroupDividerEnabled(menu.getMenu(), true);

        menu.getMenu().add(0, 0, 0, R.string.menu_plugin_additional_configuration_title)
                .setEnabled(false);

        final MenuItem.OnMenuItemClickListener listener = item -> {
            final String fileName = item.getTitle().toString();
            context.startActivity(new Intent(context, ConfigEditActivity.class)
                    .putExtra("plugin", plugin.getName())
                    .putExtra("file", fileName));
            return true;
        };
        for (int i = 0; i < files.length; i++) {
            String fileName = files[i];
            menu.getMenu().add(1, i, i, fileName).setOnMenuItemClickListener(listener);
        }

        menu.show();
        return true;
    }
}