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

package io.github.krlvm.powertunnel.android.preferences;

import android.app.Activity;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.function.Consumer;

import io.github.krlvm.powertunnel.exceptions.PreferenceParseException;
import io.github.krlvm.powertunnel.i18n.I18NBundle;
import io.github.krlvm.powertunnel.plugin.PluginLoader;
import io.github.krlvm.powertunnel.preferences.PreferenceGroup;
import io.github.krlvm.powertunnel.preferences.PreferenceParser;
import io.github.krlvm.powertunnel.sdk.plugin.PluginInfo;
import io.github.krlvm.powertunnel.sdk.utiities.TextReader;
import io.github.krlvm.powertunnel.utilities.JarLoader;

public class AndroidPluginPreferenceParser {

    private static final String LOG_TAG = "PrefParser";

    public static void loadPreferences(Activity context, PluginInfo pluginInfo, Consumer<List<PreferenceGroup>> consumer) {
        try(final JarLoader loader = new JarLoader(PluginLoader.getPluginFile(context.getFilesDir(), pluginInfo.getSource()))) {
            loader.open(PreferenceParser.FILE, (in) -> {
                if(in == null) {
                    consumer.accept(null);
                    return;
                }
                getJarLocaleBundleInputStream(loader, (_in) -> {
                    final PropertyResourceBundle bundle;
                    if(_in != null) {
                        try {
                            bundle = new PropertyResourceBundle(new InputStreamReader(_in, StandardCharsets.UTF_8));
                        } catch (IOException ex) {
                            Log.e(LOG_TAG, String.format("Failed to read '%s' locale: %s", pluginInfo.getId(), ex.getMessage()), ex);
                            return;
                        }
                    } else {
                        bundle = null;
                    }
                    consumer.accept(loadPreferences(pluginInfo, in, new I18NBundle(bundle)));
                });
            }, true);
        } catch (IOException ex) {
            Log.e(LOG_TAG, String.format("Failed to open plugin '%s' jar file: %s", pluginInfo.getId(), ex.getMessage()), ex);
        }
    }

    public static List<PreferenceGroup> loadPreferences(PluginInfo pluginInfo, InputStream in, I18NBundle bundle) {
        final String json;

        try {
            json = TextReader.read(in);
        } catch (IOException ex) {
            Log.e(LOG_TAG, "Failed to read schema of '" + pluginInfo.getSource() + "': " + ex.getMessage(), ex);
            return null;
        }

        final List<PreferenceGroup> preferences;
        try {
            preferences = PreferenceParser.parsePreferences(pluginInfo.getSource(), json, bundle);
        } catch (PreferenceParseException ex) {
            Log.e(LOG_TAG, "Failed to parse preferences of '" + pluginInfo.getSource() + "': " + ex.getMessage(), ex);
            return null;
        }

        if(preferences.isEmpty()) return null;
        return preferences;
    }

    private static void getJarLocaleBundleInputStream(JarLoader loader, Consumer<InputStream> consumer) throws IOException {
        loader.open(I18NBundle.getLocaleFilePath(Locale.getDefault().getLanguage()), (in) -> {
            if(in == null) {
                loader.open(I18NBundle.getLocaleFilePath(null), consumer::accept, true);
            } else {
                consumer.accept(in);
            }
        }, true);
    }
}
