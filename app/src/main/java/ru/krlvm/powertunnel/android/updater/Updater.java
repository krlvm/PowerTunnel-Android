package ru.krlvm.powertunnel.android.updater;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;

import ru.krlvm.powertunnel.android.BuildConfig;
import ru.krlvm.powertunnel.android.R;

public class Updater {

    private static String[] pendingUpdate;

    private static String load() throws IOException {
        URL url = new URL("https://raw.githubusercontent.com/krlvm/PowerTunnel-Android/master/new_version.txt");
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(url.openStream()));
        String string = reader.readLine();
        reader.close();
        return string;
    }

    private static String getChangelog(int version) throws IOException {
        URL url = new URL("https://raw.githubusercontent.com/krlvm/PowerTunnel-Android/master/fastlane/metadata/android/en-US/changelogs/" + version + ".txt");
        StringBuilder builder = new StringBuilder();
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(url.openStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append('\n').append(line);
        }
        reader.close();
        return builder.toString();
    }

    private static void continueUpdating(UpdateIntent intent) {
        if(intent.dialog != null) {
            if(intent.dialog.isShowing()) {
                intent.dialog.dismiss();
            } else {
                return;
            }
        }
        int currentVerCode = BuildConfig.VERSION_CODE;
        boolean updatesFound = false;

        int title = R.string.no_updates_title;
        int message = R.string.no_updates;
        if(pendingUpdate == null) {
            title = R.string.update_error_title;
            message = R.string.update_error;
        }
        final boolean ready = pendingUpdate != null && currentVerCode < Integer.parseInt(pendingUpdate[0]);
        if(ready) {
            title = R.string.update_available_title;
            message = R.string.update_available;
            updatesFound = true;
        }
        if(intent.dialog == null && !updatesFound) {
            return;
        }
        String messageString = null;
        if(message == R.string.update_available) {
            messageString = intent.context.getString(R.string.update_available, pendingUpdate[1])
                    + "\n" + pendingUpdate[2];
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(intent.context);
        builder.setTitle(title)
            .setCancelable(true)
            .setPositiveButton(ready ? R.string.download : R.string.ok,
                    (dialog, id) -> {
                        dialog.cancel();
                        if(ready) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/krlvm/PowerTunnel-Android/releases/download/v" + pendingUpdate[1] + "/PowerTunnel.apk"));
                            browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.context.startActivity(browserIntent);
                        }
                    });
        if(messageString != null) {
            builder.setMessage(messageString);
        } else {
            builder.setMessage(message);
        }
        AlertDialog alert = builder.create();
        alert.show();
    }

    public static void checkUpdates(UpdateIntent intent) {
        new UpdateRequest().execute(intent);
    }

    public static class UpdateRequest extends AsyncTask<UpdateIntent, Void, Void> {

        private UpdateIntent intent;

        @Override
        protected Void doInBackground(UpdateIntent... intents) {
            intent = intents[0];
            boolean result = true;
            int versionCode = -1;
            try {
                String response = load();
                String[] data = response.split(";");
                if (data.length != 3) {
                    result = false;
                } else {
                    try {
                        int minApiVersion = Integer.parseInt(data[2]);
                        if(android.os.Build.VERSION.SDK_INT >= minApiVersion) {
                            versionCode = Integer.parseInt(data[0]);
                        } else {
                            result = false;
                        }
                    } catch (NumberFormatException ex) {
                        result = false;
                    }
                }
                if(result) {
                    pendingUpdate = new String[3];
                    pendingUpdate[0] = data[0];
                    pendingUpdate[1] = data[1];
                    pendingUpdate[2] = getChangelog(versionCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void s) {
            super.onPostExecute(s);
            continueUpdating(intent);
        }
    }
}