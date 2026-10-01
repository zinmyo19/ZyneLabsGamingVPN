# DzinLabs Gaming VPN — hub source

Editable hub UI sources for the DzinLabs Gaming VPN (WarpWG-based, package `com.zeus.warpwg`).

- `src/com/dzinlabs/gvpn/` — GameHubActivity, AboutActivity, boost/HUD/net/reflex/device/domain/settings tools
- `src/com/booster/shizuku/` — Shizuku booster backend (Prefs, ScriptStore, BoostBubbleService)

## Build

The full APK is rebuilt from the previous release via apktool surgical smali replace:

1. Decode previous release APK (`apktool d`)
2. Bump `versionCode`/`versionName` in `apktool.yml`
3. Recompile changed `.java` with `javac` → `d8` → `baksmali`
4. Replace only the changed `*.smali` in `smali_classes2`
5. `apktool b` → `zipalign -p` → sign with the project debug key

Releases keep their APK assets; newer versions install as updates over older ones.
