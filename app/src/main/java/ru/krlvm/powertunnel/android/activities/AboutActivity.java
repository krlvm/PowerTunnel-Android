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

package ru.krlvm.powertunnel.android.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import ru.krlvm.powertunnel.android.BuildConfig;
import ru.krlvm.powertunnel.android.MainActivity;
import ru.krlvm.powertunnel.android.R;
import ru.krlvm.powertunnel.android.ui.NoUnderlineSpan;
import ru.krlvm.powertunnel.android.updater.Updater;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        TextView version = findViewById(R.id.about_version);
        String versionText = getString(R.string.about_version, BuildConfig.VERSION_NAME)
                + "<a href=\"https://raw.githubusercontent.com/krlvm/PowerTunnel-Android/master/LICENSE\">MIT License</a><br>";
                //+ "<a href=\"https://raw.githubusercontent.com/krlvm/PowerTunnel-Android/\">GitHub Repository</a><br>";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            version.setText(NoUnderlineSpan.stripUnderlines(Html.fromHtml(versionText, Html.FROM_HTML_MODE_COMPACT)));
        } else {
            version.setText(NoUnderlineSpan.stripUnderlines(Html.fromHtml(versionText)));
        }
        version.setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView) findViewById(R.id.about_description)).setText(R.string.description);
        ((TextView) findViewById(R.id.about_powertunnel)).setMovementMethod(LinkMovementMethod.getInstance());
        Button button = findViewById(R.id.updates_button);
        button.setOnClickListener(v -> {
            ProgressDialog progress = new ProgressDialog(AboutActivity.this);
            progress.setTitle(R.string.update_checking_title);
            progress.setMessage(getString(R.string.update_checking, BuildConfig.VERSION_NAME));
            progress.show();

            Updater.checkUpdates((info) -> {
                progress.dismiss();
                if(info != null && info.isReady()) {
                    Updater.showUpdateDialog(this, info);
                    return;
                }
                int title, message;
                if(info == null) {
                    title = R.string.update_error_title;
                    message = R.string.update_error;
                } else {
                    title = R.string.no_updates_title;
                    message = R.string.no_updates;
                }
                new AlertDialog.Builder(this)
                        .setTitle(title)
                        .setMessage(message)
                        .setCancelable(true)
                        .show();
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainActivity.applyTheme(this);
    }
}
