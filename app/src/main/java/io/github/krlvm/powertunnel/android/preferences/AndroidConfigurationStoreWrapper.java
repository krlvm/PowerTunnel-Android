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

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.preference.PreferenceDataStore;

import java.io.File;
import java.io.IOException;

import io.github.krlvm.powertunnel.android.R;
import io.github.krlvm.powertunnel.android.managers.ConfigurationManager;
import io.github.krlvm.powertunnel.configuration.ConfigurationStore;
import io.github.krlvm.powertunnel.sdk.configuration.Configuration;

public class AndroidConfigurationStoreWrapper extends PreferenceDataStore {

    private final String LOG_TAG = "PrefWrapper";

    private final File file;
    private final ConfigurationStore store = new ConfigurationStore();
    private final Consumer<Integer> errorConsumer;

    public AndroidConfigurationStoreWrapper(Context context, String filename, Consumer<Integer> errorConsumer) {
        this.errorConsumer = errorConsumer;
        this.file = new File(ConfigurationManager.getConfigsDirectory(context), filename + Configuration.EXTENSION);
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Failed to create ConfigurationStore ('" + file.getName() + "'): " + ex.getMessage(), ex);
                errorConsumer.accept(R.string.plugin_settings_failed_to_create);
                return;
            }
        }
        try {
            this.store.read(file);
        } catch (IOException ex) {
            Log.e(LOG_TAG, "Failed to read ConfigurationStore ('" + file.getName() + "'): " + ex.getMessage(), ex);
            errorConsumer.accept(R.string.plugin_settings_failed_to_read);
        }
    }

    private void save() {
        try {
            this.store.save(file);
        } catch (IOException ex) {
            Log.e(LOG_TAG, "Failed save ConfigurationStore ('" + file.getName() + "'): " + ex.getMessage(), ex);
            errorConsumer.accept(R.string.plugin_settings_failed_to_save);
        }
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        return store.get(key, defValue);
    }

    @Override
    public void putString(String key, @Nullable String value) {
        store.set(key, value);
        save();
    }

    @Override
    public int getInt(String key, int defValue) {
        return store.getInt(key, defValue);
    }

    @Override
    public void putInt(String key, int value) {
        store.setInt(key, value);
        save();
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return store.getBoolean(key, defValue);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        store.setBoolean(key, value);
        save();
    }
}
