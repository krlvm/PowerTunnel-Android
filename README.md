<div align="center">
<img src="https://raw.githubusercontent.com/krlvm/PowerTunnel/master/images/logo.png" height="192px" width="192px" />
<br><h1>PowerTunnel for Android</h1><br>
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

The Android version of PowerTunnel is built on [VPN Server](https://github.com/raise-isayan/TunProxy) that intercepts traffic and directs it through the [LibertyTunnel](https://github.com/krlvm/PowerTunnel/tree/libertytunnel) proxy

***You can't publish the app on the Google Play Store without permission: it is a violation of the license and the DMCA.***

### What is it
Nowadays Internet censorship is introducing in many countries: governments analyze and block traffic to this sites using DPI - Deep Packet Inspection systems, forcing you using circumvention utilities like VPN, for example. That approach have many disadvantages, most noticeable - connection speed slowdown. In addition, these services cannot guarantee work stability and your data confidence.

PowerTunnel for Android is active DPI circumvention utility, that works only on your phone and don't send your traffic to third-party servers, respecting your privacy and do not slowing down your internet connection.

Since PowerTunnel for Android uses the same architecture as the PC version, the Android version also uses proxy server - it lies under the VPN service and running at *127.0.0.1:8085*, so you can setup PowerTunnel in ways other than VPN. Just enable proxy mode in the settings.

### How does it work?
PowerTunnel for Android establishes a transparent proxy server on your phone and starts local VPN server, that forwards your traffic into the proxy server, where are DPI circumvention tricks applying.

PowerTunnel never decrypts your traffic, all code is open-source.\
You should install Root CA only in case you have [some specific options](https://github.com/krlvm/PowerTunnel/wiki/SNI-Tricks) enabled.

## How can I get it?
You can compile a binary yourself or download prepared binary [here](https://github.com/krlvm/PowerTunnel-Android/releases).

It's also available through a [F-Droid](https://f-droid.org/) client via the [IzzyOnDroid repo](https://apt.izzysoft.de/fdroid/). Details of PowerTunnel in IzzyOnDroid repo can be found [here](https://apt.izzysoft.de/fdroid/index/apk/ru.krlvm.powertunnel.android), it serves the same apk as the one you can find in the release section, it just helps to keep the app updated.

### Setup
Just install it as a regular Android application: no root access is needed.

## Configuring the application
### DNS lookup
PowerTunnel for Android provides various ways to configure DNS lookup and host name resolving. It's not recommended to change PowerTunnel's DNS settings if you don't have valuable reasons to to that because it can be unstable and slowdown your Internet connection.

#### DNS over HTTPS mode
You can enable DNS over HTTPS (DoH) mode in the settings - enable DNS override and select provider with the DoH label.

All available DoH providers are tested and fully compatible with PowerTunnel, you can also specify your favorite server. The DNS provider that works best with PowerTunnel is Google.

You can check does DoH work [there](https://ipleak.net/).

If you try to check your DNS [here](http://www.whatsmydnsserver.com/) you'll get nothing due to the particular features internal architecture of PowerTunnel Android version, although it actually works.

Overriding DNS settings can led to connectivity problems on Android 10 and higher. If you're experiencing problems, please, create an issue and submit your phone vendor.

#### DNSSec mode
DNSSec mode appears to validate DNS responses.

DNSSec mode is experimental and not recommended to use. Note that it is useless and not working when DoH mode is enabled (because DoH already validating DNS responses on server-side). When there's some troubles with resolving, resolving is going on with the system's DNS settings.

#### Custom DNS providers
You can also choose one of custom DNS providers (without DoH). It doesn't work with some of Android versions.

When there's some troubles with resolving using the choosen custom provider, resolving is going on with the system's DNS settings.

#### How to specify my favorite DNS provider
Just type its address in the settings. If the address starts with `https://` it will be recognized as a DoH provider. 

## Doesn't work
Try to disable chunking mode and enable SNI Spoil.

Most likely your ISP blocked the website you need by IP address, so only encrypted tunnel (VPN/Tor) can help you.

## Contributing
PowerTunnel is open-source software: you can help in the development process.

If you have a suggestsion or want to improve extising functionality consider making an issue or a pull request.

### Translating
You can also help by translating PowerTunnel to your language.

Localization contributors:
- Russian: [krlvm](https://github.com/krlvm)
- Polish: [Atrate](https://github.com/Atrate)

## Dependencies
* [TunProxy](https://github.com/raise-isayan/TunProxy) - VPN server and traffic interceptor
* [LittleProxy](https://github.com/adamfisk/LittleProxy) - proxy server
* [LittleProxy-MITM](https://github.com/ganskef/LittleProxy-mitm) - LittleProxy SSL extension
* [dnsjava](https://github.com/dnsjava/dnsjava) - DNS library, DoH realization
* [dnssecjava](https://github.com/ibauersachs/dnssecjava) - DNSSec realization for dnsjava
* [DNSSEC4J](https://github.com/adamfisk/DNSSEC4J) - DNSSec realization for LittleProxy

### Credits
* [blockcheck](https://github.com/ValdikSS/blockcheck)
