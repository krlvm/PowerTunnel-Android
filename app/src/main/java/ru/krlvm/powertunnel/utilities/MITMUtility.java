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
