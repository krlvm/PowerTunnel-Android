package ru.krlvm.powertunnel.android.services;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import ru.krlvm.powertunnel.android.R;
import ru.krlvm.powertunnel.android.managers.NotificationHelper;
import ru.krlvm.powertunnel.android.managers.PTManager;

public class ProxyModeService extends Service {

    private static final String TAG = "PTProxyMode.Service";
    private static final String CHANNEL_ID = "PowerTunnelProxy";
    private static final int FOREGROUND_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.prepareNotificationChannel(this, CHANNEL_ID);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = NotificationHelper.createMainActivityNotification(this,
                CHANNEL_ID, getString(R.string.app_name),
                getString(R.string.notification_proxy_background)
        );

        Log.i(TAG, "Starting proxy server...");
        PTManager.configure(this, PreferenceManager.getDefaultSharedPreferences(this));

        if(!PTManager.safeStartProxy(this)) {
            // failed to start
            stopForeground(true);
        } else {
            startForeground(FOREGROUND_ID, notification);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PTManager.stopProxy(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
