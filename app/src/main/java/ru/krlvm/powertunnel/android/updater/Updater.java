package ru.krlvm.powertunnel.android.updater;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;

import ru.krlvm.powertunnel.android.BuildConfig;
import ru.krlvm.powertunnel.android.R;
import tun.proxy.MyApplication;

public class Updater {

    private static String[] pendingUpdate;

    private static String load() throws IOException {
        URL url = new URL("https://raw.githubusercontent.com/krlvm/PowerTunnel-Android/master/version.txt");
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(url.openStream()));
        String string = reader.readLine();
        reader.close();
        return string;
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
        if(pendingUpdate != null && currentVerCode < Integer.parseInt(pendingUpdate[0])) {
            title = R.string.update_available_title;
            message = R.string.update_available;
            updatesFound = true;
        }
        if(intent.dialog == null && !updatesFound) {
            return;
        }
        String messageString = null;
        if(message == R.string.update_available) {
            messageString = MyApplication.getInstance().getString(R.string.update_available, pendingUpdate[1]);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(intent.context);
        builder.setTitle(title)
            .setCancelable(false)
            .setNegativeButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        if(pendingUpdate != null) {
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/krlvm/PowerTunnel/releases/download/v" + pendingUpdate[1] + "/PowerTunnel.apk"));
                            MyApplication.getInstance().startActivity(browserIntent);
                        }
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

    public static class UpdateRequest extends AsyncTask<UpdateIntent, String, String> {

        private UpdateIntent intent;

        @Override
        protected String doInBackground(UpdateIntent... intents) {
            intent = intents[0];
            boolean result = true;
            try {
                String response = load();
                String[] data = response.split(";");
                if (data.length != 2) {
                    result = false;
                } else {
                    try {
                        Integer.parseInt(data[0]);
                    } catch (NumberFormatException ex) {
                        result = false;
                    }
                }
                if(result) {
                    pendingUpdate = data;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "OK";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            continueUpdating(intent);
        }
    }
}