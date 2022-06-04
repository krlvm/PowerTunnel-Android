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

package io.github.krlvm.powertunnel.android.managers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.core.util.Consumer;
import androidx.preference.PreferenceManager;

import org.jetbrains.annotations.NotNull;
import org.littleshoot.proxy.mitm.CertificateHelper;

import java.io.File;
import java.util.List;
import java.util.UUID;

import io.github.krlvm.powertunnel.LittleProxyServer;
import io.github.krlvm.powertunnel.PowerTunnel;
import io.github.krlvm.powertunnel.android.BuildConfig;
import io.github.krlvm.powertunnel.android.plugin.AndroidPluginLoader;
import io.github.krlvm.powertunnel.android.services.PowerTunnelService;
import io.github.krlvm.powertunnel.android.utility.DNSUtility;
import io.github.krlvm.powertunnel.mitm.MITMAuthority;
import io.github.krlvm.powertunnel.plugin.PluginLoader;
import io.github.krlvm.powertunnel.sdk.ServerAdapter;
import io.github.krlvm.powertunnel.sdk.ServerListener;
import io.github.krlvm.powertunnel.sdk.exceptions.ProxyStartException;
import io.github.krlvm.powertunnel.sdk.plugin.PluginInfo;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyAddress;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyCredentials;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyServer;
import io.github.krlvm.powertunnel.sdk.proxy.ProxyStatus;
import io.github.krlvm.powertunnel.sdk.proxy.UpstreamProxyServer;
import io.github.krlvm.powertunnel.sdk.types.PowerTunnelPlatform;
import io.github.krlvm.powertunnel.sdk.types.UpstreamProxyType;

public class ProxyManager implements ServerListener {

    private static final String LOG_TAG = "ProxyManager";
    private static final PluginInfo PLUGIN_INFO = new PluginInfo(
            "android-app",
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            "PowerTunnel-Android",
            "Powerful and extensible proxy server",
            "krlvm",
            "https://github.com/krlvm/PowerTunnel-Android",
            new String[0],
            null,
            io.github.krlvm.powertunnel.BuildConstants.VERSION_CODE,
            null
    );

    private final Context context;

    private PowerTunnel server;
    private final ProxyAddress address;

    private final Consumer<ProxyStatus> statusListener;
    private final Consumer<String> failureListener;

    private final List<String> dnsServers;

    private boolean hostnameAvailability = true;

    public ProxyManager(Context context, Consumer<ProxyStatus> statusListener, Consumer<String> failureListener, List<String> dnsServers) {
        this.context = context;
        this.address = PowerTunnelService.getAddress(PreferenceManager.getDefaultSharedPreferences(context));
        this.statusListener = statusListener;
        this.failureListener = failureListener;
        this.dnsServers = dnsServers;
    }

    public void start() {
        if(this.server != null) {
            Log.w(LOG_TAG, "Attempted to start server when it is already running");
            return;
        }
        statusListener.accept(ProxyStatus.STARTING);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        this.server = new PowerTunnel(
                address,
                PowerTunnelPlatform.ANDROID,
                context.getFilesDir(),
                prefs.getBoolean("transparent_mode", true),
                !prefs.getBoolean("strict_dns", false),
                dnsServers,
                DNSUtility.getDNSDomainsSearchPath(context),
                MITMAuthority.create(
                        new File(context.getFilesDir(), "cert"),
                        getMitmCertificatePassword().toCharArray()
                ),
                ConfigurationManager.isUseExternalConfigs(context)
                        ? ConfigurationManager.getExternalConfigsDirectory(context) : null,
                null
        );

        new Thread(() -> {
            Log.d(LOG_TAG, "Starting server...");
            this.server.registerServerListener(PLUGIN_INFO, this);
            try {
                PluginLoader.loadPlugins(
                        AndroidPluginLoader.enumerateEnabledPlugins(context),
                        this.server, new AndroidPluginLoader(context)
                );
                this.server.start();
                Log.d(LOG_TAG, "Server started");
                statusListener.accept(ProxyStatus.RUNNING);
                this.server.registerServerListener(PLUGIN_INFO, new ServerAdapter() {
                    @Override
                    public void onProxyStatusChanged(@NotNull ProxyStatus status) {
                        statusListener.accept(status);
                    }
                });
            } catch (ProxyStartException ex) {
                Log.e(LOG_TAG, "Failed to start server: " + ex.getMessage(), ex);
                failureListener.accept(ex.getMessage());
            }
        }, "Proxy Bootstrap").start();
    }

    public void stop() {
        stop(true);
    }
    public void stop(boolean graceful) {
        if(this.server == null) {
            Log.w(LOG_TAG, "Attempted to stop server when it is not running");
            return;
        }
        if (!this.server.isRunning()) return;

        new Thread(() -> {
            Log.i(LOG_TAG, "Stopping server...");
            this.server.stop(graceful);
            Log.i(LOG_TAG, "Server has been stopped");
        }, "Proxy Shutdown").start();
    }

    public ProxyStatus getStatus() {
        return server.getStatus();
    }

    public boolean isRunning() {
        return getStatus() == ProxyStatus.RUNNING;
    }

    @Override
    public void beforeProxyStatusChanged(@NotNull ProxyStatus status) {
        if(status == ProxyStatus.STARTING) {
            CertificateHelper.ANDROID_P_DISABLE_PROVIDER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;

            final ProxyServer proxy = server.getProxyServer();
            assert proxy != null;

            if (proxy instanceof LittleProxyServer) {
                ((LittleProxyServer) proxy).setHostnamesAvailability(hostnameAvailability);
            }

            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

            if (prefs.getBoolean("upstream_proxy_enabled", false)) {
                ProxyCredentials credentials = null;
                if (prefs.getBoolean("upstream_proxy_auth_enabled", false)) {
                    credentials = new ProxyCredentials(
                            prefs.getString("upstream_proxy_auth_username", ""),
                            prefs.getString("upstream_proxy_auth_password", "")
                    );
                }
                proxy.setUpstreamProxyServer(new UpstreamProxyServer(
                        new ProxyAddress(
                                prefs.getString("upstream_proxy_host", ""),
                                Integer.parseInt(prefs.getString("upstream_proxy_port", "80"))
                        ),
                        credentials,
                        UpstreamProxyType.valueOf(prefs.getString("upstream_proxy_protocol", "http").toUpperCase())
                ));
            }

            if (prefs.getBoolean("proxy_auth_enabled", false)) {
                proxy.setAuthorizationCredentials(new ProxyCredentials(
                        prefs.getString("proxy_auth_username", ""),
                        prefs.getString("proxy_auth_password", "")
                ));
            }

            proxy.setAllowRequestsToOriginServer(prefs.getBoolean("allow_requests_to_origin_server", true));
        } else if(status == ProxyStatus.RUNNING) {
            if(server.getProxyServer().isMITMEnabled()) {
                context.sendBroadcast(new Intent(PowerTunnelService.BROADCAST_CERT));
            }
        }
    }

    @Override
    public void onProxyStatusChanged(@NotNull ProxyStatus status) {}

    public void setHostnamesAvailability(boolean availability) {
        hostnameAvailability = availability;
    }

    public ProxyAddress getAddress() {
        return server.getProxyServer().getAddress();
    }

    private String getMitmCertificatePassword() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(!prefs.contains("cert_password")) {
            final String password = UUID.randomUUID().toString();
            prefs.edit().putString("cert_password", password).apply();
            return password;
        }
        return prefs.getString("cert_password", null);
    }
}
