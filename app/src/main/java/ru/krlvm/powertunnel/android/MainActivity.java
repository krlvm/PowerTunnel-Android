package ru.krlvm.powertunnel.android;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.security.KeyChain;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.util.Scanner;

import ru.krlvm.powertunnel.PowerTunnel;
import ru.krlvm.powertunnel.android.activities.AboutActivity;
import ru.krlvm.powertunnel.android.managers.PTManager;
import ru.krlvm.powertunnel.android.service.ProxyModeService;
import ru.krlvm.powertunnel.android.ui.NoUnderlineSpan;
import ru.krlvm.powertunnel.android.updater.UpdateIntent;
import ru.krlvm.powertunnel.android.updater.Updater;
import tun.proxy.preferences.SimplePreferenceActivity;
import tun.proxy.service.Tun2HttpVpnService;

public class MainActivity extends AppCompatActivity {

    public static File DATA_DIR;

    public static final int REQUEST_VPN = 1;
    private static final int REQUEST_CERT = 2;

    public static final String STARTUP_FAIL_BROADCAST = "ru.krlvm.powertunnel.android.action.STARTUP_FAIL";
    public static final String SERVER_START_BROADCAST = "ru.krlvm.powertunnel.android.action.SERVER_START";
    public static final String SERVER_STOP_BROADCAST = "ru.krlvm.powertunnel.android.action.SERVER_STOP";

    private ImageView logo;
    private TextView status;
    private Button start;
    private Handler statusHandler = new Handler();
    private Handler progressHandler = new Handler();

