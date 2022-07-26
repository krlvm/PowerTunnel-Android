/*
 * This file is part of PowerTunnel-Android.
 *
 * PowerTunnel-Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PowerTunnel-Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PowerTunnel-Android.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.krlvm.powertunnel.android.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import io.github.krlvm.powertunnel.android.R;
import io.github.krlvm.powertunnel.android.databinding.MainActivityBinding;
import io.github.krlvm.powertunnel.android.managers.CertificateManager;
import io.github.krlvm.powertunnel.android.managers.ConfigurationManager;
import io.github.krlvm.powertunnel.android.services.PowerTunnelService;
import io.github.krlvm.powertunnel.android.types.GlobalStatus;
import io.github.krlvm.powertunnel.android.types.TunnelMode;
import io.github.krlvm.powertunnel.android.updater.Updater;
import io.github.krlvm.powertunnel.android.utility.AnimationHelper;
import io.github.krlvm.powertunnel.android.utility.NetworkHelper;
import io.github.krlvm.powertunnel.android.utility.NoUnderlineSpan;
import io.github.krlvm.powertunnel.android.utility.Utility;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyAddress;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "MainActivity";

    private static final int REQUEST_VPN = 1;
    private static final int REQUEST_CERT_INSTALL = 2;
    private static final int REQUEST_CERT_SAVE = 3;

    private static final int PERMISSION_STORAGE_REQUEST_CODE = 1;

    private MainActivityBinding binding;
    private BroadcastReceiver receiver;

    private boolean restartServerOnResume = false;
    private boolean unlockConfigurationWhenRunning = false;

    private MenuItem menuViewLogs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = MainActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final IntentFilter filter = new IntentFilter();
        filter.addAction(PowerTunnelService.BROADCAST_STARTED);
        filter.addAction(PowerTunnelService.BROADCAST_STOPPED);
        filter.addAction(PowerTunnelService.BROADCAST_FAILURE);
        filter.addAction(PowerTunnelService.BROADCAST_CERT);
        registerReceiver(receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null) return;
                switch (intent.getAction()) {
                    case PowerTunnelService.BROADCAST_STARTED:
                    case PowerTunnelService.BROADCAST_STOPPED: {
                        updateStatus();
                        if(restartServerOnResume) {
                            doStart();
                            restartServerOnResume = false;
                            unlockConfigurationWhenRunning = false;
                        }
                        break;
                    }
                    case PowerTunnelService.BROADCAST_FAILURE: {
                        setStatus(GlobalStatus.NOT_RUNNING);
                        if(intent.getExtras() == null) {
                            Log.w(LOG_TAG, "Startup failure broadcast has no details");
                            break;
                        }
                        final Bundle extras = intent.getExtras();
                        final TunnelMode mode = ((TunnelMode) extras.getSerializable(PowerTunnelService.EXTRAS_MODE));
                        if(mode == null) {
                            Log.w(LOG_TAG, "Startup failure broadcast extras doesn't have mode");
                            break;
                        }
                        String error = extras.getString(PowerTunnelService.EXTRAS_ERROR);
                        if(error == null) error = "unknown error";

                        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.dialog_error_failed_to_start_title)
                                .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss());
                        switch (mode) {
                            case PROXY: {
                                builder.setMessage(getString(R.string.dialog_error_failed_to_start_message_proxy, error));
                                break;
                            }
                            case VPN: {
                                if(extras.containsKey(PowerTunnelService.EXTRAS_ERROR_FW) && extras.getBoolean(PowerTunnelService.EXTRAS_ERROR_FW)) {
                                    builder.setMessage(R.string.dialog_error_failed_to_start_message_vpn_fw)
                                            .setPositiveButton(R.string.learn_more, (dialog, which) ->
                                                    Utility.launchUri(
                                                            MainActivity.this,
                                                            "https://github.com/krlvm/PowerTunnel-Android/wiki/Troubleshooting-VPN-on-Samsung-devices"
                                                    ));
                                } else {
                                    builder.setMessage(getString(R.string.dialog_error_failed_to_start_message_vpn, error));
                                }
                                break;
                            }
                        }
                        builder.create().show();
                        break;
                    }
                    case PowerTunnelService.BROADCAST_CERT: {
                        CertificateManager.checkCertificate(MainActivity.this, REQUEST_CERT_INSTALL, REQUEST_CERT_SAVE);
                        break;
                    }
                }
            }
        }, filter);

        binding.statusButton.setOnClickListener(v -> {
            if(PowerTunnelService.isRunning()) {
                unlockConfigurationWhenRunning = false;
                doStop();
            } else {
                if(!ConfigurationManager.checkStorageAccess(this)) {
                    Toast.makeText(this, R.string.toast_permission_for_external_configs, Toast.LENGTH_LONG).show();
                    ConfigurationManager.requestStorageAccess(this, PERMISSION_STORAGE_REQUEST_CODE);
                    return;
                }
                doStart();
            }
        });

        NoUnderlineSpan.stripUnderlines(binding.links);
        binding.links.setMovementMethod(LinkMovementMethod.getInstance());

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // VPN Exclusion is not available on Android < 5,
            // therefore, VPN doesn't work correctly on Android KitKat
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putString("mode", "proxy").commit();
        }

        Updater.checkUpdatesIfNecessary(this, info -> {
            if(info != null && info.isReady()) {
                Updater.showUpdateDialog(this, info);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus(false);
        checkLogsVisibility();

        final TunnelMode mode = PowerTunnelService.getTunnelMode(this);

        binding.modeDescription.setText(mode == TunnelMode.PROXY ? R.string.main_description_proxy : R.string.main_description_vpn);

        binding.proxyAddress.setVisibility(mode == TunnelMode.PROXY ? View.VISIBLE : View.GONE);
        final ProxyAddress address = PowerTunnelService.getAddress(PreferenceManager.getDefaultSharedPreferences(this));
        if (address.getHost().equals("0.0.0.0")) {
            final String privateAddress = NetworkHelper.getWiFiPrivateAddress(this);
            String text = getString(R.string.main_description_proxy_loopback,
                    address.toString(), "127.0.0.1:" + address.getPort(), privateAddress == null ? "" : privateAddress + ":" + address.getPort());
            if (privateAddress == null) {
                text = text.substring(0, text.indexOf("|"));
            } else {
                text = text.replace("|", "");
            }
            binding.proxyAddress.setText(Html.fromHtml(text));
        } else {
            binding.proxyAddress.setText(address.getHost().equals("0.0.0.0") ? "127.0.0.1:" + address.getPort() : address.toString());
        }

        if(restartServerOnResume) {
            if (!PowerTunnelService.isRunning()) {
                restartServerOnResume = false;
                unlockConfigurationWhenRunning = false;
            } else {
                Toast.makeText(this, R.string.toast_configure_while_running_restart, Toast.LENGTH_LONG).show();
                doStop();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_VPN: {
                if (resultCode == RESULT_OK) {
                    PowerTunnelService.startService(this, TunnelMode.VPN);
                } else {
                    setStatus(GlobalStatus.NOT_RUNNING, false);
                    Toast.makeText(this, R.string.toast_allow_vpn, Toast.LENGTH_LONG).show();
                }
                break;
            }
            case REQUEST_CERT_INSTALL: {
                final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                if(resultCode == RESULT_OK) {
                    editor.putBoolean("cert_installed", true);
                    Toast.makeText(this, R.string.toast_cert_installed, Toast.LENGTH_SHORT).show();
                } else {
                    editor.putBoolean("cert_installed", false);
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.dialog_cert_extract_title)
                            .setMessage(R.string.dialog_cert_extract_message)
                            .setPositiveButton(R.string.yes, (dialog, which) ->
                                    CertificateManager.extractCertificate(this, REQUEST_CERT_SAVE))
                            .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                            .show();
                }
                editor.apply();
                break;
            }
            case REQUEST_CERT_SAVE: {
                if (resultCode != RESULT_OK) return;
                CertificateManager.extractCertificate(this, data.getData());
                break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_STORAGE_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    doStart();
                }
                break;
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        menuViewLogs = menu.findItem(R.id.action_logs);
        checkLogsVisibility();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        if(id == R.id.action_plugins || id == R.id.action_settings) {
            if (PowerTunnelService.isRunning()) {
                if (!unlockConfigurationWhenRunning) {
                    Toast.makeText(this, R.string.toast_configure_while_running, Toast.LENGTH_SHORT).show();
                    unlockConfigurationWhenRunning = true;
                    return true;
                }
                restartServerOnResume = true;
            }
            if (id == R.id.action_plugins) {
                startActivity(new Intent(this, PluginsActivity.class));
            } else {
                startActivity(new Intent(this, SettingsActivity.class));
            }
        } else if(id == R.id.action_logs) {
            startActivity(new Intent(this, LogActivity.class));
        } else if(id == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void doStart() {
        startTunnel();
        setStatus(GlobalStatus.STARTING);
    }
    private void doStop() {
        stopTunnel();
        setStatus(GlobalStatus.STOPPING);
    }

    private void startTunnel() {
        final TunnelMode mode = PowerTunnelService.getTunnelMode(this);
        if(mode == TunnelMode.VPN) {
            final Intent intent = VpnService.prepare(this);
            if (intent != null) {
                startActivityForResult(intent, REQUEST_VPN);
            } else {
                onActivityResult(REQUEST_VPN, RESULT_OK, null);
            }
        } else {
            PowerTunnelService.startService(this, TunnelMode.PROXY);
        }
    }

    private void stopTunnel() {
        PowerTunnelService.stopTunnel(this);
    }

    // region User Interface

    private void checkLogsVisibility() {
        if(menuViewLogs == null) return;
        menuViewLogs.setVisible(PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("collect_logs", false));
    }

    private void updateStatus() {
        setStatus(PowerTunnelService.getStatus(), true);
    }
    private void updateStatus(boolean animate) {
        setStatus(PowerTunnelService.getStatus(), animate);
    }

    private final Runnable onAnimationEnd = () -> {
        setProgressVisibility(false);
        setStatus(PowerTunnelService.getStatus(), false);
    };
    private void setStatus(GlobalStatus status) {
        setStatus(status, true);
    }
    private void setStatus(GlobalStatus status, boolean animate) {
        final @StringRes int buttonText;
        final boolean buttonEnabled = status != GlobalStatus.STARTING && status != GlobalStatus.STOPPING;
        final TunnelMode mode = status.getMode() != null ? status.getMode() : PowerTunnelService.getTunnelMode(this);
        switch (status) {
            case STARTING: {
                buttonText = mode == TunnelMode.VPN ? R.string.action_connecting : R.string.action_starting;
                if(!animate) binding.logo.setImageResource(R.drawable.ic_logo_disabled);
                setProgressVisibility(true);
                break;
            }
            case VPN:
            case PROXY: {
                buttonText = mode == TunnelMode.VPN ? R.string.action_disconnect : R.string.action_stop;
                if(animate) {
                    AnimationHelper.animate(
                            binding.logo,
                            new int[]{R.drawable.ic_logo_disabled, R.drawable.ic_logo},
                            onAnimationEnd
                    );
                } else {
                    binding.logo.setImageResource(R.drawable.ic_logo);
                    setProgressVisibility(false);
                }
                binding.status.setText(PowerTunnelService.getStatus() == GlobalStatus.VPN ?
                        R.string.status_running_vpn : R.string.status_running_proxy);
                break;
            }
            case STOPPING: {
                buttonText = mode == TunnelMode.VPN ? R.string.action_disconnecting : R.string.action_stopping;
                if(!animate) binding.logo.setImageResource(R.drawable.ic_logo);
                setProgressVisibility(true);
                break;
            }
            case NOT_RUNNING: {
                buttonText = mode == TunnelMode.VPN ? R.string.action_connect : R.string.action_start;
                if(animate) {
                    AnimationHelper.animate(
                            binding.logo,
                            new int[]{R.drawable.ic_logo, R.drawable.ic_logo_disabled},
                            onAnimationEnd
                    );
                } else {
                    binding.logo.setImageResource(R.drawable.ic_logo_disabled);
                    setProgressVisibility(false);
                }
                binding.status.setText(PowerTunnelService.getTunnelMode(this) == TunnelMode.VPN ?
                        R.string.status_not_running_vpn : R.string.status_not_running_proxy);
                break;
            }
            default: throw new IllegalArgumentException("Unknown status");
        }

        binding.statusButton.setText(buttonText);
        binding.statusButton.setEnabled(buttonEnabled);
    }

    private void setProgressVisibility(boolean visible) {
        // This animation looks better than default one set via adding
        // android:animateLayoutChanges="true" to ProgressBar Container
        if(binding.progressCircle.getVisibility() == View.GONE) {
            binding.progressCircle.setVisibility(View.VISIBLE);
        }
        binding.progressCircle.animate()
                .alpha(visible ? 1 : 0)
                .setDuration(visible ? 500 : 250)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        binding.progressCircle.setVisibility(visible ? View.VISIBLE : View.GONE);
                    }
                });
    }

    // endregion
}
