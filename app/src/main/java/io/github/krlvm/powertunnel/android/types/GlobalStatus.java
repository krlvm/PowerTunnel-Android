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

public enum GlobalStatus {

    NOT_RUNNING,
    STARTING,
    STOPPING,

    PROXY(TunnelMode.PROXY),
    VPN(TunnelMode.VPN);


    final TunnelMode mode;

    GlobalStatus() {
        this(null);
    }

    GlobalStatus(TunnelMode mode) {
        this.mode = mode;
    }

    public TunnelMode getMode() {
        return mode;
    }
}
