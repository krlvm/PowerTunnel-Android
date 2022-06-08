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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import io.github.krlvm.powertunnel.android.R;
import io.github.krlvm.powertunnel.android.managers.ConfigurationManager;
import io.github.krlvm.powertunnel.sdk.utiities.TextReader;

public class ConfigEditActivity extends AppCompatActivity {

    private static final String LOG_TAG = "ConfigEdit";
    private String fileName;

    private long tBackPressed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config_edit_activity);

        final Intent intent = getIntent();
        fileName = intent.getStringExtra("file");

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(intent.getStringExtra("plugin"));
            actionBar.setSubtitle(fileName);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        reloadContents();
    }

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - tBackPressed > 1200L) {
            tBackPressed = System.currentTimeMillis();
            Toast.makeText(this, R.string.plugins_activity_toast_back, Toast.LENGTH_SHORT).show();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_config_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == android.R.id.home) {
            super.onBackPressed();
        } else if(id == R.id.action_save) {
            save();
        }

        return super.onOptionsItemSelected(item);
    }

    private void reloadContents() {
        final View progressCircle = findViewById(R.id.progress_circular);
        progressCircle.setVisibility(View.VISIBLE);

        load((contents) -> {
            if(contents == null) {
                Toast.makeText(this, R.string.toast_config_editor_error, Toast.LENGTH_LONG).show();
                return;
            }
            runOnUiThread(() -> {
                progressCircle.setVisibility(View.GONE);
                ((EditText) findViewById(R.id.config_text)).setText(contents);
            });
        });
    }

    private void load(Consumer<String> consumer) {
        final File file = getFile();
        if (file == null) return;

        new Thread(() -> {
            final String contents;
            try {
                contents = TextReader.read(new FileInputStream(file));
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Failed to read configuration file '" + fileName + "': " + ex.getMessage(), ex);
                consumer.accept(null);
                return;
            }
            consumer.accept(contents);
            Log.d(LOG_TAG, "Successfully read configuration file: '" + fileName + "'");
        }, "Configuration Reader").start();
    }

    private void save() {
        final File file = getFile();
        if (file == null) return;

        new Thread(() -> {
            try {
                FileOutputStream out = new FileOutputStream(file);
                try {
                    out.write(((EditText) findViewById(R.id.config_text)).getText().toString().getBytes());
                } finally {
                    out.close();
                }
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Failed to save configuration file '" + fileName + "': " + ex.getMessage(), ex);
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.toast_config_editor_saved_error, Toast.LENGTH_LONG).show();
                });
                return;
            }
            runOnUiThread(() -> {
                Toast.makeText(this, R.string.toast_config_editor_saved, Toast.LENGTH_LONG).show();
                finish();
            });
        }, "Configuration Saver").start();
    }

    private File getFile() {
        final File rootDirectory = ConfigurationManager.isUseExternalConfigs(this)
                ? ConfigurationManager.getExternalConfigsDirectory(this) : new File(getFilesDir(), "configs");
        final File file = new File(rootDirectory, fileName.replace("/", File.separator));
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                Toast.makeText(this, R.string.toast_config_init_error, Toast.LENGTH_LONG).show();
                return null;
            }
        }
        return file;
    }
}