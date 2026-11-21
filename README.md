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
- **The endpoint pool.** 80 Cloudflare WARP ingress addresses (20 hosts x ports
  2408 / 500 / 1701 / 4500). The app probes each one with a real WireGuard
  handshake-initiation packet and keeps the ones that answer from your network.
- **Persistence.** Registration, chosen endpoint, favourites, session history and
  preferences are stored on the device.

## Screens

| Screen | What it does |
|---|---|
| Connect | Oversized circular power button with breathing glow rings and a rotating conic ring; live session timer, live throughput, current server, failover progress, honest status note |
| Servers | The endpoint pool with measured latency, reachability dots, PRIMARY/FALLBACK badges, search, Reachable and Favourites filters |
| Transport | 100+ SNI hosts in 8 categories with search and category filter; 9 payload templates plus a custom editor; the 80-endpoint pool with a Test all button |
| Usage | Lifetime totals, top servers by data, full session history |
| Settings | Auto-connect, kill switch, MTU, DNS, active transport summary, protocol, about |

## Transport: SNI, payload and endpoints

- **SNI hosts** — 100+ names across Cloudflare/CDN, social, Google, Microsoft,
  streaming, commerce, telecom self-care and public-sector categories. Pick one in
  Transport, or type your own.
- **Payload templates** — HTTP CONNECT, CONNECT + SNI, WebSocket upgrade, GET
  keep-alive, POST, Cloudflare fronting, HTTP/2 preface, plus a custom editor.
  Placeholders: `[crlf] [host] [sni] [host_port] [ua] [random]`.
- **Endpoints** — 80 WARP ingress addresses. Tap **Test all** to probe them from
  your own network; the fastest that answers becomes the active endpoint.
- **Auto-failover** — on connect, the app tries the selected endpoint first, then
  the rest of the pool (primary port before fallbacks), probing each before it
  connects. The first one that comes up wins and is remembered.

### What SNI and payload actually do here

A WireGuard tunnel is plain UDP and has no TLS handshake, so it never sends an SNI
or an HTTP `Host` header. The SNI host and payload are stored on the profile, shown
in the UI and exported with it (as `# SNI = …` / `# Payload = …` comments in the
wg-quick text), and they take effect when the profile is used with a TLS-wrapping
relay. Whether a given host is zero-rated is decided by the carrier, not by this app.

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
