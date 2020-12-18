package ru.krlvm.powertunnel.adapters;

import org.littleshoot.proxy.ChainedProxyAdapter;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import ru.krlvm.powertunnel.PowerTunnel;

public class UpstreamChainedProxyAdapter extends ChainedProxyAdapter {

    private InetSocketAddress cachedAddress = null;

    public UpstreamChainedProxyAdapter() {
        if (PowerTunnel.UPSTREAM_PROXY_CACHE) {
            try {
                cachedAddress = getAddress();
            } catch (UnknownHostException ex) {
                System.out.println("[x] Failed to cache upstream proxy address: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    @Override
    public InetSocketAddress getChainedProxyAddress() {
        try {
            return cachedAddress == null ? getAddress() : cachedAddress;
        } catch (UnknownHostException ex) {
            System.out.println("[x] Failed to resolve upstream proxy address: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    private InetSocketAddress getAddress() throws UnknownHostException {
        return new InetSocketAddress(InetAddress.getByName(PowerTunnel.UPSTREAM_PROXY_IP), PowerTunnel.UPSTREAM_PROXY_PORT);
    }
}
