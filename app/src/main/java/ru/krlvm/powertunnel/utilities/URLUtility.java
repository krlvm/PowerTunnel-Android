package ru.krlvm.powertunnel.utilities;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.Collection;

/**
 * Utility for working with URLs
 *
 * @author krlvm
 */
public class URLUtility {

    /**
     * Retrieves is host contains in a collection
     * For example:
     * host -> cdn-images-1.[medium.com]
     * s -> [medium.com]
     * ==> true
     *
     * @param host - host
     * @param collection - list
     * @return true if contains or false if it isn't
     */
    public static boolean checkIsHostContainsInList(String host, Collection<String> collection) {
        if(collection.contains("*")) {
            return true;
        }
        for (String s : collection) {
            if(host.endsWith(s) || host.startsWith(s)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves host without protocol, www. and etc.
     *
     * @param host - original host
     * @return cleared host
     */
    public static String clearHost(String host) {
        return host.replace("https://", "").replace("http://", "").replace("www.", "")
                .replace(":443", "");
    }

    /**
     * Retrieves URL address content
     *
     * @param address - URL address
     * @return - content
     *
     * @throws IOException - read/connect failure
     */
    public static String load(String address) throws IOException {
        LineNumberReader reader = new LineNumberReader(new InputStreamReader(new URL(address).openStream()));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }
}