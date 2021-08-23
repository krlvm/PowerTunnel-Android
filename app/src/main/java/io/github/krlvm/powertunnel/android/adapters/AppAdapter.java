package io.github.krlvm.powertunnel.android.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.krlvm.powertunnel.android.databinding.AppItemBinding;
import io.github.krlvm.powertunnel.android.types.AppInfo;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private final Context context;
    private final String preferenceKey;
    private final LayoutInflater inflater;
    private final List<AppInfo> source;
    private List<AppInfo> list;

    public AppAdapter(Context context, String preferenceKey, List<AppInfo> source) {
        this.context = context;
        this.preferenceKey = preferenceKey;
        this.inflater = LayoutInflater.from(context);
        this.source = source;

        filtrate(null);
    }

    public void filtrate(AppInfo.FilterCallback callback) {
        list = new ArrayList<>();
        if(callback == null) {
            list.addAll(source);
        } else {
            for (AppInfo app : source) {
                if (callback.filtrate(app)) {
                    list.add(app);
                }
            }
        }
        notifyDataSetChanged();
    }

    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AppItemBinding.inflate(inflater));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final AppInfo app = list.get(position);
        holder.binding.appLabel.setText(app.label);
        holder.binding.appPackage.setText(app.packageName);
        holder.binding.icon.setImageDrawable(app.icon);
        holder.binding.checkbox.setChecked(app.checked);

        holder.binding.getRoot().setOnClickListener(v -> {
            boolean checked = !holder.binding.checkbox.isChecked();
            holder.binding.checkbox.setChecked(checked);
            app.checked = checked;

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            Set<String> apps = new HashSet<>(prefs.getStringSet(preferenceKey, new HashSet<>()));
            if (apps.contains(app.packageName)) {
                apps.remove(app.packageName);
            } else {
                apps.add(app.packageName);
            }
            prefs.edit().putStringSet(preferenceKey, apps).apply();
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final AppItemBinding binding;

        ViewHolder(AppItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}