package ru.krlvm.powertunnel;

import org.jitsi.dnssec.validator.ValidatingResolver;
import org.littleshoot.proxy.HostResolver;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.HttpProxyServerBootstrap;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import ru.krlvm.powertunnel.android.PTManager;
import ru.krlvm.powertunnel.android.resolver.AndroidDohResolver;
import ru.krlvm.powertunnel.filter.ProxyFilter;
import ru.krlvm.powertunnel.utilities.URLUtility;

/**
 * The PowerTunnel Android Proxy Server
 * Code is synchronized with the LibertyTunnel branch of PowerTunnel
 * The server has to be started before the VPN server start
 *
 * LibertyTunnel Bootstrap class
 *
 * LibertyTunnel: PowerTunnel with nothing extra
 *
 * This class initializes LibertyTunnel, loads government blacklist
 * and controls the LittleProxy Server
 *
 * @author krlvm
 */
public class PowerTunnel {

    public static final String NAME = "LibertyTunnel";
    public static final String VERSION = "1.0";
    public static final String BASE_VERSION = "1.10";
    public static final int VERSION_CODE = 10; //base version code
    public static final String REPOSITORY_URL = "https://github.com/krlvm/PowerTunnel/tree/libertytunnel";

    private static HttpProxyServer SERVER;
    private static boolean RUNNING = false;
    public static String SERVER_IP_ADDRESS = "127.0.0.1";
    public static int SERVER_PORT = 8085;

    public static boolean FULL_CHUNKING = false;
    public static int DEFAULT_CHUNK_SIZE = 2;
    public static int PAYLOAD_LENGTH = 0; //21 recommended

    public static boolean USE_DNS_SEC = false;
    public static boolean MIX_HOST_CASE = false;

    public static String DOH_ADDRESS = null;

    private static final Set<String> GOVERNMENT_BLACKLIST = new HashSet<>();
    private static final Set<String> ISP_STUB_LIST = new HashSet<>();

    /**
     * PowerTunnel bootstrap
     */
    public static void bootstrap() throws UnknownHostException {
        System.out.println("[*] Base PowerTunnel/LibertyTunnel version is " + BASE_VERSION);
        //Load data
        GOVERNMENT_BLACKLIST.add("*");
        //ISP_STUB_LIST.add("");
        System.out.println("[i] Loaded '" + GOVERNMENT_BLACKLIST.size() + "' government blocked sites");
        System.out.println();

        //Fill payload cache
        ProxyFilter.PAYLOAD.clear();
        for(int i = 0; i < PAYLOAD_LENGTH; i++) {
            ProxyFilter.PAYLOAD.add(new String(new char[1000])
                    .replace("\0", String.valueOf(i % 10)).intern());
        }

        //Start server
        startServer();
    }

