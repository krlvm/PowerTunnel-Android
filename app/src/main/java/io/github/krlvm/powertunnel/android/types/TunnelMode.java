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

package io.github.krlvm.powertunnel.android.types;

import android.app.Service;

import io.github.krlvm.powertunnel.android.services.ProxyService;
import io.github.krlvm.powertunnel.android.services.TunnelingVpnService;

public enum TunnelMode {

    PROXY(ProxyService.class),
    VPN(TunnelingVpnService.class);

    final Class<? extends Service> serviceClass;

    TunnelMode(Class<? extends Service> serviceClass) {
        this.serviceClass = serviceClass;
    }

    public Class<? extends Service> getServiceClass() {
        return serviceClass;
    }

    // Android doesn't allow ForegroundID < 1
    public int id() {
        return ordinal() + 1;
    }
}
