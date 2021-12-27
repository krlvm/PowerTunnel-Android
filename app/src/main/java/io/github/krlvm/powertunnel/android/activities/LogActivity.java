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
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.github.krlvm.powertunnel.android.R;
import io.github.krlvm.powertunnel.android.adapters.LogLineAdapter;
import io.github.krlvm.powertunnel.android.utility.FileUtility;

public class LogActivity extends AppCompatActivity {

    private static final String LOG_TAG = "LogReader";

    private static final int REQUEST_SAVE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.log_activity);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        updateLogs();
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_log, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == android.R.id.home) {
            super.onBackPressed();
        } else if(id == R.id.action_refresh) {
            updateLogs();
        } else if(id == R.id.action_save) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                Toast.makeText(this, R.string.toast_logs_save_os_unsupported, Toast.LENGTH_LONG).show();
            } else {
                startActivityForResult(
                        new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                                .setType("text/plain").putExtra(Intent.EXTRA_TITLE, "powertunnel.log"),
                        REQUEST_SAVE);
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_SAVE || resultCode != RESULT_OK) return;
        collectLogs((lines) -> {
            if(lines == null) {
                Toast.makeText(this, R.string.toast_logs_error, Toast.LENGTH_LONG).show();
                return;
            }
            int toastStringRes;
            try {
                final InputStream in = new ByteArrayInputStream(TextUtils.join("\n", lines).getBytes(StandardCharsets.UTF_8));
                FileUtility.copy(in, getContentResolver().openOutputStream(data.getData()));
                toastStringRes = R.string.toast_logs_saved;
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Failed to save log file: " + ex.getMessage(), ex);
                toastStringRes = R.string.toast_logs_save_failed;
            }
            final int pToastStringRes = toastStringRes;
            runOnUiThread(() -> Toast.makeText(this, pToastStringRes, Toast.LENGTH_SHORT).show());
        });
    }

    private void updateLogs() {
        final View progressCircle = findViewById(R.id.progress_circular);
        final RecyclerView listView = findViewById(R.id.log_lines);

        progressCircle.setVisibility(View.VISIBLE);

        collectLogs((lines) -> {
            if(lines == null) {
                Toast.makeText(this, R.string.toast_logs_error, Toast.LENGTH_LONG).show();
                return;
            }
            runOnUiThread(() -> {
                progressCircle.setVisibility(View.GONE);
                listView.setLayoutManager(new LinearLayoutManager(this));
                listView.setAdapter(new LogLineAdapter(this, lines));
                listView.scrollToPosition(lines.size() - 1);
            });
        });
    }

    private static void collectLogs(Consumer<List<String>> consumer) {
        new Thread(() -> {
            Log.d(LOG_TAG, "Calling logcat...");
            final Process process;
            try {
                process = Runtime.getRuntime().exec("logcat *:I -d");
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Failed to call logcat: " + ex.getMessage(), ex);
                consumer.accept(null);
                return;
            }

            final List<String> lines = new ArrayList<>();
            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if(isUseless(line)) continue;
                    lines.add(line);
                }
                consumer.accept(lines);
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Failed to call logcat: " + ex.getMessage(), ex);
                consumer.accept(null);
            }
            Log.d(LOG_TAG, "Logs has been collected");
        }, "Log Collector").start();
    }

    private static boolean isUseless(String line) {
        return line.contains("nel.android.de") || line.contains("ViewPostIme");
    }
}