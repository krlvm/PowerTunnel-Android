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
