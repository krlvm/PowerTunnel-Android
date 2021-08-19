package ru.krlvm.powertunnel.android.updater;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ru.krlvm.powertunnel.android.R;

public class Updater {

    public  static final String NOTIFICATION_CHANNEL = "Update Notifier";
    private static final String LOG_TAG = "Updater";
    private static final String UPDATE_URL = "https://raw.githubusercontent.com/krlvm/PowerTunnel-Android/master/new_version.txt";
    private static final String COMMON_CHANGELOG_URL = "https://raw.githubusercontent.com/krlvm/PowerTunnel-Android/master/CHANGELOG";
    private static final String CHANGELOG_URL = "https://raw.githubusercontent.com/krlvm/PowerTunnel-Android/master/fastlane/metadata/android/en-US/changelogs/%s.txt";
    private static final String DOWNLOAD_URL = "https://github.com/krlvm/PowerTunnel-Android/releases/download/v%s/PowerTunnel.apk";

    public static void checkUpdates(UpdateHandler handler) {
        new UpdateTask().execute(handler);
    }

    public static void showUpdateDialog(Context context, UpdateInfo info) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.update_available_title)
                .setMessage(context.getString(R.string.update_available, info.getVersion()) + "\n\n" + info.getChangelog())
                .setPositiveButton(R.string.download, (dialog, id) -> {
                    dialog.cancel();
                    initiateDownload(context, info);
                }).show();
    }
    public static void initiateDownload(Context context, UpdateInfo info) {
        context.startActivity(getDownloadIntent(info));
    }
    public static Intent getDownloadIntent(UpdateInfo info) {
        return new Intent(Intent.ACTION_VIEW, Uri.parse(Updater.getDownloadUrl(info)))
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    public static String getDownloadUrl(UpdateInfo info) {
        return String.format(DOWNLOAD_URL, info.getVersion());
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

    static class UpdateTask extends AsyncTask<UpdateHandler, Void, UpdateInfo> {

        private UpdateHandler handler;

        @Override
        protected UpdateInfo doInBackground(UpdateHandler... handlers) {
            this.handler = handlers[0];
            UpdateInfo info = null;

            try {
                info = parseUpdateInfo(fetch(UPDATE_URL));
            } catch (Exception ex) {
                Log.d(LOG_TAG, "Failed to check for updates: " + ex.getMessage(), ex);
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
                    Log.d(LOG_TAG, String.format("Failed to load changelog for version '%s' (obs=%s): %s", info.getVersionCode(), info.calculateObsolescence(), ex.getMessage()), ex);
                }
            }

            return info;
        }

        @Override
        protected void onPostExecute(UpdateInfo info) {
            this.handler.handle(info);
            this.handler = null;
        }
    }

    private static String fetch(String address) throws IOException {
        LineNumberReader reader = null;
        try {
            URL url = new URL(address);
            reader = new LineNumberReader(new InputStreamReader(url.openStream()));

            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            return TextUtils.join("\n", lines);
        } finally {
            if(reader != null) reader.close();
        }
    }
}
