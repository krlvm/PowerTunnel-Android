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
        String versionText = getString(R.string.about_version, BuildConfig.VERSION_NAME);
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
