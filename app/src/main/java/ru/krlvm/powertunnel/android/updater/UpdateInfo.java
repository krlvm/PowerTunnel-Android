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

package ru.krlvm.powertunnel.android.updater;

import ru.krlvm.powertunnel.android.BuildConfig;

public class UpdateInfo {

    private final int versionCode;
    private final String version;
    private final int requiredOs;
    private String changelog = "";

    protected UpdateInfo(int versionCode, String version, int requiredOs) {
        this.versionCode = versionCode;
        this.version = version;
        this.requiredOs = requiredOs;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getVersion() {
        return version;
    }

    public void setChangelog(String changelog) {
        this.changelog = changelog;
    }

    public String getChangelog() {
        return changelog;
    }

    public boolean isReady() {
        return BuildConfig.VERSION_CODE < versionCode
                && android.os.Build.VERSION.SDK_INT >= requiredOs;
    }

    public int calculateObsolescence() {
        return versionCode - BuildConfig.VERSION_CODE;
    }
}
