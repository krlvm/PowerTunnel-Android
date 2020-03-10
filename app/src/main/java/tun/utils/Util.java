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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class Util {

    private static native String jni_getprop(String name);

    public static List<String> getDefaultDNS(Context context) {
        List<String> defaultServers = new ArrayList<>();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(cm != null) {
                Network an = cm.getActiveNetwork();
                if (an != null) {
                    LinkProperties lp = cm.getLinkProperties(an);
                    if (lp != null) {
                        List<InetAddress> dns = lp.getDnsServers();
                        for (InetAddress address : dns) {
                            defaultServers.add(address.getHostAddress());
                        }
                    }
                }
            }
        } else {
            defaultServers.add(jni_getprop("net.dns1"));
            defaultServers.add(jni_getprop("net.dns2"));
        }
        return defaultServers;
    }
}
