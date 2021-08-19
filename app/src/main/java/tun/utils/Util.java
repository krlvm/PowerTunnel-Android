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

package tun.utils;

/*
    This file is part of NetGuard.

    NetGuard is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    NetGuard is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with NetGuard.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2015-2017 by Marcel Bokhorst (M66B)
*/

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Build;
import android.text.TextUtils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class Util {

    private static native String jni_getprop(String name);

    public static List<String> getDefaultDNS(Context context) {
        final List<String> servers = new ArrayList<>();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            final ConnectivityManager cm =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final Network network = cm.getActiveNetwork();
            if (network != null) {
                LinkProperties properties = cm.getLinkProperties(network);
                if (properties != null) {
                    final List<InetAddress> addresses = properties.getDnsServers();
                    for (InetAddress address : addresses) {
                        final String host = address.getHostAddress();
                        if (TextUtils.isEmpty(host)) continue;
                        servers.add(host);
                    }
                }
            }
        } else {
            String dns = jni_getprop("net.dns1");
            if(!TextUtils.isEmpty(dns)) servers.add(dns);
            dns = jni_getprop("net.dns1");
            if(!TextUtils.isEmpty(dns)) servers.add(dns);
        }

        return servers;
    }
}
