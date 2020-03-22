<div align="center">
<img src="https://raw.githubusercontent.com/krlvm/PowerTunnel/master/images/logo.png" height="192px" width="192px" />
<br><h1>PowerTunnel for Android</h1><br>
Simple, scalable, cross-platform and effective solution against government censorship for Android

<a href="https://github.com/krlvm/PowerTunnel">Looking for the PC version?<a/>
<br><br>
<a href="https://github.com/krlvm/PowerTunnel-Android/blob/master/LICENSE"><img src="https://img.shields.io/github/license/krlvm/PowerTunnel-Android?style=flat-square" alt="License"/></a>
<a href="https://github.com/krlvm/PowerTunnel-Android/releases"><img src="https://img.shields.io/github/v/release/krlvm/PowerTunnel-Android?style=flat-square" alt="Latest release"/></a>
<a href="https://github.com/krlvm/PowerTunnel-Android/releases"><img src="https://img.shields.io/github/downloads/krlvm/PowerTunnel-Android/total?style=flat-square" alt="Downloads"/></a>
<a href="https://github.com/krlvm/PowerTunnel-Android/wiki"><img src="https://img.shields.io/badge/help-wiki-yellow?style=flat-square" alt="Help on the Wiki"/></a>
<br>
<img src="https://raw.githubusercontent.com/krlvm/PowerTunnel-Android/master/images/ui.png" alt="PowerTunnel User Interface" height="500px" /> <img src="https://raw.githubusercontent.com/krlvm/PowerTunnel-Android/master/images/ui-active.png" alt="PowerTunnel User Interface" height="500px" /> <img src="https://raw.githubusercontent.com/krlvm/PowerTunnel-Android/master/images/ui-settings.png" alt="PowerTunnel User Interface" height="500px" />
</div>

The Android version of PowerTunnel is built on [VPN Server](https://github.com/raise-isayan/TunProxy) that intercepts traffic and directs it through the [LibertyTunnel](https://github.com/krlvm/PowerTunnel/tree/libertytunnel) proxy

### What is it
Nowadays Internet censorship is introducing in many countries: governments analyze and block traffic to this sites using DPI - Deep Packet Inspection systems, forcing you using circumvention utilities like VPN, for example. That approach have many disadvantages, most noticeable - connection speed slowdown. In addition, these services cannot guarantee work stability and your data confidence.

PowerTunnel for Android is active DPI circumvention utility, that works only on your phone and don't send your traffic to third-party servers, respecting your privacy and do not slowing down your internet connection.

Since PowerTunnel for Android uses the same architecture as the PC version, the Android version also uses proxy server - it lies under the VPN service and running at *127.0.0.1:8085*, so you can setup PowerTunnel in ways other than VPN.

### How does it work?
PowerTunnel for Android establishes a transparent proxy server on your phone and starts local VPN server, that forwards your traffic into the proxy server, where are DPI circumvention tricks applying.

## How can I get it?
You can compile a binary yourself or download prepared binary [here](https://github.com/krlvm/PowerTunnel-Android/releases).

### Setup
Just install it as a regular Android application: no root access is needed.

### Doesn't work
Most likely your ISP blocked the website you need by IP address, so only encrypted tunnel (VPN/Tor) can help you.

Also, you can try enabling full chunking mode (will be added in the upcoming stable release).

## Dependencies
* [TunProxy](https://github.com/raise-isayan/TunProxy) with [bugfixes](https://github.com/krlvm/TunProxy) - codebase, VPN server and traffic interceptor
* [LittleProxy](https://github.com/adamfisk/LittleProxy) with some [patches](https://github.com/krlvm/PowerTunnel-Android/tree/master/app/src/main/java/org/littleshoot/proxy/impl) - proxy server
* [DNSSEC4J](https://github.com/adamfisk/DNSSEC4J) - DNSSec realization for LittleProxy
* [dnsjava](https://github.com/dnsjava/dnsjava) - library for making DoH requests
