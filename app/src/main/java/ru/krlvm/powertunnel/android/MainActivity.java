package ru.krlvm.powertunnel.android;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toolbar;

import ru.krlvm.powertunnel.PowerTunnel;
import tun.proxy.SimplePreferenceActivity;
import tun.proxy.service.Tun2HttpVpnService;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_VPN = 1;

    private ImageView logo;
    private Button start;
    private MenuItem settings;
    private Handler statusHandler = new Handler();
    private Handler progressHandler = new Handler();

    private Tun2HttpVpnService service;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Tun2HttpVpnService.ServiceBinder serviceBinder = (Tun2HttpVpnService.ServiceBinder) binder;
            service = serviceBinder.getService();
        }
        public void onServiceDisconnected(ComponentName className) {
            service = null;
        }
    };
    Runnable statusRunnable = new Runnable() {
        @Override
        public void run() {
            updateStatus();
            statusHandler.post(statusRunnable);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logo = findViewById(R.id.status_logo);
        start = findViewById(R.id.start_button);
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRunning()) {
                    stopVpn();
                } else {
                    startVpn();
                }
            }
        });
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        settings = menu.findItem(R.id.action_activity_settings);
        settings.setEnabled(start.isEnabled());
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_activity_settings: {
                Intent intent = new Intent(this, SimplePreferenceActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.action_about: {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.app_name) + " v" + getVersionName())
                        .setMessage(getString(R.string.description) + "\n\n" +
                                "This is a very early preview version and can be unstable\n\n" +
                                "(c) krlvm, 2019-2020").show();
                break;
            }
        }
        return true;
    }

    private String getVersionName() {
        PackageManager packageManager = getPackageManager();
        if (packageManager == null) {
            return null;
        }
        try {
            return packageManager.getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "Unknown";
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        statusHandler.post(statusRunnable);
        Intent intent = new Intent(this, Tun2HttpVpnService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    boolean isRunning() {
        return service != null && service.isRunning();
    }

    @Override
    protected void onPause() {
        super.onPause();
        statusHandler.removeCallbacks(statusRunnable);
        unbindService(serviceConnection);
    }

    void updateStatus() {
        if (service == null) {
            return;
        }
        if (isRunning()) {
            logo.setImageResource(R.mipmap.ic_launcher_round);
            start.setText(R.string.server_stop);
            settings.setEnabled(false);
        } else {
            logo.setImageResource(R.mipmap.ic_disabled);
            start.setText(R.string.server_start);
            settings.setEnabled(true);
        }
    }

    private void stopVpn() {
        final ProgressDialog dialog = progress(false);
        dialog.show();
        updateStatus();
        progressHandler.post(new Runnable() {
            @Override
            public void run() {
                Tun2HttpVpnService.stop(MainActivity.this);
                dialog.dismiss();
            }
        });
    }

    private void startVpn() {
        final ProgressDialog dialog = progress(true);
        dialog.show();
        updateStatus();
        progressHandler.post(new Runnable() {
            @Override
            public void run() {
                Intent i = VpnService.prepare(MainActivity.this);
                if (i != null) {
                    startActivityForResult(i, REQUEST_VPN);
                } else {
                    onActivityResult(REQUEST_VPN, RESULT_OK, null);
                }
                dialog.dismiss();
            }
        });
    }

    private ProgressDialog progress(boolean starting) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle(starting ? R.string.starting_server : R.string.stopping_server);
        dialog.setMessage(getString(R.string.status_please_wait));
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCancelable(false);
        return dialog;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_VPN) {
            updateStatus();
            Tun2HttpVpnService.start(this);
        }
    }
}
