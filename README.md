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
  (rx/tx bytes), not from a timer. The peer's `latestHandshakeEpochMillis()` is read
  too, so the app can tell a live tunnel from a stalled one.
- **The endpoint pool.** 80 Cloudflare WARP ingress addresses (20 hosts × 4 ports),
  all inside Cloudflare's published ranges. Which one answers depends on the network
  the phone is on — the app measures that rather than guessing.
- **Persistence.** Registration, chosen edge, favourites, session history and
  preferences are stored on the device.

## What is not real, and is labelled as such

- **SNI hosts and payload templates do not affect a WireGuard tunnel.** WireGuard is
  plain UDP: it has no TLS handshake, so it never sends an SNI or an HTTP `Host`
  header. The 100+ hosts and the payload presets are stored on the profile, shown in
  the UI and exported with it (`# SNI = …` comments in the wg-quick form), and they
  take effect when the profile is used with a TLS-wrapping relay. The app says this on
  the Transport screen rather than implying otherwise.
- **Zero-rating is the carrier's decision, not the app's.** Every host in the Pakistan
  sections was DNS-verified to resolve; whether a given carrier bills it as free is
  something only that carrier knows.
- **A VPN does not create a data allowance.** It encrypts and routes traffic. The
  device still needs a working connection (Wi-Fi or mobile data) for the tunnel to
  carry anything.

## Screens

| Screen | What it does |
|---|---|
| Connect | Oversized circular power button with breathing glow rings, orbiting particles and a rotating conic ring; live session timer, live throughput with a real-sample speed graph, current endpoint, carrier badge, failover log |
| Servers | The 80-endpoint pool with measured handshake latency, reachability dots, search, Reachable/Favourites filters |
| Transport | Carrier profiles (Auto / Jazz / Telenor / Ufone / Zong / Other), 100+ SNI hosts in 14 categories, 20 payload templates with a custom editor, the endpoint pool with a real probe |
| Usage | Lifetime totals, a 7-day data chart, top servers by data, full session history |
| Settings | Carrier, auto-connect, kill switch, auto-failover, auto-MTU, watchdog, DNS presets, MTU, protocol and encryption detail, about |

## How a connection actually comes up

1. **Register** — a WARP device is created once and cached.
2. **Probe** — each candidate endpoint gets a real WireGuard handshake-initiation
   packet (type 1); a peer that is alive replies with type 2/3. A blocked port is
   skipped in about a second instead of hanging the connect button.
3. **MTU ladder** — each reachable endpoint is tried at 1420 → 1380 → 1280 → 1240 →
   1200. A network that silently drops large packets accepts the handshake and then
   stalls; stepping the MTU down is what gets past it. The value that worked is
   remembered, so the next connect starts there.
4. **Watchdog** — once up, a stale handshake or a tunnel that has moved no bytes
   triggers a reconnect.

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
