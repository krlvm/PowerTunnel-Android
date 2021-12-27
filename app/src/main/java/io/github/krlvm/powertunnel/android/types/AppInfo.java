package io.github.krlvm.powertunnel.android.types;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppInfo {
    public final String label;
    public final String packageName;
    public final Drawable icon;
    public boolean checked;

    AppInfo(String label, String packageName, Drawable icon) {
        this.label = label;
        this.packageName = packageName;
        this.icon = icon;
    }

    public static List<AppInfo> getInstalledApps(Context context, String filterKey) {
        final Set<String> checked = PreferenceManager.getDefaultSharedPreferences(context)
                .getStringSet(filterKey, new HashSet<>());

        final PackageManager packageManager = context.getPackageManager();

        List<AppInfo> apps = new ArrayList<>();
        List<PackageInfo> packages = packageManager.getInstalledPackages(0);
        for (PackageInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(context.getPackageName())) continue;
            final AppInfo appInfo = new AppInfo(
                    packageInfo.applicationInfo.loadLabel(packageManager).toString(),
                    packageInfo.packageName,
                    packageInfo.applicationInfo.loadIcon(packageManager)
            );
            appInfo.checked = checked.contains(appInfo.packageName);
            apps.add(appInfo);
        }

        Collections.sort(apps, (a, b) -> a.label.compareTo(b.label));

        return apps;
    }

    public interface FilterCallback {
        boolean filtrate(AppInfo app);
    }
}