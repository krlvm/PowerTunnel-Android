package ru.krlvm.powertunnel.managers;

import android.util.Log;

import org.littleshoot.proxy.ChainedProxy;
import org.littleshoot.proxy.ChainedProxyManager;

import java.net.UnknownHostException;
import java.util.Queue;

import io.netty.handler.codec.http.HttpRequest;
import ru.krlvm.powertunnel.PowerTunnel;
import ru.krlvm.powertunnel.adapters.UpstreamChainedProxyAdapter;

public class UpstreamProxyChainedProxyManager implements ChainedProxyManager {

    private static final String TAG = PowerTunnel.NAME + ".UpMan";

    private UpstreamChainedProxyAdapter adapter = null;

    public UpstreamProxyChainedProxyManager() {
        if(PowerTunnel.UPSTREAM_PROXY_CACHE) {
            try {
                adapter = new UpstreamChainedProxyAdapter(PowerTunnel.resolveUpstreamProxyAddress());
            } catch (UnknownHostException ex) {
                Log.e(TAG, "Failed to cache upstream proxy address - resolution failed: " + ex.getMessage(), ex);
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void lookupChainedProxies(HttpRequest httpRequest, Queue<ChainedProxy> queue) {
        queue.add(adapter != null ? adapter : new UpstreamChainedProxyAdapter());
    }
}
