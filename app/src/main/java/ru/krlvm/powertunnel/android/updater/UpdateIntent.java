package ru.krlvm.powertunnel.android.updater;

import android.app.ProgressDialog;
import android.content.Context;

public class UpdateIntent {

    public ProgressDialog dialog;
    public Context context;

    public UpdateIntent(ProgressDialog dialog, Context context) {
        this.dialog = dialog;
        this.context = context;
    }
}
