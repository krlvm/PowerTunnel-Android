package tun.utils;

import android.content.Intent;
import android.security.KeyChain;
import android.util.Log;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CertificateUtil {
    private static final String TAG = "CertificateManager";

    public enum CertificateInstallType {SYSTEM, USER};

    private final static Pattern CA_COMMON_NAME = Pattern.compile("CN=([^,]+),?.*$");
    private final static Pattern CA_ORGANIZATION = Pattern.compile("O=([^,]+),?.*$");

    public static boolean findCAStore(String caName) {
        boolean found = false;
        try {
            KeyStore ks = KeyStore.getInstance("AndroidCAStore");
            if (ks == null)
                return false;

            ks.load(null, null);
            X509Certificate rootCACert = null;
            Enumeration aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                rootCACert = (X509Certificate) ks.getCertificate(alias);
                if (rootCACert.getIssuerDN().getName().contains(caName)) {
                    found = true;
                    break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (KeyStoreException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (CertificateException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return found;
    }

    public static List<X509Certificate> getRootCAStore() {
        final List<X509Certificate> rootCAList = new ArrayList<>();
        try {
            KeyStore ks = KeyStore.getInstance("AndroidCAStore");
            if (ks == null)
                return null;

            ks.load(null, null);
            X509Certificate rootCACert = null;
            Enumeration aliases = ks.aliases();
            boolean found = false;
            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
                System.out.println(alias + "/" + cert.getIssuerX500Principal().getName());
                rootCAList.add(cert);
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (KeyStoreException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (CertificateException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return rootCAList;
    }

    public static Map<String, String[]> getRootCAMap(EnumSet<CertificateInstallType> type) {
        final Map<String, String[]> rootCAMap = new HashMap<>();
        try {
            KeyStore ks = KeyStore.getInstance("AndroidCAStore");
            if (ks == null) {
                return null;
            }

            ks.load(null, null);
            X509Certificate rootCACert = null;
            Enumeration aliases = ks.aliases();
            List<X509Certificate> certList = new ArrayList<>();
            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
                if (type.contains(CertificateInstallType.SYSTEM) && alias.startsWith("system:")) {
                    certList.add(cert);
                }
                if (type.contains(CertificateInstallType.USER) && alias.startsWith("user:")) {
                    certList.add(cert);
                }
            }
            Collections.sort(certList, new Comparator<X509Certificate>() {
                @Override
                public int compare(X509Certificate t1, X509Certificate t2) {
                String t1cn = CertificateUtil.getCommonName(t1.getIssuerX500Principal().getName());
                String t2cn = CertificateUtil.getCommonName(t2.getIssuerX500Principal().getName());
                return t1cn.compareToIgnoreCase(t2cn);
                }
            });
            // ソート後
            List<String> rootCANameList = new ArrayList<>();
            List<String> rootCAList = new ArrayList<>();
            for (X509Certificate cert : certList) {
                String cn = CertificateUtil.getCommonName(cert.getIssuerX500Principal().getName());
                if (cn.trim().isEmpty()) continue;
                //String o = CertificateUtil.getOrganization( cert.getIssuerX500Principal().getName());
                rootCANameList.add(cn);
                rootCAList.add(encode(cert.getEncoded()));
            }
            rootCAMap.put("entry", rootCANameList.toArray(new String[0]));
            rootCAMap.put("value", rootCAList.toArray(new String[0]));
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (KeyStoreException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (CertificateException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return rootCAMap;
    }

    public static String encode(byte b[]) {
        return new String(b, StandardCharsets.ISO_8859_1);
    }

    public static byte[] decode(String s) {
        return s.getBytes(StandardCharsets.ISO_8859_1);
    }

    public static Intent trustRootCA(X509Certificate cert) {
        Log.d(TAG, "root CA is not yet trusted");
        Intent intent = KeyChain.createInstallIntent();
        try {
            if (findCAStore(cert.getIssuerDN().getName())) return null;
            intent.putExtra(KeyChain.EXTRA_CERTIFICATE, cert.getEncoded());
            intent.putExtra(KeyChain.EXTRA_NAME, getCommonName(cert.getIssuerDN().getName()));
        } catch (CertificateEncodingException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return intent;
    }

    // get the CA certificate by the path
    public static X509Certificate getCACertificate(byte[] buff) {
        X509Certificate ca = null;
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ca = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(buff));
        } catch (CertificateException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return ca;
    }

    // get the CA certificate by the path
    public static X509Certificate getCACertificate(File caFile) {
        try (InputStream inStream = new FileInputStream(caFile)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(inStream);
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        } catch (CertificateException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return null;
    }

    public static String getCommonName(String dn) {
        String cn = "";
        Matcher m = CA_COMMON_NAME.matcher(dn);
        if (m.find()) {
            cn = m.group(1);
        }
        return cn;
    }

    public static String getOrganization(String dn) {
        String on = "";
        Matcher m = CA_ORGANIZATION.matcher(dn);
        if (m.find()) {
            on = m.group(1);
        }
        return on;
    }

}
