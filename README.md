# NINJA Private VPN v0.3 beta

Android app for **OpenVPN for Android** by Arne Schwabe. This app is a controller, **not a VPN engine**. It needs the independently installed OpenVPN for Android, plus a real OpenVPN TCP or UDP server and a self-contained client .ovpn profile. No servers, game IPs, credentials, or private keys are bundled.

Build APK via GitHub **Actions > Build NINJA APK**, select latest successful run, download **NINJA_Private_VPN_APK**, unzip and install app-debug.apk on Red Magic. Android may ask to allow installation from browser/files.

The client provides live status from the external OpenVPN engine, import local .ovpn config, connect and disconnect, TCP latency/jitter probe to 1.1.1.1:443 (not Konami ping). A VPN cannot guarantee matchmaking region or improve eFootball latency. Review security of your server provider and avoid uploading .ovpn keys to GitHub. Debug build is only for testing.
