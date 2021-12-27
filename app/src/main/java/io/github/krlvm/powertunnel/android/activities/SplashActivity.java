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

package io.github.krlvm.powertunnel.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import io.github.krlvm.powertunnel.android.R;
import io.github.krlvm.powertunnel.android.managers.PluginManager;
import io.github.krlvm.powertunnel.android.utility.Utility;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_activity);

        Utility.applyTheme(this);
        PluginManager.extract(this);

        new Handler().postDelayed(() -> {
            startActivity(new Intent(this, getTargetActivity()));
            finish();
            overridePendingTransition(0, R.anim.fade_out);
        }, 300);
    }

    private Class<?> getTargetActivity() {
        return PreferenceManager.getDefaultSharedPreferences(this).contains("crash_message") ?
                CrashActivity.class : MainActivity.class;
    }
}