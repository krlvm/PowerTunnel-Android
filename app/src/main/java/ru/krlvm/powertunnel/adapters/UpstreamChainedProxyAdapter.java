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

package ru.krlvm.powertunnel.adapters;

import android.util.Log;

import org.littleshoot.proxy.ChainedProxyAdapter;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import ru.krlvm.powertunnel.PowerTunnel;

public class UpstreamChainedProxyAdapter extends ChainedProxyAdapter {

    private static final String TAG = PowerTunnel.NAME + ".UpAdapter";

    private final InetSocketAddress address;
    private String auth = null;

    public UpstreamChainedProxyAdapter() {
        this(null);
    }

    public UpstreamChainedProxyAdapter(InetSocketAddress address) {
        this.address = address;
        if(PowerTunnel.UPSTREAM_PROXY_AUTH_CODE != null) {
            this.auth = "Basic " + PowerTunnel.UPSTREAM_PROXY_AUTH_CODE;
        }
    }

    @Override
    public InetSocketAddress getChainedProxyAddress() {
        try {
            return address != null ? address : PowerTunnel.resolveUpstreamProxyAddress();
        } catch (UnknownHostException ex) {
            Log.e(TAG, "Failed to resolve upstream proxy address: " + ex.getMessage(), ex);
            return null;
        }
    }

    @Override
    public void filterRequest(HttpObject httpObject) {
        if (httpObject instanceof HttpRequest && auth != null) {
            ((HttpRequest) httpObject).headers().add("Proxy-Authorization", auth);
        }
    }
}
