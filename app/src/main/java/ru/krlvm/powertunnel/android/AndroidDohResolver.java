package ru.krlvm.powertunnel.android;

import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.Type;
import org.xbill.DNS.utils.base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import ru.krlvm.powertunnel.PowerTunnel;

/**
 * Parts of code were derived from project
 * dohjava (https://github.com/NUMtechnology/dohjava)
 * licensed under the MIT license
 */
public class AndroidDohResolver {

    private static final OPTRecord queryOPT = new OPTRecord(0, 0, 0);
    public static InetAddress resolve(String host) throws IOException {
        Record queryRecord = Record.newRecord(new Name(host + '.'), Type.A, DClass.IN);
        Message queryMessage = Message.newQuery(queryRecord);
        queryMessage.getHeader().setID(0);
        queryMessage.addRecord(queryOPT, Section.ADDITIONAL);
        Message response = query(queryMessage, 5000);
        for (RRset rrset : response.getSectionRRsets(Section.ANSWER)) {
            if (rrset.getType() == Type.A) {
                List<Record> records = rrset.rrs();
                for (Record record : records) {
                    return ((ARecord) record).getAddress();
                }
            }
        }
        throw new UnknownHostException("No records");
    }

    public static Message query(Message query, int timeout) throws IOException {
        String encodedQuery = base64.toString(query.toWire(), true);//Base64.encodeToString(query.toWire(), Base64.NO_PADDING);
        return makeGetRequest(encodedQuery, timeout);
    }

    private static Message makeGetRequest(String encodedQuery, int timeout) throws IOException {
        URL url = new URL(PowerTunnel.DOH_ADDRESS + "?dns=" + encodedQuery);
        HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Content-Type", "application/dns-message");
        con.setRequestProperty("Accept", "application/dns-message");
        con.setConnectTimeout(timeout);
        con.setReadTimeout(timeout);
        con.setDoInput(true);

        ByteArrayOutputStream content = new ByteArrayOutputStream();
        InputStream in = con.getInputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) != -1) {
            content.write(buffer, 0, length);
        }
        in.close();
        con.disconnect();
        try {
            return new Message(content.toByteArray());
        } finally {
            content.close();
        }
    }
}