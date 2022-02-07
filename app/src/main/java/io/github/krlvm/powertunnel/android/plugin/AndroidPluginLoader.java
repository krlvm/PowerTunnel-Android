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

package io.github.krlvm.powertunnel.android.plugin;

import android.content.Context;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import dalvik.system.DexClassLoader;
import io.github.krlvm.powertunnel.android.managers.PluginManager;
import io.github.krlvm.powertunnel.plugin.PluginInjector;
import io.github.krlvm.powertunnel.plugin.PluginLoader;

public class AndroidPluginLoader implements PluginInjector {

    private final String optimizedDirectory;

    public AndroidPluginLoader(Context context) {
        final File file = new File(getPluginsDir(context), "dex");
        if(!file.exists()) file.mkdir();
        this.optimizedDirectory = file.getAbsolutePath();
    }

    @Override
    public Class<?> inject(File file, String mainClass) throws Exception {
        final DexClassLoader loader = new DexClassLoader(
                file.getPath(),
                optimizedDirectory,
                null,
                AndroidPluginLoader.class.getClassLoader()
        );
        return loader.loadClass(mainClass);
    }

    public static File getPluginsDir(Context context) {
        final File dir = new File(context.getFilesDir(), "plugins");
        if(!dir.exists()) dir.mkdir();
        return dir;
    }

    public static File[] enumeratePlugins(Context context) {
        return PluginLoader.enumeratePlugins(getPluginsDir(context));
    }

    public static File[] enumerateEnabledPlugins(Context context) {
        final Set<String> disabled = PluginManager.getDisabledPlugins(context);
        final Set<File> enabled = new HashSet<>();
        for (File file : PluginLoader.enumeratePlugins(getPluginsDir(context))) {
            if(!disabled.contains(file.getName())) enabled.add(file);
        }
        return enabled.toArray(new File[0]);
    }

    public static void deleteOatCache(Context context) {
        final File dir = new File(AndroidPluginLoader.getPluginsDir(context), "oat");
        if(dir.exists()) dir.delete();
    }

    public static void deleteDexCache(Context context) {
        final File dir = new File(AndroidPluginLoader.getPluginsDir(context), "dex");
        if(dir.exists()) dir.delete();
    }
}
