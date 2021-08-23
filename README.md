<div align="center">
<img src="https://raw.githubusercontent.com/krlvm/PowerTunnel/master/.github/images/logo.png" height="192px" width="192px" />
<br><h1>PowerTunnel for Android</h1>
Simple, scalable, cross-platform and effective solution against government censorship for Android
<!-- That does not mean the battle is finished -->
<!--<h3><b>Please, read <a href="https://gist.github.com/krlvm/76595f2fec7e23cf5e20f8ccfa43997a">important announcement</a></b></h3>-->

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
PowerTunnel is an extensible proxy server built on top of [LittleProxy](https://github.com/adamfisk/LittleProxy).

PowerTunnel provides an SDK that allows you to extend its functionality however you like, and even handle encrypted HTTPS traffic (powered by [LittleProxy-MITM](https://github.com/ganskef/LittleProxy-mitm)), which can be especially useful in web development. PowerTunnel has an Android version, so any plugin you write can work on almost all devices.


PowerTunnel was originally developed and is best known as a censorship bypass tool. This functionality has been spun off in the [LibertyTunnel](https://github.com/krlvm/LibertyTunnel) plugin, it is installed by default, just like [DNS Resolver](https://github.com/krlvm/PowerTunnel-DNS) with DNS over HTTPS support.

Version 2.0 is currently in Beta, the SDK Documentation is coming soon.


## Dependencies
* [NetGuard](https://github.com/M66B/NetGuard) - VPN server and traffic interceptor
* [LittleProxy](https://github.com/adamfisk/LittleProxy) - proxy server
* [LittleProxy-MITM](https://github.com/ganskef/LittleProxy-mitm) - LittleProxy SSL extension
* [dnsjava](https://github.com/dnsjava/dnsjava) - DNS library, DoH realization
* [dnssecjava](https://github.com/ibauersachs/dnssecjava) - DNSSec realization for dnsjava
* [DNSSEC4J](https://github.com/adamfisk/DNSSEC4J) - DNSSec realization for LittleProxy

### Credits
* [blockcheck](https://github.com/ValdikSS/blockcheck)
