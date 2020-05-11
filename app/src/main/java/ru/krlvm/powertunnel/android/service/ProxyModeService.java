package ru.krlvm.powertunnel.android.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import ru.krlvm.powertunnel.android.MainActivity;
import ru.krlvm.powertunnel.android.PTManager;
import ru.krlvm.powertunnel.android.R;

public class ProxyModeService extends Service {

    private static final String TAG = "PTProxyMode.Service";
    private static final String CHANNEL_ID = "PowerTunnelProxy";
    private static final int FOREGROUND_ID = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, getString(R.string.notification_channel_proxy),
                    NotificationManager.IMPORTANCE_MIN);
            channel.setDescription(getString(R.string.notification_channel_proxy));
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if(notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setDefaults(Notification.DEFAULT_ALL)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.notification_proxy_background))
                .setContentIntent(pendingIntent).build();

        Log.i(TAG, "Starting proxy server...");
        PTManager.configure(this, PreferenceManager.getDefaultSharedPreferences(this));

        Exception proxyFailure = PTManager.safeStartProxy();
        if(proxyFailure != null) {
            sendBroadcast(new Intent(MainActivity.STARTUP_FAIL_BROADCAST));
            stopForeground(true);
        } else {
            startForeground(FOREGROUND_ID, notification);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PTManager.stopProxy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
