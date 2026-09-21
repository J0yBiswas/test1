# IVAC OTP Autofill — companion Android app

A tiny, professional-looking Android app that reads the IVAC login SMS, converts the
word-form code (`Four-Two-Six-One-Four-Seven`) into digits (`426147`), and pushes it to
your relay. The **Visa Automator** extension then fills and submits the OTP
automatically. If a code ever fails to arrive, you can still type it into the extension
by hand.

Kotlin + Jetpack Compose (Material 3). **No third-party HTTP library. No READ_SMS.**
The worker URL is fixed in code — the app shows no server field.

---

## Before you build: set the URL once (only place)

Open `app/src/main/java/com/bypassbd/otp/Constants.kt` and confirm `DEFAULT_SERVER` is
your worker's base URL. It is already set to:

```
https://otp-verify.bypassbd.workers.dev
```

Only change it if you ever move the worker. Nothing about the URL is shown or editable
in the app UI.

## Build it

**Android Studio (recommended)**

1. `File ▸ Open…` → select this `OTP-Android-App` folder and let it sync (it downloads
   Gradle 8.9 + the SDK the first time).
2. Press **Run ▶** onto your phone, or `Build ▸ Build APK(s)` and copy
   `app/build/outputs/apk/…/app-…apk` to the phone.

Min Android 8.0 (API 26). Package id: `com.bypassbd.otp`.

### Optional but recommended: a signed release APK

A properly signed release build is trusted more by Android/Play Protect than a debug
one. One time:

```bash
keytool -genkey -v -keystore ivacotp.jks -keyalg RSA -keysize 2048 -validity 10000 -alias ivacotp
```

Create `OTP-Android-App/keystore.properties`:

```
storeFile=../ivacotp.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=ivacotp
keyPassword=YOUR_KEY_PASSWORD
```

Then `Build ▸ Generate Signed Bundle / APK` (or `./gradlew assembleRelease`). If
`keystore.properties` is absent, debug builds still work unchanged.

---

## Pair it (one time)

1. In the **Visa Automator** extension ▸ **CONFIG** ▸ *Phone OTP auto-fill*, copy the
   **Client key** (e.g. `ABCD-1234`) — unique to that computer.
2. In this app enter:
   - **Phone number** — the SIM that receives the IVAC SMS (same number as that
     computer's extension profile).
   - **Client key** — paste it.
3. Tap **SAVE PAIRING**, then **GRANT PERMISSIONS** (Receive SMS + notifications).
4. Tap **Send a test push (0000)** → you should see "Sent ✓".

**Multiple computers?** Each computer's extension has its own client key. Give each
phone's app that computer's key. Routing is `client key + phone`, so codes never cross
between clients.

---

## Getting past Play Protect / device security

Because the app can receive SMS, Google Play Protect and some phones warn when you
install it from outside the Play Store. It is safe to allow — here's how:

- **Install:** when you open the APK, if it says "blocked", tap **More details ▸ Install
  anyway**. Allow your Files/Chrome app under **Settings ▸ Apps ▸ Special access ▸
  Install unknown apps**.
- **Play Protect prompt** ("App not scanned" / "unsafe app"): tap **Install without
  scanning** / **Install anyway**. If it hard-blocks, open **Play Store ▸ profile ▸
  Play Protect ▸ Settings (gear)** and turn **Scan apps with Play Protect** off, install,
  then turn it back on.
- **Xiaomi/MIUI/HyperOS:** turn off **Settings ▸ Privacy protection ▸ Special
  permissions / MIUI optimization**, and in Security app disable the install scanner.
- **Samsung:** disable **Auto Blocker** (Settings ▸ Security and privacy) for the install.
- **Keep it running:** **Settings ▸ Apps ▸ IVAC OTP Autofill ▸ Battery ▸ Unrestricted**
  so it isn't stopped from receiving SMS in the background.

A **signed release APK** (above) triggers fewer of these warnings than a debug build.

---

## Privacy

The app reads only the IVAC OTP SMS (never your other messages — it has no READ_SMS
permission), stores only your phone number and client key on the device, and sends the
OTP code to your own worker over HTTPS. No IVAC password or personal data is stored.
