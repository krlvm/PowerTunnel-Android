package io.github.krlvm.powertunnel.android.updater;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import androidx.core.util.Consumer;
import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import io.github.krlvm.powertunnel.android.R;
import io.github.krlvm.powertunnel.android.utility.Utility;

public class Updater {

    private static final String LOG_TAG = "Updater";
    public static final String NOTIFICATION_CHANNEL_ID = "UPDATER";

    private static final String UPDATE_URL = "https://raw.githubusercontent.com/krlvm/PowerTunnel-Android/master/new_version.txt";
    private static final String COMMON_CHANGELOG_URL = "https://raw.githubusercontent.com/krlvm/PowerTunnel-Android/master/CHANGELOG";
    private static final String CHANGELOG_URL = "https://raw.githubusercontent.com/krlvm/PowerTunnel-Android/master/fastlane/metadata/android/en-US/changelogs/%s.txt";
    private static final String DOWNLOAD_URL = "https://github.com/krlvm/PowerTunnel-Android/releases/download/v%s/PowerTunnel.apk";

    public static void checkUpdatesIfNecessary(Context context, Consumer<UpdateInfo> handler) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (prefs.getBoolean("disable_update_notifier", false)) return;

        if(System.currentTimeMillis() - prefs.getLong("last_update_check", 0) < 24 * 60 * 60 * 1000) return;
        prefs.edit().putLong("last_update_check", System.currentTimeMillis()).apply();

        checkUpdates(handler);
    }

    public static void checkUpdates(Consumer<UpdateInfo> handler) {
        new UpdateTask().execute(handler);
    }

    public static void showUpdateDialog(Context context, UpdateInfo info) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.dialog_update_available_title)
                .setMessage(context.getString(R.string.dialog_update_available_message, info.getVersion()) + "\n\n" + info.getChangelog())
                .setPositiveButton(R.string.download, (dialog, id) -> {
                    dialog.cancel();
                    download(context, info);
                }).show();
    }

    public static void download(Context context, UpdateInfo info) {
        context.startActivity(getDownloadIntent(info));
    }

    public static Intent getDownloadIntent(UpdateInfo info) {
        return Utility.getUriIntent(String.format(DOWNLOAD_URL, info.getVersion()));
    }

    private static UpdateInfo parseUpdateInfo(String raw) {
        String[] data = raw.split(";");
        if(data.length == 3) {
            try {
                return new UpdateInfo(
                        Integer.parseInt(data[0]),
                        data[1],
                        Integer.parseInt(data[2])
                );
            } catch (NumberFormatException ignore) {}
        }

        Log.d(LOG_TAG, "Failed to parse update info, data length: " + data.length);
        return null;
    }

    static class UpdateTask extends AsyncTask<Consumer<UpdateInfo>, Void, UpdateInfo> {

        private Consumer<UpdateInfo> handler;

        @Override
        protected UpdateInfo doInBackground(Consumer<UpdateInfo>... handlers) {
            this.handler = handlers[0];
            UpdateInfo info;

            try {
                info = parseUpdateInfo(fetch(UPDATE_URL));
            } catch (Exception ex) {
                Log.d(LOG_TAG, "Failed to check for updates: " + ex.getMessage(), ex);
                return null;
            }

            if(info != null) {
                try {
                    info.setChangelog(fetch(
                            info.calculateObsolescence() > 1 ?
                                    COMMON_CHANGELOG_URL :
                                    String.format(CHANGELOG_URL, info.getVersionCode())
                            )
                    );
                } catch (Exception ex) {
                    Log.d(LOG_TAG, String.format("Failed to load changelog for version '%s': %s",
                            info.getVersionCode(), ex.getMessage()), ex);
                }
            }

            return info;
        }

        @Override
        protected void onPostExecute(UpdateInfo info) {
            this.handler.accept(info);
            this.handler = null;
        }
    }

    private static String fetch(String address) throws IOException {
        try(final BufferedReader in = new BufferedReader(new InputStreamReader(new URL(address).openStream()))) {
            final StringBuilder builder = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.substring(0, builder.lastIndexOf("\n"));
        }
    }
}