    private Tun2HttpVpnService service;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Tun2HttpVpnService.ServiceBinder serviceBinder = (Tun2HttpVpnService.ServiceBinder) binder;
            service = serviceBinder.getService();
        }
        @Override
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

    private BroadcastReceiver statusReceiver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DATA_DIR = getFilesDir();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        configureProxyServer(prefs);
        applyTheme(prefs);

        logo = findViewById(R.id.status_logo);
        status = findViewById(R.id.status);
        start = findViewById(R.id.start_button);
        start.setOnClickListener(v -> {
            if(isRunning()) {
                stopTunnel();
            } else {
                startTunnel();
            }
        });
        displayHelp(prefs);

        TextView copyright = findViewById(R.id.main_copyright);
        NoUnderlineSpan.stripUnderlines(copyright);
        copyright.setMovementMethod(LinkMovementMethod.getInstance());

        // Listen to startup failures
        IntentFilter filter = new IntentFilter();
        filter.addAction(STARTUP_FAIL_BROADCAST);
        filter.addAction(SERVER_START_BROADCAST);
        registerReceiver(statusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent == null || intent.getAction() == null) {
                    return;
                }
                switch (intent.getAction()) {
                    case STARTUP_FAIL_BROADCAST: {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.startup_failed_proxy)
                                .setMessage(getString(R.string.startup_failed_proxy_message, intent.getStringExtra("cause")))
                                .show();
                        context.getString((R.string.startup_failed_proxy));
                        break;
                    }
                    case SERVER_START_BROADCAST: {
                        if(PowerTunnel.SNI_TRICK != null) {
                            installCertificate();
                        }
                        break;
                    }
                }
            }
        }, filter);

        Updater.checkUpdates(new UpdateIntent(null, MainActivity.this));
    }

    private void displayHelp(SharedPreferences prefs) {
        ((TextView) findViewById(R.id.help)).setText(PTManager.isVPN(prefs) ? getString(R.string.help) :
                getString(R.string.help_proxy, (PowerTunnel.SERVER_IP_ADDRESS + ":" + PowerTunnel.SERVER_PORT)));
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
                if(isRunning()) {
                    Toast.makeText(this, R.string.stop_server_to_edit_settings, Toast.LENGTH_SHORT).show();
                    break;
                }
                Intent intent = new Intent(this, SimplePreferenceActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.action_about: {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                break;
            }
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        configureProxyServer(PreferenceManager.getDefaultSharedPreferences(this));
        applyTheme(this);
        displayHelp(PreferenceManager.getDefaultSharedPreferences(this));
        updateStatus();
        statusHandler.post(statusRunnable);
        Intent intent = new Intent(this, Tun2HttpVpnService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    boolean isRunning() {
        return PowerTunnel.isRunning();
    }

    @Override
    protected void onPause() {
        super.onPause();
        statusHandler.removeCallbacks(statusRunnable);
        unbindService(serviceConnection);
    }

    @Override
    protected void onStop() {
        if(statusReceiver != null) {
            try {
                unregisterReceiver(statusReceiver);
            } catch (IllegalArgumentException ignore) {}
        }
        super.onStop();
    }

    private void configureProxyServer(SharedPreferences prefs) {
        PowerTunnel.SERVER_IP_ADDRESS = prefs.getString("proxy_ip", getString(R.string.proxy_ip));
        try {
            PowerTunnel.SERVER_PORT = Integer.parseInt(prefs.getString("proxy_port", getString(R.string.proxy_port)));
        } catch (NumberFormatException ex) {
            String defPort = getString(R.string.proxy_port);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("proxy_port", defPort);
            editor.apply();
            PowerTunnel.SERVER_PORT = Integer.parseInt(defPort);
        }
    }

    private void updateStatus() {
        if (service == null) {
            return;
        }
        if (isRunning()) {
            logo.setImageResource(R.drawable.ic_logo_running);
            status.setText(R.string.server_running);
            start.setText(R.string.server_stop);
        } else {
            logo.setImageResource(R.drawable.ic_logo_not_running);
            status.setText(R.string.server_not_running);
            start.setText(R.string.server_start);
        }
    }

    private void startTunnel() {
        final ProgressDialog dialog = progress(true);
        dialog.show();
        updateStatus();
        final boolean vpn = PTManager.isVPN(this);
        progressHandler.post(() -> {
            if(vpn) {
                startVpn();
            } else {
                startProxy();
            }
            dialog.dismiss();
        });
    }

    private void startVpn() {
        Intent i = VpnService.prepare(this);
        if (i != null) {
            startActivityForResult(i, REQUEST_VPN);
        } else {
            onActivityResult(REQUEST_VPN, RESULT_OK, null);
        }
    }

    private void startProxy() {
        startService(new Intent(this, ProxyModeService.class));
    }

    private void installCertificate() {
        Toast.makeText(this, R.string.please_install_cert, Toast.LENGTH_LONG).show();
        Intent installIntent = KeyChain.createInstallIntent();
        StringBuilder cert = new StringBuilder();
        try {
            Scanner scanner = new Scanner(new File(DATA_DIR.getAbsolutePath() + "/powertunnel-root-ca.pem"));
            while (scanner.hasNextLine()) {
                cert.append(scanner.nextLine()).append("\n");
            }
            scanner.close();
        } catch (Exception ex) {
            return;
        }
        installIntent.putExtra(KeyChain.EXTRA_CERTIFICATE, cert.toString().getBytes());
        installIntent.putExtra(KeyChain.EXTRA_NAME, "PowerTunnel Root CA");
        startActivityForResult(installIntent, REQUEST_CERT);
    }

    private void stopTunnel() {
        final ProgressDialog dialog = progress(false);
        dialog.show();
        updateStatus();
        progressHandler.post(() -> {
            PTManager.stopTunnel(this);
            dialog.dismiss();
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
        switch (requestCode) {
            case REQUEST_VPN: {
                if (resultCode == RESULT_OK) {
                    updateStatus();
                    Tun2HttpVpnService.start(this);
                }
                break;
            }
            case REQUEST_CERT: {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                SharedPreferences.Editor editor = prefs.edit();
                if(resultCode == RESULT_OK) {
                    editor.putBoolean("cert_installed", true);
                    editor.apply();
                    Toast.makeText(this, R.string.cert_installed, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.cert_not_installed, Toast.LENGTH_LONG).show();
                    editor.putBoolean("cert_installed", false);
                    stopTunnel();
                }
                break;
            }
        }
    }

    public static void applyTheme(Context context) {
        applyTheme(PreferenceManager.getDefaultSharedPreferences(context));
    }

    public static void applyTheme(SharedPreferences prefs) {
        applyTheme(prefs.getString("theme", "AUTO"));
    }

    public static void applyTheme(String theme) {
        switch (theme) {
            case "AUTO": {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            }
            case "LIGHT": {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            }
            case "DARK": {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            }
            case "BATTERY": {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                break;
            }
        }
    }
}
