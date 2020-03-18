package ru.krlvm.powertunnel.android;

import org.xbill.DNS.DohResolver;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import ru.krlvm.powertunnel.PowerTunnel;

public class DOHUtility {

    public static String resolve(String host) throws Exception {
        if(host.isEmpty()) {
            return host;
        }
        Lookup lookup = new Lookup(host, Type.URI);
        lookup.setResolver(new DohResolver(PowerTunnel.DOH_ADDRESS));
        for (Record record : lookup.run()) {
            if (record.getRRsetType() == Type.A && record.getName().equals(Name.fromString(host))) {
                return record.rdataToString();
            }
        }
        return null;
    }
}
