# Aether Tunnel

A premium Android VPN client with a **real** WireGuard tunnel.

## What is real

- **The tunnel.** The app runs the WireGuard project's Go userspace implementation
  inside Android's `VpnService` (`com.wireguard.android:tunnel`). When the connect
  button turns mint, the device's traffic is genuinely routed through the peer.
- **The servers.** On first connect the app registers a free, unlimited Cloudflare
  WARP device (`api.cloudflareclient.com`) and builds a real WireGuard profile from
  the response — real peer public key, real endpoint, real keypair. No reseller, no
  API key, no account of ours in the middle.
- **The stats.** Download/upload counters come from the tunnel's own `Statistics`
  (rx/tx bytes), not from a timer.
- **Persistence.** Registration, chosen edge, favourites, session history and
  preferences are stored on the device.

## Screens

| Screen | What it does |
|---|---|
| Connect | Oversized circular power button with breathing glow rings and a rotating conic ring; live session timer, live throughput, current server, honest status note |
| Servers | The Cloudflare edge list with flags, ping, colour-coded load bars, tier badges, search and favourites |
| Usage | Lifetime totals, top servers by data, full session history |
| Settings | Auto-connect, kill switch, MTU, DNS, SNI, host header, payload, protocol, about |

## Build the APK

The APK is built by GitHub Actions (`.github/workflows/build-apk.yml`) on every push
to `main`: JDK 17, Gradle 8.7, a generated release keystore, then the APK is uploaded
as the `aether-tunnel-apk` artifact.

To build locally instead:

```bash
./gradlew :app:assembleRelease
```

## Permissions

- `INTERNET`, `ACCESS_NETWORK_STATE` — the tunnel itself
- `FOREGROUND_SERVICE`, `POST_NOTIFICATIONS` — the VPN foreground service
- Android will additionally ask for the system VPN consent dialog on first connect.

## Notes

- The first connection shows Android's VPN consent dialog. Accept it once.
- `minSdk 24`, `targetSdk 34`.
- The debug build installs alongside the release build (`applicationIdSuffix .debug`).
