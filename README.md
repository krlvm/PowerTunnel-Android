<div align="center">
<img src="https://raw.githubusercontent.com/krlvm/PowerTunnel/master/.github/images/logo.png" height="192px" width="192px" />
<br><h1>PowerTunnel for Android</h1>
Simple, scalable, cross-platform and effective solution against government censorship for Android

<a href="https://t.me/powertunnel_dpi">Telegram channel</a>
<br>
<a href="https://github.com/krlvm/PowerTunnel">Looking for the PC version?<a/>
<br><br>
<a href="https://github.com/krlvm/PowerTunnel-Android/blob/master/LICENSE"><img src="https://img.shields.io/github/license/krlvm/PowerTunnel-Android?style=flat-square" alt="License"/></a>
<a href="https://github.com/krlvm/PowerTunnel-Android/releases/latest"><img src="https://img.shields.io/github/v/release/krlvm/PowerTunnel-Android?style=flat-square" alt="Latest release"/></a>
<a href="https://github.com/krlvm/PowerTunnel-Android/releases"><img src="https://img.shields.io/github/downloads/krlvm/PowerTunnel-Android/total?style=flat-square" alt="Downloads"/></a>
<a href="https://github.com/krlvm/PowerTunnel-Android/wiki"><img src="https://img.shields.io/badge/help-wiki-yellow?style=flat-square" alt="Help on the Wiki"/></a>
<br>
<img src="https://raw.githubusercontent.com/krlvm/PowerTunnel-Android/master/fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" alt="PowerTunnel User Interface" height="500px" /> <img src="https://raw.githubusercontent.com/krlvm/PowerTunnel-Android/master/fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" alt="PowerTunnel User Interface" height="500px" /> <img src="https://raw.githubusercontent.com/krlvm/PowerTunnel-Android/master/fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" alt="PowerTunnel User Interface" height="500px" />
</div>

The Android version of PowerTunnel is built on [VPN Server](https://github.com/M66B/NetGuard) that intercepts traffic and directs it through the PowerTunnel proxy

***You can't publish the app on the Google Play Store without permission: it is a violation of the license and the DMCA.***

### What is it
PowerTunnel is an extensible proxy server built on top of [LittleProxy](https://github.com/adamfisk/LittleProxy) that does not require root-access to work.

PowerTunnel provides an SDK that allows you to extend its functionality however you like, and even handle encrypted HTTPS traffic (powered by [LittleProxy-MITM](https://github.com/ganskef/LittleProxy-mitm)), which can be especially useful in web development. PowerTunnel has an Android version, so any plugin you write can work on almost all devices.


PowerTunnel was originally developed and is best known as a censorship bypass tool. This functionality has been spun off in the [LibertyTunnel](https://github.com/krlvm/LibertyTunnel) plugin, it is installed by default, just like [DNS Resolver](https://github.com/krlvm/PowerTunnel-DNS) with DNS over HTTPS support.


#### Anti-censorship tool

Digital censorship has become widespread in authoritarian and developing countries: governments install DPI - Deep Packet Inspection systems - for Internet Service Providers, which allows analyzing and blocking traffic to websites they don't want you to see, forcing you to use slow and often paid proxies or VPN services with dubious privacy policy.

PowerTunnel is an active DPI circumvention utility - it works only on your PC and do not route your traffic through some third-party webservers. It creates a local proxy server on your device and diverts your HTTP(S) traffic there, where PowerTunnel modifies your traffic in a special way to exploit bugs in DPI systems which makes it possible to bypass the lock - without (significantly) slowing down your Internet connection.

Anti-censorship module can be configured in Plugins window - it is called LibertyTunnel.

In this sense, PowerTunnel is a free cross-platform implementation of [GoodbyeDPI](https://github.com/ValdikSS/GoodbyeDPI) written in Java with support for Android.

Please, note that PowerTunnel does not change your IP address.

## Configuring

### Downloading PowerTunnel

PowerTunnel binary can be downloaded from the [Releases](https://github.com/krlvm/PowerTunnel-Android/releases) page.

If you don't trust the prebuilt APK, you can build PowerTunnel from source with Android Studio. It is also available in [F-Droid](https://f-droid.org) via [IzzyOnDroid repo](https://apt.izzysoft.de/fdroid/) ([details](https://apt.izzysoft.de/fdroid/index/apk/io.github.krlvm.powertunnel.android), versions 1.x are also [available](https://apt.izzysoft.de/fdroid/index/apk/ru.krlvm.powertunnel.android)).

### Using proxy instead of VPN

If you want to use PowerTunnel only with a single app, you can change mode from VPN to Proxy in PowerTunnel settings and configure the app manually to make it route its traffic via the proxy server.

VPN mode is supported on Android 5 Lollipop and higher.

### Configuring DPI circumvention

DPI circumvention can be configured in LibertyTunnel settings - open plugins page and tap to the gear opposite to LibertyTunnel plugin.

### Configuring DNS

To configure DNS, open plugins page and tap to the gear opposite to DNS plugin.

You are able to choose between pre-installed Google and Cloudflare DNS (DoH) providers or add yours.

DNS customization is not supported on Android versions below Android 8 Oreo.

### Enabling AdBlock

AdBlock is disabled by default. To enable, open plugins page and check the box next to AdBlock plugin, then restart PowerTunnel.

## Bundled Plugins
* [LibertyTunnel](https://github.com/krlvm/LibertyTunnel) - anti-censorship plugin for PowerTunnel
* [DNS Resolver](https://github.com/krlvm/PowerTunnel-DNS) - DNS Resolver with DNS over HTTPS (DoH) support
* [AdBlock](https://github.com/krlvm/PowerTunnel-AdBlock) - simple, but efficient ads and trackers blocker

## Dependencies
* [NetGuard](https://github.com/M66B/NetGuard) - VPN server and traffic interceptor
* [LittleProxy](https://github.com/adamfisk/LittleProxy) - proxy server
* [LittleProxy-MITM](https://github.com/ganskef/LittleProxy-mitm) - LittleProxy SSL extension
* [dnsjava](https://github.com/dnsjava/dnsjava) - DNS library, DoH realization
* [dnssecjava](https://github.com/ibauersachs/dnssecjava) - DNSSec realization for dnsjava
* [DNSSEC4J](https://github.com/adamfisk/DNSSEC4J) - DNSSec realization for LittleProxy
