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

package ru.krlvm.powertunnel.utilities;

import org.littleshoot.proxy.mitm.Authority;
import org.littleshoot.proxy.mitm.CertificateSniffingMitmManager;

import ru.krlvm.powertunnel.PowerTunnel;
import ru.krlvm.powertunnel.android.MainActivity;

public class MITMUtility {

    public static CertificateSniffingMitmManager mitmManager() throws Exception {
        try {
            return new CertificateSniffingMitmManager(new Authority(MainActivity.DATA_DIR,
                    "powertunnel-root-ca", PowerTunnel.MITM_PASSWORD,
                    "PowerTunnel Root CA",
                    "PowerTunnel",
                    "PowerTunnel",
                    "PowerTunnel",
                    "PowerTunnel"));
        } finally {
            PowerTunnel.MITM_PASSWORD = null;
        }
    }
}
