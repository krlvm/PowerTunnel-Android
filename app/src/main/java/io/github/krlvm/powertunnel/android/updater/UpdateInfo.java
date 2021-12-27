package io.github.krlvm.powertunnel.android.updater;

import io.github.krlvm.powertunnel.android.BuildConfig;

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
