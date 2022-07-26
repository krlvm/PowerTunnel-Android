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

package io.github.krlvm.powertunnel.android;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.multidex.MultiDexApplication;
import androidx.preference.PreferenceManager;

public class PowerTunnelApplication extends MultiDexApplication {

    private static final String LOG_TAG = "Application";

    @SuppressLint("ApplySharedPref")
    @Override
    public void onCreate() {
        super.onCreate();

        final Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            Log.d(LOG_TAG, "Caught unhandled exception: " + e.getMessage());
            if (!(e instanceof RuntimeException)) {
                PreferenceManager.getDefaultSharedPreferences(this).edit()
                        .putString("crash_message", e.getMessage())
                        .putString("crash_stacktrace", Log.getStackTraceString(e))
                        .commit();
            }
            if (handler != null) {
                handler.uncaughtException(t, e);
            } else {
                System.exit(1);
            }
        });
    }
}
