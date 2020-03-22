package ru.krlvm.powertunnel.android;

import org.xbill.DNS.DClass;
import org.xbill.DNS.DohResolver;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import java.net.UnknownHostException;

import ru.krlvm.powertunnel.PowerTunnel;

public class DOHUtility {

    public static String resolve(String host) throws TextParseException, UnknownHostException {
        if(host.isEmpty()) {
            return host;
        }
        Lookup lookup = new Lookup(host, Type.A, DClass.IN);
        lookup.setResolver(new DohResolver(PowerTunnel.DOH_ADDRESS));
        Record[] records = lookup.run();
        for (Record record : records) {
            if(record.getName().equals(Name.fromString(host))) {
                return record.rdataToString();
            }
        }
        throw new UnknownHostException("DoH lookup failed");
    }
}
