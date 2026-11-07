# Gatehouse Android app

Flutter client for the Gatehouse demo API. One Flutter codebase supports the
Security and Resident roles. This project currently includes the Android
platform; iOS can be added later without rewriting the Dart application.

## Requirements

- Flutter SDK (stable)
- Android Studio or Android SDK command-line tools
- Android emulator or USB-debugging-enabled Android device

## Run

From this directory:

```sh
flutter pub get
flutter run
```

The app uses `https://gatehouse-demo.onrender.com` by default. Override it for
local development with:

```sh
flutter run --dart-define=GATEHOUSE_API_BASE=http://10.0.2.2:8080
```

`10.0.2.2` is the Android emulator's route to the host computer. For a physical
device, use a development API hostname reachable from that device.

## Demo accounts

- Security: `guard01` / `demo123`
- Resident: `ananya@example.com` / `demo123`

This demo API uses HTTP Basic authentication. Credentials are held in app
memory only and sent over HTTPS in deployed use. Do not use real resident or
visitor information with the demo service. Replace demo authentication before
production use.

## Android release

Create a signed Android App Bundle after configuring a private upload key and
release signing in `android/app/build.gradle.kts`:

```sh
flutter build appbundle --release
```

Do not commit signing keys or passwords. The current debug signing setup is
only suitable for local runs.
