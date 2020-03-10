package ru.krlvm.powertunnel.android;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import ru.krlvm.powertunnel.android.updater.UpdateIntent;
import ru.krlvm.powertunnel.android.updater.Updater;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ((TextView) findViewById(R.id.about_version)).setText(getString(R.string.about_version, BuildConfig.VERSION_NAME));
        ((TextView) findViewById(R.id.about_description)).setText(R.string.description);
        ((TextView) findViewById(R.id.about_powertunnel)).setMovementMethod(LinkMovementMethod.getInstance());
        Button button = findViewById(R.id.updates_button);
        button.setText(R.string.btn_check_for_updates);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProgressDialog progress = new ProgressDialog(AboutActivity.this);
                progress.setTitle(R.string.update_checking_title);
                progress.setMessage(getString(R.string.update_checking, BuildConfig.VERSION_NAME));
                progress.show();

                Updater.checkUpdates(new UpdateIntent(progress, AboutActivity.this));
            }
        });
    }
}
