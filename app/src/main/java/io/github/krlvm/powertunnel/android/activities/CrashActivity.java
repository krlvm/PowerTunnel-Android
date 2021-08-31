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
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import io.github.krlvm.powertunnel.BuildConstants;
import io.github.krlvm.powertunnel.android.BuildConfig;
import io.github.krlvm.powertunnel.android.databinding.CrashActivityBinding;
import io.github.krlvm.powertunnel.android.utility.Utility;

public class CrashActivity extends AppCompatActivity {

    private CrashActivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = CrashActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final String message = prefs.getString("crash_message", "");
        final String stacktrace = prefs.getString("crash_stacktrace", "");
        prefs.edit().remove("crash_message").remove("crash_stacktrace").apply();

        binding.crashAppVersion.setText(BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")");
        binding.crashCoreVersion.setText(BuildConstants.VERSION + " (" + BuildConstants.VERSION_CODE + ")");
        binding.crashOs.setText(Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        binding.crashMessage.setText(message);

        binding.crashStacktrace.setText(stacktrace);

        binding.reportButton.setOnClickListener(v ->
                Utility.launchUri(this, "https://github.com/krlvm/PowerTunnel-Android/issues"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}