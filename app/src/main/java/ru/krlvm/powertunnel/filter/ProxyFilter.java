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

package ru.krlvm.powertunnel.filter;

import org.littleshoot.proxy.HttpFiltersAdapter;

import java.util.LinkedList;
import java.util.List;

import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import ru.krlvm.powertunnel.PowerTunnel;

/**
 * Implementation of LittleProxy filter
 *
 * @author krlvm
 */
public class ProxyFilter extends HttpFiltersAdapter {

    public ProxyFilter(HttpRequest originalRequest) {
        super(originalRequest);
        //Allow us to modify 'HOST' request header
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }

    /**
     * Filtering client to proxy request:
     * 1) Check if website is in the government blacklist - if it's true goto 2)
     * 2) Try to circumvent DPI
     */
    @Override
    public HttpResponse clientToProxyRequest(HttpObject httpObject) {
        if (httpObject instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) httpObject;
            if(request.getMethod() == HttpMethod.CONNECT && !PowerTunnel.APPLY_HTTP_TRICKS_TO_HTTPS) {
                return null;
            }
            if(!request.headers().contains("Host")) {
                return null;
            }
            circumventDPI(request);
        }

        return null;
    }

    //@Override
    //public HttpResponse proxyToServerRequest(HttpObject httpObject) {
    //    if (httpObject instanceof HttpRequest) {
    //        HttpRequest request = (HttpRequest) httpObject;
    //        if(request.headers().contains("Via")) {
    //            request.headers().remove("Via");
    //        }
    //    }
    //    return null;
    //}

    /*@Override
    public HttpObject serverToProxyResponse(HttpObject httpObject) {
        if (httpObject instanceof DefaultHttpResponse) {
            DefaultHttpResponse response = (DefaultHttpResponse) httpObject;
            if(response.getStatus().code() == 302 && PowerTunnel.isIspStub(response.headers().get("Location"))) {
                return HttpUtility.getStub("Thrown out ISP redirect to the stub");
            }
        }

        return httpObject;
    }*/

    /**
     * DPI circumvention algorithm for HTTP requests
     *
     * @param request - original HttpRequest
     */
    private static void circumventDPI(HttpRequest request) {
        String host = request.headers().get("Host");
        if(PowerTunnel.MIX_HOST_CASE) {
            if(PowerTunnel.COMPLETE_MIX_HOST_CASE) {
                StringBuilder modified = new StringBuilder();
                for (int i = 0; i < host.length(); i++) {
                    char c = host.toCharArray()[i];
                    if (i % 2 == 0) {
                        c = Character.toUpperCase(c);
                    }
                    modified.append(c);
                }
                host = modified.toString();
            } else {
                host = host.substring(0, host.length()-1) + host.substring(host.length()-1).toUpperCase();
            }
        }
        if(PowerTunnel.PAYLOAD_LENGTH > 0) {
            request.headers().remove("Host");
            for (int i = 0; i < PowerTunnel.PAYLOAD_LENGTH; i++) {
                request.headers().add("X-Padding" + i, PAYLOAD.get(i));
            }
        }
        if(request.getMethod() != HttpMethod.CONNECT && PowerTunnel.isHTTPMethodTricksEnabled()) {
            String method = request.getMethod().name();
            if(PowerTunnel.LINE_BREAK_BEFORE_GET) {
                method = "\r\n" + method;
            }
            if(PowerTunnel.ADDITIONAL_SPACE_AFTER_GET) {
                method = method + " ";
            }
            request.setMethod(new HttpMethod(method));
        }
        if(PowerTunnel.DOT_AFTER_HOST_HEADER) {
            host = host + ".";
            request.headers().remove("Host");
        }
        if(!request.headers().contains("Host")) {
            String hostHeader = PowerTunnel.MIX_HOST_HEADER_CASE ? "hOSt" : "Host";
            request.headers().add(hostHeader, host);
        }
    }

    public static final List<String> PAYLOAD = new LinkedList<>();
}
