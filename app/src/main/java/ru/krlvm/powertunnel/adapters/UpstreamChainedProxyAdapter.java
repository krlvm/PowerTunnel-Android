package ru.krlvm.powertunnel.adapters;

import org.littleshoot.proxy.ChainedProxyAdapter;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;

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
}
