package tun.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import tun.proxy.MyApplication;

import java.util.*;


public class PackageUtil {

    public static Map<String, String[]> getPackageMap() {
        final Map<String, String[]> packageMap = new HashMap<>();
        Context context = MyApplication.getInstance().getApplicationContext();
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> installedPackages = pm.getInstalledPackages(PackageManager.GET_META_DATA);
        Collections.sort(installedPackages, new Comparator<PackageInfo>() {
            @Override
            public int compare(PackageInfo t1, PackageInfo t2) {
            String t1pkg = t1.packageName;
            String t2pkg = t2.packageName;
            return t1pkg.compareToIgnoreCase(t2pkg);
            }
        });
        // ソート後
        List<String> pkgLabelList = new ArrayList<>();
        List<String> pkgNameList = new ArrayList<>();
        for (final PackageInfo pi : installedPackages) {
            pkgLabelList.add(pi.applicationInfo.loadLabel(pm).toString() + "(" + pi.packageName + ")");
            pkgNameList.add(pi.packageName);
        }
        packageMap.put("entry", pkgLabelList.toArray(new String[0]));
        packageMap.put("value", pkgNameList.toArray(new String[0]));

        return packageMap;
    }

}
