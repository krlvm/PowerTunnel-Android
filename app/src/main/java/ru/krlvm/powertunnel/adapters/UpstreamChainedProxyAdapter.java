package ru.krlvm.powertunnel.adapters;

import org.littleshoot.proxy.ChainedProxyAdapter;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import ru.krlvm.powertunnel.PowerTunnel;

public class UpstreamChainedProxyAdapter extends ChainedProxyAdapter {

    private InetSocketAddress address;

    public UpstreamChainedProxyAdapter() {
        this(null);
    }

    public UpstreamChainedProxyAdapter(InetSocketAddress address) {
        this.address = address;
    }

    @Override
    public InetSocketAddress getChainedProxyAddress() {
        try {
            return address != null ? address : PowerTunnel.resolveUpstreamProxyAddress();
        } catch (UnknownHostException ex) {
            System.out.println("[x] Failed to resolve upstream proxy address: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    @Override
    public void filterRequest(HttpObject httpObject) {
        if (httpObject instanceof HttpRequest && PowerTunnel.UPSTREAM_PROXY_AUTH_CODE != null) {
            ((HttpRequest) httpObject).headers().add("Proxy-Authorization", "Basic " + PowerTunnel.UPSTREAM_PROXY_AUTH_CODE);
        }
    }
}
