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
import android.view.View;
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

    private ImageView logo;
    private TextView status;
    private Button start;
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

    private BroadcastReceiver cBroadcastRecv = null;

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
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isRunning()) {
                    stopTunnel();
                } else {
                    startTunnel();
                }
            }
        });
        displayHelp(prefs);

        TextView copyright = findViewById(R.id.main_copyright);
        NoUnderlineSpan.stripUnderlines(copyright);
        copyright.setMovementMethod(LinkMovementMethod.getInstance());

        //Listen to startup failures
        IntentFilter filter = new IntentFilter();
        filter.addAction(STARTUP_FAIL_BROADCAST);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.startup_failed_proxy)
                        .setMessage(R.string.startup_failed_proxy_message)
                        .show();
                context.getString((R.string.startup_failed_proxy));
            }
        }, filter);

        Updater.checkUpdates(new UpdateIntent(null, MainActivity.this));

        //TODO: remove this code by July, 2020
        if(prefs.getString("dns_provider", "CLOUDFLARE").equals("SECDNS_DOH")) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("dns_provider", "CLOUDFLARE_DOH");
            editor.apply();
            editor.commit();
            AlertDialog.Builder note = new AlertDialog.Builder(this)
                    .setTitle(R.string.notification)
                    .setMessage(R.string.secdns_discounted);
            note.show();
        }
    }

    private void displayHelp(SharedPreferences prefs) {
        ((TextView) findViewById(R.id.help)).setText(isVPN(prefs) ? getString(R.string.help) :
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

        cBroadcastRecv = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {
                if(PowerTunnel.SNI_TRICK != null) {
                    installCertificate();
                }
            }
        };
        IntentFilter filter = new IntentFilter(PTManager.PROMPT_CERT);
        registerReceiver(cBroadcastRecv, filter);
    }

    boolean isRunning() {
        return PowerTunnel.isRunning();
    }

    @Override
    protected void onPause() {
        super.onPause();
        statusHandler.removeCallbacks(statusRunnable);
        unbindService(serviceConnection);
        if(cBroadcastRecv != null) {
            unregisterReceiver(cBroadcastRecv);
        }
    }

    private void configureProxyServer(SharedPreferences prefs) {
        PowerTunnel.SERVER_IP_ADDRESS = prefs.getString("proxy_ip", getString(R.string.proxy_ip));
        PowerTunnel.SERVER_PORT = Integer.parseInt(prefs.getString("proxy_port",
                getString(R.string.proxy_port)));
    }

    private void updateStatus() {
        if (service == null) {
            return;
        }
        if (isRunning()) {
            logo.setImageResource(R.mipmap.ic_enabled);
            status.setText(R.string.server_running);
            start.setText(R.string.server_stop);
        } else {
            logo.setImageResource(R.mipmap.ic_disabled);
            status.setText(R.string.server_not_running);
            start.setText(R.string.server_start);
        }
    }

    private void startTunnel() {
        final ProgressDialog dialog = progress(true);
        dialog.show();
        updateStatus();
        final boolean vpn = isVPN(PreferenceManager.getDefaultSharedPreferences(this));
        progressHandler.post(new Runnable() {
            @Override
            public void run() {
                if(vpn) {
                    startVpn();
                } else {
                    startProxy();
                }
                dialog.dismiss();
            }
        });
    }

    private void startVpn() {
        Intent i = VpnService.prepare(MainActivity.this);
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
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(R.string.please_install_cert)
                .show();
        Intent installIntent = KeyChain.createInstallIntent();
        StringBuilder cert = new StringBuilder();
        try {
            File f = new File(DATA_DIR.getAbsolutePath() + "/powertunnel-root-ca.pem");
            Scanner r = new Scanner(f);
            while (r.hasNextLine()) {
                cert.append(r.nextLine()).append("\n");
            }
            r.close();
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
        final boolean vpn = isVPN(PreferenceManager.getDefaultSharedPreferences(this));
        progressHandler.post(new Runnable() {
            @Override
            public void run() {
                if(vpn) {
                    stopVpn();
                } else {
                    stopProxy();
                }
                dialog.dismiss();
            }
        });
    }

    private void stopVpn() {
        Tun2HttpVpnService.stop(MainActivity.this);
    }

    private void stopProxy() {
        stopService(new Intent(this, ProxyModeService.class));
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
                if(resultCode == RESULT_OK) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putBoolean("cert_installed", true);
                    editor.commit();
                    Toast.makeText(this, R.string.cert_installed, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.cert_not_installed, Toast.LENGTH_LONG).show();
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

    public static boolean isVPN(SharedPreferences prefs) {
        return !prefs.getBoolean("proxy_mode", false);
    }
}
