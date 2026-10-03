# vr-guardian student app (Meta Quest, native)

A real installed Quest app — **not** a web page. Built with **Meta Spatial SDK 0.14.0**:
one spatial panel floats in the room and everything inside it is native Compose.

The teacher dashboard stays a web page on the laptop. This project is only the
student side that runs on the headset.

```
package  mn.vrguardian.student
label    Миний сургалт        (Mongolian, nothing about supervision)
panel    1.6 x 1.0 m, ~1.7 m in front of the student, 800 x 500 dp of Compose
```

## What it does

| Behaviour | Where |
|---|---|
| Polls the teacher hub (`GET /api/state`) every 1.2 s | `hub/HubSession.kt` |
| Heartbeats this headset (`POST /api/student`) every 1.2 s | `hub/HubSession.kt` |
| Launcher with 4 tiles (Калькулятор / Тэмдэглэл / Интернет / Тоглоом) | `ui/StudentApp.kt` |
| Blocked apps show **хориглосон** and refuse to open | `ui/StudentApp.kt` |
| Blocked *while open* → denial tone + red **БАГШ ТҮГЖЛЭЭ** overlay + app closes | `HubSession` → `StudentApp` |
| Search screen with 4 topic chips; blocked topics are reported anyway | `ui/StudentApp.kt` |
| Server URL + student name, persisted in SharedPreferences | `hub/HubClient.kt` (`Prefs`) |

The poll loop deliberately lives in the **Activity**, not in the panel's Compose
tree: a supervision client must keep reporting and keep enforcing blocks even
when the panel is not being drawn.

## Toolchain (all verified against Meta's official samples repo)

| Piece | Version |
|---|---|
| Meta Spatial SDK | 0.14.0 (`com.meta.spatial:meta-spatial-sdk{,-vr,-compose,-toolkit,-ovrmetrics}`) |
| Android Gradle Plugin | 8.11.1 |
| Gradle | 9.4.1 |
| Kotlin | **2.2.0** — must match the compiler embedded in `com.meta.spatial.plugin` 0.14.0 |
| compileSdk / targetSdk / minSdk | 34 (Horizon OS is Android 14) |
| JDK | Temurin 17 |

> Kotlin 2.1.0 (the version in Meta's sample catalog) fails with
> `IncrementalCompilationFeatures(...)` `NoSuchMethodError`, because the spatial
> Gradle plugin puts `kotlin-compiler-embeddable:2.2.0` on the build classpath.

Local toolchain paths (installed manually — `metavr tools install` is account-gated):

```
JDK      C:\Users\hi\bin\android\jdk17
Android  C:\Users\hi\bin\android\sdk
adb      C:\Users\hi\bin\platform-tools\adb.exe
```

## Build

```powershell
$env:JAVA_HOME='C:\Users\hi\bin\android\jdk17'
$env:ANDROID_HOME='C:\Users\hi\bin\android\sdk'
cd C:\Users\hi\Documents\vr-guardian-quest
.\gradlew.bat :app:assembleDebug
# -> app\build\outputs\apk\debug\app-debug.apk   (~85 MB)
```

## Deploy + verify

```powershell
powershell -ExecutionPolicy Bypass -File C:\Users\hi\Documents\vr-student-app\_deploy-app.ps1
```

That waits for the headset, keeps it awake, installs, runs `adb reverse tcp:8000`,
launches it and dumps logs, hub state and screenshots.

Manual equivalent:

```powershell
adb install -r -g app-debug.apk
adb reverse tcp:8000 tcp:8000
adb shell am start -a android.intent.action.MAIN -c com.oculus.intent.category.VR -n mn.vrguardian.student/.MainActivity
adb logcat | findstr VrGuardian     # "hub session start", "hub online", "remote block"
```

The default server URL is `http://127.0.0.1:8000`, which is exactly what
`adb reverse` exposes — plug in USB and it works with no configuration.
On Wi-Fi, set `http://<laptop-ip>:8000` in **Тохиргоо** inside the panel.

## Design notes / gotchas

- **Headset must be awake and worn.** A VR activity is paused when the device
  sleeps, so nothing renders (`screencap` returns empty and the panel never
  composes). Before a test: `prox_close` broadcast + `KEYCODE_WAKEUP`.
- Symbol lookup at scene start follows `FeatureDevSample`: `setViewOrigin(0, 0, 2, 180)`
  puts the viewer 2 m along +Z facing −Z; the panel sits at `z = 0.3`, i.e. ~1.7 m ahead.
- `res/xml/network_security_config.xml` allows cleartext — the demo talks plain
  HTTP to the laptop. Move to HTTPS before any real deployment.
- `com.oculus.supportedDevices` is `quest2|questpro|quest3`, exactly the value
  Meta's samples ship (Quest 3S runs the Quest 3 binary).

## Still unverified in-headset

- Tapping tiles with the controller inside the panel
- The denial tone being audible in VR
- The block → auto-close reaction end to end (teacher taps ХОРИГЛОХ while the
  student has Тоглоом open)
