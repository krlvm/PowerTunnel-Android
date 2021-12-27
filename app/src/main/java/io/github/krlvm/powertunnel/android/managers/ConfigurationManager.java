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

package io.github.krlvm.powertunnel.android.managers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;

import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import java.io.File;

public class ConfigurationManager {

    public static boolean isUseExternalConfigs(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("external_configs", false);
    }
    public static File getConfigsDirectory(Context context) {
        final File file = isUseExternalConfigs(context) ?
                getExternalConfigsDirectory(context) : new File(context.getFilesDir(), "configs");
        if(!file.exists()) file.mkdir();
        return file;
    }
    public static File getExternalConfigsDirectory(Context context) {
        final File file = new File(Environment.getExternalStoragePublicDirectory(
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT ?
                        Environment.DIRECTORY_DOCUMENTS : "Documents"), "PowerTunnel");
        if(!file.exists()) file.mkdir();
        return file;
    }

    public static boolean checkStorageAccess(Context context) {
        return checkStorageAccess(context, true);
    }
    public static boolean checkStorageAccess(Context context, boolean checkPref) {
        if (checkPref && !isUseExternalConfigs(context)) return true;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        return context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
    public static void requestStorageAccess(Activity activity, int requestCode) {
        ActivityCompat.requestPermissions(
                activity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                requestCode
        );
    }
}
