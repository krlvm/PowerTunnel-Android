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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import io.github.krlvm.powertunnel.android.BuildConfig;
import io.github.krlvm.powertunnel.android.R;
import io.github.krlvm.powertunnel.android.plugin.AndroidPluginLoader;
import io.github.krlvm.powertunnel.android.utility.FileUtility;

public class AssetPluginsManager {

    public static void extract(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(prefs.getInt("version_code", -1) == BuildConfig.VERSION_CODE) return;

        try {
            extractPlugins(context);
        } catch (IOException ex) {
            Log.e("AssetMan", "Failed to extract default plugins: " + ex.getMessage(), ex);
            Toast.makeText(context, R.string.toast_failed_to_extract_plugins, Toast.LENGTH_LONG).show();
        }

        prefs.edit().putInt("version_code", BuildConfig.VERSION_CODE).apply();
    }

    private static void extractPlugins(Context context) throws IOException {
        final AssetManager manager = context.getAssets();
        for (String plugin : manager.list("plugins")) {
            final File file = new File(AndroidPluginLoader.getPluginsDir(context), plugin);
            try(InputStream in = manager.open("plugins/" + plugin)) {
                FileUtility.copy(in, file);
            }
        }
    }
}
