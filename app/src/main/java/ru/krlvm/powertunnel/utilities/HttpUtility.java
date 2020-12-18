package ru.krlvm.powertunnel.utilities;

import android.util.Base64;

import org.littleshoot.proxy.impl.ProxyUtils;

import java.nio.charset.Charset;
import java.util.Date;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * Utility for working with HTTP requests and responses
 *
 * @author krlvm
 */
public class HttpUtility {

    // Android Studio loves to replace Charset.forName("UTF-8")
    // with StandardCharsets.UTF-8 which is not compatible
    // with older Android versions, so we use Charset.forName("UTF-8" + "")
    // to avoid warnings and auto-replace, because this warning can not be suppressed
    // in the standard way

    /**
     * Retrieves stub with reason in a HTML-body
     *
     * @param reason - reason
     * @return stub packet
     */
    public static HttpResponse getStub(String reason) {
        return getClosingResponse("<html><head>\n"
                + "<title>Access denied</title>\n"
                + "</head><body>\n"
                + "<p style='color: red; font-weight: bold'>" + reason + "</p>"
                + "</body></html>\n");
    }

    /**
     * Retrieves response with connection-close mark
     *
     * @param html - HTML code
     * @return HttpResponse with connection-close mark
     */
    public static HttpResponse getClosingResponse(String html) {
        HttpResponse response = getResponse(html);
        response.headers().set(HttpHeaders.Names.CONNECTION, "close");
        return response;
    }

    /**
     * Retrieves response with HTML code
     *
     * @param html - HTML code
     * @return HttpResponse with HTML code
     */
    public static HttpResponse getResponse(String html) {
        String body = "<!DOCTYPE html>\n" + html;
        byte[] bytes = body.getBytes(Charset.forName("UTF-8" + ""));
        ByteBuf content = Unpooled.copiedBuffer(bytes);
        HttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY, content);
        response.headers().set(HttpHeaders.Names.CONTENT_LENGTH, bytes.length);
        response.headers().set("Content-Type", "text/html; charset=UTF-8");
        response.headers().set("Date", ProxyUtils.formatDate(new Date()));
        return response;
    }

    /**
     * Retrieves formatted host string
     *
     * @param host - initial host value
     * @return formatted host string
     */
    public static String formatHost(String host) {
        return host.replace(":443", "").replace("www.", "");
    }

    /**
     * Generates upstream proxy auth code
     */
    public static String generateAuthCode(String username, String password) {
        String credential = username + ":" + password;
        byte[] data;
        data = credential.getBytes(Charset.forName("UTF-8" + ""));
        return Base64.encodeToString(data, Base64.DEFAULT).trim();
    }
}
