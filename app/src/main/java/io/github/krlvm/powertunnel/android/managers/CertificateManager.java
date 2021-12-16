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

package io.github.krlvm.powertunnel.android.managers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.security.KeyChain;
import android.util.Log;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import io.github.krlvm.powertunnel.android.R;
import io.github.krlvm.powertunnel.android.utility.FileUtility;
import io.github.krlvm.powertunnel.mitm.MITMAuthority;

public class CertificateManager {

    private static final String LOG_TAG = "CertMgr";

    public static void checkCertificate(Activity activity, int requestCodeInstall, int requestCodeExtract) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if(prefs.getBoolean("cert_refuse", false)) return;

        boolean installed = false;
        try {
            final KeyStore ks = KeyStore.getInstance("AndroidCAStore");
            if (ks != null) {
                ks.load(null, null);
                final Enumeration<String> aliases = ks.aliases();
                while (aliases.hasMoreElements()) {
                    String alias = aliases.nextElement();
                    final X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
                    if (cert.getIssuerDN().getName().contains("PowerTunnel")) {
                        installed = true;
                        break;
                    }
                }
            }
        } catch (Exception ignore) {}

        installed = installed && prefs.getBoolean("cert_installed", false);
        if (!installed) {
            installCertificate(activity, requestCodeInstall, requestCodeExtract);
        }
    }

    public static void installCertificate(Activity activity, int requestCodeInstall, int requestCodeExtract) {
        Toast.makeText(activity, R.string.toast_cert_install, Toast.LENGTH_LONG).show();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            Toast.makeText(activity, R.string.toast_cert_install_a11, Toast.LENGTH_LONG).show();
            extractCertificate(activity, requestCodeExtract);
            return;
        }

        final StringBuilder cert = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(getCertificate(activity)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                cert.append(line).append('\n');
            }
        } catch (Exception ex) {
            Log.e(LOG_TAG, "Failed to read certificate and show installation prompt: " + ex.getMessage(), ex);
            Toast.makeText(activity, R.string.toast_cert_install_error, Toast.LENGTH_LONG).show();
            return;
        }

        activity.startActivityForResult(KeyChain.createInstallIntent()
                        .putExtra(KeyChain.EXTRA_CERTIFICATE, cert.toString().getBytes())
                        .putExtra(KeyChain.EXTRA_NAME, "PowerTunnel Root CA"),
                requestCodeInstall);
    }

    public static void extractCertificate(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Toast.makeText(activity, R.string.toast_cert_extract_os_unsupported, Toast.LENGTH_LONG).show();
            return;
        }
        activity.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT)
                        .addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("application/x-pem-file")
                        .putExtra(Intent.EXTRA_TITLE, "PowerTunnel-CA.pem"),
                requestCode);
    }

    public static void extractCertificate(Context context, Uri uri) {
        try {
            FileUtility.copy(CertificateManager.getCertificate(context),
                    context.getContentResolver().openOutputStream(uri));
            Toast.makeText(context, R.string.toast_cert_extracted, Toast.LENGTH_SHORT).show();
        } catch (IOException ex) {
            Log.e(LOG_TAG, "Failed to extract certificate: " + ex.getMessage(), ex);
            Toast.makeText(context, R.string.toast_cert_extraction_failed, Toast.LENGTH_SHORT).show();
        }
    }

    public static File getCertificate(Context context) {
        return new File(context.getFilesDir(), "cert" + File.separator + MITMAuthority.CERTIFICATE_ALIAS + ".pem");
    }
}
