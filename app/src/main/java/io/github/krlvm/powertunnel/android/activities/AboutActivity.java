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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import io.github.krlvm.powertunnel.BuildConstants;
import io.github.krlvm.powertunnel.android.BuildConfig;
import io.github.krlvm.powertunnel.android.R;
import io.github.krlvm.powertunnel.android.databinding.AboutActivityBinding;
import io.github.krlvm.powertunnel.android.updater.Updater;
import io.github.krlvm.powertunnel.android.utility.NoUnderlineSpan;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final AboutActivityBinding binding = AboutActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        binding.version.setText(getString(R.string.app_version,
                BuildConfig.VERSION_NAME,
                BuildConstants.VERSION,
                BuildConstants.VERSION_CODE,
                BuildConstants.SDK
        ));

        NoUnderlineSpan.stripUnderlines(binding.credits);
        binding.credits.setMovementMethod(LinkMovementMethod.getInstance());

        NoUnderlineSpan.stripUnderlines(binding.license);
        binding.license.setMovementMethod(LinkMovementMethod.getInstance());

        binding.updateButton.setOnClickListener(v -> {
            final ProgressDialog progress = new ProgressDialog(AboutActivity.this);
            progress.setTitle(R.string.dialog_update_checking_title);
            progress.setMessage(getString(R.string.dialog_update_checking_message, BuildConfig.VERSION_NAME));
            progress.show();

            Updater.checkUpdates((info) -> {
                progress.dismiss();
                if(info != null && info.isReady()) {
                    Updater.showUpdateDialog(this, info);
                    return;
                }
                int title, message;
                if(info == null) {
                    title = R.string.dialog_update_error_title;
                    message = R.string.dialog_update_error_message;
                } else {
                    title = R.string.dialog_update_no_updates_title;
                    message = R.string.dialog_update_no_updates_message;
                }
                new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(message)
                        .setCancelable(true)
                        .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                        .show();
            });
        });
        binding.updateButton.setLongClickable(true);
        binding.updateButton.setOnLongClickListener(v -> {
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            final boolean wasDisabled = prefs.getBoolean("disable_update_notifier", false);
            prefs.edit().putBoolean("disable_update_notifier", !wasDisabled).apply();
            Toast.makeText(this, "Update Notifier has been " + (wasDisabled ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == android.R.id.home) {
            super.onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }
}