    /**
     * Starts LittleProxy server
     */
    private static void startServer() throws UnknownHostException {
        System.out.println("[.] Starting LittleProxy server on " + SERVER_IP_ADDRESS + ":" + SERVER_PORT);
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        HttpProxyServerBootstrap bootstrap = DefaultHttpProxyServer.bootstrap().withFiltersSource(new HttpFiltersSourceAdapter() {
            @Override
            public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext ctx) {
                return new ProxyFilter(originalRequest);
            }
        }).withAddress(new InetSocketAddress(InetAddress.getByName(SERVER_IP_ADDRESS), SERVER_PORT))
                .withTransparent(true).withUseDnsSec(USE_DNS_SEC);
        boolean useDoh = DOH_ADDRESS != null && !DOH_ADDRESS.isEmpty();
        if (useDoh) {
            if (DOH_ADDRESS.endsWith("/")) {
                DOH_ADDRESS = DOH_ADDRESS.substring(0, DOH_ADDRESS.length() - 1);
            }
            System.out.println("[*] DNS over HTTPS is enabled: '" + DOH_ADDRESS + "'");
            bootstrap.withServerResolver(new HostResolver() {
                @Override
                public InetSocketAddress resolve(String host, int port) throws UnknownHostException {
                    try {
                        //System.out.println("[DoH] Resolving: " + host);
                        return new InetSocketAddress(AndroidDohResolver.resolve(host), port);
                    } catch (Exception ex) {
                        System.out.println(String.format("[x] DoH: Failed to resolve '%s': %s", host, ex.getMessage()));
                        ex.printStackTrace();
                        //return new InetSocketAddress(InetAddress.getByName(host), port);
                        throw new UnknownHostException(String.format("DoH: Failed to resolve '%s': %s", host, ex.getMessage()));
                    }
                }
            });
        } else if(USE_DNS_SEC || PTManager.DNS_OVERRIDE) {
            System.out.println("[*] Enabled advanced resolver | DNSSec: " + USE_DNS_SEC + " / DNSOverride: " + PTManager.DNS_OVERRIDE);
            final Resolver resolver = getResolver();
            if(resolver != null) {
                bootstrap.withServerResolver(new HostResolver() {
                    @Override
                    public InetSocketAddress resolve(String host, int port) throws UnknownHostException {
                        try {
                            Lookup lookup = new Lookup(host, Type.A);
                            lookup.setResolver(resolver);
                            Record[] records = lookup.run();
                            if (lookup.getResult() == Lookup.SUCCESSFUL) {
                                return new InetSocketAddress(((ARecord) records[0]).getAddress(), port);
                            } else {
                                throw new UnknownHostException(lookup.getErrorString());
                            }
                        } catch (Exception ex) {
                            //System.out.println(String.format("[x] Failed to resolve '%s': %s", host, ex.getMessage()));
                            //throw new UnknownHostException(String.format("Failed to resolve '%s': %s", host, ex.getMessage()));
                            return new InetSocketAddress(InetAddress.getByName(host), port);
                        }
                    }
                });
            }
        }
        SERVER = bootstrap.start();
        RUNNING = true;
        System.out.println("[.] Server started");
        System.out.println();
    }

    private static Resolver getResolver() throws UnknownHostException {
        try {
            Class.forName("java.time.Duration"); //one of dnsjava Java 8 imports
        } catch (ClassNotFoundException ex) {
            return null;
        }
        String primaryDnsServer = PTManager.DNS_SERVERS.get(0);
        Resolver resolver = new SimpleResolver(primaryDnsServer);
        if(USE_DNS_SEC) {
            System.out.println("[*] DNSSec is enabled");
            resolver = new ValidatingResolver(resolver);
        }
        return resolver;
    }

    /**
     * Stops LittleProxy server
     */
    public static void stopServer() {
        System.out.println();
        System.out.println("[.] Stopping server...");
        SERVER.stop();
        System.out.println("[.] Server stopped");
        System.out.println();
        RUNNING = false;
    }

    /**
     * Save data and goodbye
     */
    public static void stop() {
        stopServer();
        GOVERNMENT_BLACKLIST.clear();
        ISP_STUB_LIST.clear();
    }

    /**
     * Retrieve is LittleProxy server is running
     *
     * @return true if it is or false if it isn't
     */
    public static boolean isRunning() {
        return RUNNING;
    }

    /*
    Government blacklist block
     */

    /**
     * Retrieves the government blacklist
     *
     * @return government blacklist
     */
    public static Set<String> getGovernmentBlacklist() {
        return GOVERNMENT_BLACKLIST;
    }

    /**
     * Determine if 302-redirect location is ISP (Internet Service Provider) stub
     *
     * @param address - redirect location
     * @return true if it's ISP stub or false if it isn't
     */
    public static boolean isIspStub(String address) {
        String host;
        if(address.contains("/")) {
            host = address.substring(0, address.indexOf("/"));
        } else {
            host = address;
        }
        host = URLUtility.clearHost(host).toLowerCase();
        return ISP_STUB_LIST.contains(host);
    }

    /**
     * Retrieves is the website blocked by the government
     *
     * @param address - website address
     * @return is address blocked by the government
     */
    public static boolean isBlockedByGovernment(String address) {
        return URLUtility.checkIsHostContainsInList(address.toLowerCase(), GOVERNMENT_BLACKLIST);
        //return GOVERNMENT_BLACKLIST.contains(address.toLowerCase());
    }
}