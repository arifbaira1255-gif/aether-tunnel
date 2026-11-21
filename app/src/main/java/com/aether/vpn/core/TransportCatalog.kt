package com.aether.vpn.core

/**
 * The transport catalogue: TLS server names (SNI), handshake payload templates and the
 * Cloudflare WARP endpoint pool.
 *
 * IMPORTANT — what these actually do:
 *  - [endpoints] are real. The tunnel connects to one of them and the failover engine
 *    measures which one answers from the user's own network.
 *  - [sniHosts] and [payloads] are only carried by a TLS-based transport. A WireGuard
 *    tunnel is plain UDP and has no TLS handshake, so it never sends an SNI or an HTTP
 *    Host header. They are stored on the profile, shown in the UI and exported with it,
 *    and they take effect when the profile is used with a TLS-wrapping relay.
 *  - Whether a given host is zero-rated is decided by the carrier, not by this app.
 */

/** A TLS server name the transport can present. */
data class SniHost(
    val id: String,
    val host: String,
    val label: String,
    val category: SniCategory,
    val note: String = ""
)

enum class SniCategory(val title: String) {
    CLOUDFLARE("Cloudflare & CDN"),
    SOCIAL("Social & messaging"),
    GOOGLE("Google"),
    MICROSOFT("Microsoft"),
    STREAMING("Streaming"),
    COMMERCE("Shopping & payments"),
    TELECOM("Telecom self-care"),
    PUBLIC("Government & education")
}

/** A handshake payload template. [crlf] and [host]/[sni]/[host_port] are substituted. */
data class PayloadPreset(
    val id: String,
    val name: String,
    val template: String,
    val note: String = ""
)

/** One WARP ingress address. */
data class Endpoint(
    val host: String,
    val port: Int,
    val label: String
) {
    val key: String get() = "$host:$port"
}

object TransportCatalog {

    // ---- SNI hosts ---------------------------------------------------------

    val sniHosts: List<SniHost> = listOf(
        // Cloudflare & CDN
        SniHost("cf-engage", "engage.cloudflareclient.com", "WARP ingress", SniCategory.CLOUDFLARE, "The name the WARP client itself uses"),
        SniHost("cf-main", "cloudflare.com", "Cloudflare", SniCategory.CLOUDFLARE),
        SniHost("cf-cdnjs", "cdnjs.cloudflare.com", "cdnjs", SniCategory.CLOUDFLARE),
        SniHost("cf-workers", "workers.cloudflare.com", "Workers", SniCategory.CLOUDFLARE),
        SniHost("cf-speed", "speed.cloudflare.com", "Speed test", SniCategory.CLOUDFLARE),
        SniHost("cf-one", "one.one.one.one", "1.1.1.1", SniCategory.CLOUDFLARE),
        SniHost("cf-dns", "cloudflare-dns.com", "Cloudflare DNS", SniCategory.CLOUDFLARE),
        SniHost("cf-jsdelivr", "cdn.jsdelivr.net", "jsDelivr", SniCategory.CLOUDFLARE),
        SniHost("cf-unpkg", "unpkg.com", "unpkg", SniCategory.CLOUDFLARE),
        SniHost("cf-fastly", "fastly.com", "Fastly", SniCategory.CLOUDFLARE),
        SniHost("cf-akamai", "akamai.com", "Akamai", SniCategory.CLOUDFLARE),

        // Social & messaging
        SniHost("so-whatsapp", "web.whatsapp.com", "WhatsApp Web", SniCategory.SOCIAL),
        SniHost("so-whatsapp2", "whatsapp.com", "WhatsApp", SniCategory.SOCIAL),
        SniHost("so-fb", "m.facebook.com", "Facebook (mobile)", SniCategory.SOCIAL),
        SniHost("so-fb2", "facebook.com", "Facebook", SniCategory.SOCIAL),
        SniHost("so-ig", "instagram.com", "Instagram", SniCategory.SOCIAL),
        SniHost("so-tg", "t.me", "Telegram", SniCategory.SOCIAL),
        SniHost("so-tg2", "telegram.org", "Telegram (org)", SniCategory.SOCIAL),
        SniHost("so-x", "x.com", "X", SniCategory.SOCIAL),
        SniHost("so-tw", "twitter.com", "Twitter", SniCategory.SOCIAL),
        SniHost("so-tt", "tiktok.com", "TikTok", SniCategory.SOCIAL),
        SniHost("so-snap", "snapchat.com", "Snapchat", SniCategory.SOCIAL),
        SniHost("so-li", "linkedin.com", "LinkedIn", SniCategory.SOCIAL),
        SniHost("so-reddit", "reddit.com", "Reddit", SniCategory.SOCIAL),
        SniHost("so-discord", "discord.com", "Discord", SniCategory.SOCIAL),
        SniHost("so-pin", "pinterest.com", "Pinterest", SniCategory.SOCIAL),

        // Google
        SniHost("go-main", "www.google.com", "Google", SniCategory.GOOGLE),
        SniHost("go-gstatic", "www.gstatic.com", "gstatic", SniCategory.GOOGLE),
        SniHost("go-apis", "www.googleapis.com", "Google APIs", SniCategory.GOOGLE),
        SniHost("go-fonts", "fonts.googleapis.com", "Google Fonts", SniCategory.GOOGLE),
        SniHost("go-yt", "www.youtube.com", "YouTube", SniCategory.GOOGLE),
        SniHost("go-ytm", "m.youtube.com", "YouTube (mobile)", SniCategory.GOOGLE),
        SniHost("go-ytimg", "i.ytimg.com", "YouTube images", SniCategory.GOOGLE),
        SniHost("go-play", "play.google.com", "Play Store", SniCategory.GOOGLE),
        SniHost("go-android", "android.com", "Android", SniCategory.GOOGLE),
        SniHost("go-gmail", "mail.google.com", "Gmail", SniCategory.GOOGLE),
        SniHost("go-drive", "drive.google.com", "Drive", SniCategory.GOOGLE),
        SniHost("go-guc", "lh3.googleusercontent.com", "Google user content", SniCategory.GOOGLE),

        // Microsoft
        SniHost("ms-main", "www.microsoft.com", "Microsoft", SniCategory.MICROSOFT),
        SniHost("ms-live", "login.live.com", "Microsoft account", SniCategory.MICROSOFT),
        SniHost("ms-outlook", "outlook.com", "Outlook", SniCategory.MICROSOFT),
        SniHost("ms-office", "www.office.com", "Office", SniCategory.MICROSOFT),
        SniHost("ms-bing", "www.bing.com", "Bing", SniCategory.MICROSOFT),
        SniHost("ms-msn", "www.msn.com", "MSN", SniCategory.MICROSOFT),
        SniHost("ms-azure", "azure.com", "Azure", SniCategory.MICROSOFT),
        SniHost("ms-update", "windowsupdate.com", "Windows Update", SniCategory.MICROSOFT),
        SniHost("ms-skype", "skype.com", "Skype", SniCategory.MICROSOFT),
        SniHost("ms-github", "github.com", "GitHub", SniCategory.MICROSOFT),
        SniHost("ms-ghcdn", "raw.githubusercontent.com", "GitHub raw", SniCategory.MICROSOFT),

        // Streaming
        SniHost("st-netflix", "www.netflix.com", "Netflix", SniCategory.STREAMING),
        SniHost("st-nflx", "ipv4-c001.nflxvideo.net", "Netflix video", SniCategory.STREAMING),
        SniHost("st-spotify", "open.spotify.com", "Spotify", SniCategory.STREAMING),
        SniHost("st-soundcloud", "soundcloud.com", "SoundCloud", SniCategory.STREAMING),
        SniHost("st-vimeo", "vimeo.com", "Vimeo", SniCategory.STREAMING),
        SniHost("st-dailymotion", "www.dailymotion.com", "Dailymotion", SniCategory.STREAMING),
        SniHost("st-twitch", "www.twitch.tv", "Twitch", SniCategory.STREAMING),
        SniHost("st-prime", "www.primevideo.com", "Prime Video", SniCategory.STREAMING),
        SniHost("st-hotstar", "www.hotstar.com", "Hotstar", SniCategory.STREAMING),
        SniHost("st-jio", "www.jiocinema.com", "JioCinema", SniCategory.STREAMING),
        SniHost("st-zee5", "www.zee5.com", "ZEE5", SniCategory.STREAMING),
        SniHost("st-sony", "www.sonyliv.com", "SonyLIV", SniCategory.STREAMING),

        // Shopping & payments
        SniHost("sh-amazon", "www.amazon.com", "Amazon", SniCategory.COMMERCE),
        SniHost("sh-amazonin", "www.amazon.in", "Amazon India", SniCategory.COMMERCE),
        SniHost("sh-flipkart", "www.flipkart.com", "Flipkart", SniCategory.COMMERCE),
        SniHost("sh-ali", "www.aliexpress.com", "AliExpress", SniCategory.COMMERCE),
        SniHost("sh-alibaba", "www.alibaba.com", "Alibaba", SniCategory.COMMERCE),
        SniHost("sh-ebay", "www.ebay.com", "eBay", SniCategory.COMMERCE),
        SniHost("sh-paypal", "www.paypal.com", "PayPal", SniCategory.COMMERCE),
        SniHost("sh-stripe", "js.stripe.com", "Stripe", SniCategory.COMMERCE),
        SniHost("sh-razorpay", "api.razorpay.com", "Razorpay", SniCategory.COMMERCE),
        SniHost("sh-paytm", "www.paytm.com", "Paytm", SniCategory.COMMERCE),
        SniHost("sh-phonepe", "www.phonepe.com", "PhonePe", SniCategory.COMMERCE),
        SniHost("sh-daraz", "www.daraz.pk", "Daraz", SniCategory.COMMERCE),
        SniHost("sh-shopify", "www.shopify.com", "Shopify", SniCategory.COMMERCE),

        // Telecom self-care portals
        SniHost("te-jazz", "www.jazz.com.pk", "Jazz (PK)", SniCategory.TELECOM),
        SniHost("te-telenor", "www.telenor.com.pk", "Telenor (PK)", SniCategory.TELECOM),
        SniHost("te-ufone", "www.ufone.com", "Ufone (PK)", SniCategory.TELECOM),
        SniHost("te-zong", "www.zong.com.pk", "Zong (PK)", SniCategory.TELECOM),
        SniHost("te-airtel", "www.airtel.in", "Airtel (IN)", SniCategory.TELECOM),
        SniHost("te-jio", "www.jio.com", "Jio (IN)", SniCategory.TELECOM),
        SniHost("te-vi", "www.myvi.in", "Vi (IN)", SniCategory.TELECOM),
        SniHost("te-vodafone", "www.vodafone.com", "Vodafone", SniCategory.TELECOM),
        SniHost("te-etisalat", "www.etisalat.ae", "Etisalat (AE)", SniCategory.TELECOM),
        SniHost("te-du", "www.du.ae", "du (AE)", SniCategory.TELECOM),
        SniHost("te-safaricom", "www.safaricom.co.ke", "Safaricom (KE)", SniCategory.TELECOM),
        SniHost("te-mtn", "www.mtn.com", "MTN", SniCategory.TELECOM),
        SniHost("te-glo", "www.gloworld.com", "Glo (NG)", SniCategory.TELECOM),
        SniHost("te-telkom", "www.telkom.co.za", "Telkom (ZA)", SniCategory.TELECOM),
        SniHost("te-celcom", "www.celcom.com.my", "Celcom (MY)", SniCategory.TELECOM),
        SniHost("te-maxis", "www.maxis.com.my", "Maxis (MY)", SniCategory.TELECOM),
        SniHost("te-singtel", "www.singtel.com", "Singtel (SG)", SniCategory.TELECOM),
        SniHost("te-starhub", "www.starhub.com", "StarHub (SG)", SniCategory.TELECOM),
        SniHost("te-smart", "www.smart.com.ph", "Smart (PH)", SniCategory.TELECOM),
        SniHost("te-globe", "www.globe.com.ph", "Globe (PH)", SniCategory.TELECOM),
        SniHost("te-viettel", "viettel.vn", "Viettel (VN)", SniCategory.TELECOM),
        SniHost("te-ais", "www.ais.co.th", "AIS (TH)", SniCategory.TELECOM),
        SniHost("te-true", "www.true.th", "True (TH)", SniCategory.TELECOM),

        // Government & education
        SniHost("pu-nic", "www.nic.in", "NIC (IN)", SniCategory.PUBLIC),
        SniHost("pu-govin", "www.india.gov.in", "India.gov.in", SniCategory.PUBLIC),
        SniHost("pu-pkgov", "www.pakistan.gov.pk", "Pakistan.gov.pk", SniCategory.PUBLIC),
        SniHost("pu-nadra", "www.nadra.gov.pk", "NADRA", SniCategory.PUBLIC),
        SniHost("pu-hec", "www.hec.gov.pk", "HEC", SniCategory.PUBLIC),
        SniHost("pu-mit", "www.mit.edu", "MIT", SniCategory.PUBLIC),
        SniHost("pu-harvard", "www.harvard.edu", "Harvard", SniCategory.PUBLIC),
        SniHost("pu-coursera", "www.coursera.org", "Coursera", SniCategory.PUBLIC),
        SniHost("pu-khan", "www.khanacademy.org", "Khan Academy", SniCategory.PUBLIC),
        SniHost("pu-wiki", "en.wikipedia.org", "Wikipedia", SniCategory.PUBLIC),
        SniHost("pu-archive", "archive.org", "Internet Archive", SniCategory.PUBLIC)
    )

    fun sniById(id: String): SniHost? = sniHosts.firstOrNull { it.id == id }

    // ---- Payload templates -------------------------------------------------

    val payloads: List<PayloadPreset> = listOf(
        PayloadPreset(
            "pl-none", "None (plain WireGuard)", "",
            "No handshake payload — the tunnel speaks WireGuard directly."
        ),
        PayloadPreset(
            "pl-connect", "HTTP CONNECT",
            "CONNECT [host_port] HTTP/1.1[crlf]Host: [host][crlf]Proxy-Connection: keep-alive[crlf][crlf]",
            "The classic proxy CONNECT request."
        ),
        PayloadPreset(
            "pl-connect-sni", "HTTP CONNECT + SNI host",
            "CONNECT [host_port] HTTP/1.1[crlf]Host: [sni][crlf]Proxy-Connection: keep-alive[crlf][crlf]",
            "CONNECT with the chosen SNI host in the Host header."
        ),
        PayloadPreset(
            "pl-ws", "WebSocket upgrade",
            "GET / HTTP/1.1[crlf]Host: [host][crlf]Upgrade: websocket[crlf]Connection: Upgrade[crlf]Sec-WebSocket-Key: [random][crlf]Sec-WebSocket-Version: 13[crlf][crlf]",
            "Looks like a WebSocket handshake."
        ),
        PayloadPreset(
            "pl-get", "GET keep-alive",
            "GET / HTTP/1.1[crlf]Host: [host][crlf]Connection: keep-alive[crlf]User-Agent: [ua][crlf][crlf]",
            "A plain HTTP GET with a browser user agent."
        ),
        PayloadPreset(
            "pl-post", "POST with body",
            "POST / HTTP/1.1[crlf]Host: [host][crlf]Content-Type: application/octet-stream[crlf]Content-Length: 0[crlf][crlf]",
            "POST with an empty body."
        ),
        PayloadPreset(
            "pl-cf", "Cloudflare fronting",
            "GET /cdn-cgi/trace HTTP/1.1[crlf]Host: [sni][crlf]User-Agent: [ua][crlf]Accept: */*[crlf][crlf]",
            "Requests a Cloudflare trace endpoint through the chosen SNI."
        ),
        PayloadPreset(
            "pl-http2", "HTTP/2 style preface",
            "PRI * HTTP/2.0[crlf][crlf]SM[crlf][crlf]",
            "The HTTP/2 connection preface."
        ),
        PayloadPreset(
            "pl-custom", "Custom (edit below)", "",
            "Write your own template using [crlf], [host], [sni], [host_port], [ua], [random]."
        )
    )

    fun payloadById(id: String): PayloadPreset? = payloads.firstOrNull { it.id == id }

    /** Substitutes the template placeholders. */
    fun renderPayload(
        template: String,
        host: String,
        port: Int,
        sni: String,
        userAgent: String = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36"
    ): String {
        if (template.isBlank()) return ""
        return template
            .replace("[crlf]", "\r\n")
            .replace("[host_port]", "$host:$port")
            .replace("[host]", host)
            .replace("[sni]", sni)
            .replace("[ua]", userAgent)
            .replace("[random]", java.util.UUID.randomUUID().toString().replace("-", "").take(22))
    }

    // ---- Endpoint pool -----------------------------------------------------

    /**
     * Cloudflare's published WARP ingress ranges. Every address below is inside
     * 162.159.192.0/24, 162.159.193.0/24, 162.159.195.0/24 or 188.114.96.0/22, and every
     * port is one Cloudflare documents for WARP (2408 default, plus the 500 / 1701 / 4500
     * fallbacks). Which of them answers depends on the network the phone is on — that is
     * exactly what the failover engine measures.
     */
    val endpoints: List<Endpoint> = buildList {
        val hosts = listOf(
            "162.159.192.1", "162.159.192.2", "162.159.192.3", "162.159.192.4",
            "162.159.193.1", "162.159.193.2", "162.159.193.3", "162.159.193.4",
            "162.159.195.1", "162.159.195.2", "162.159.195.3", "162.159.195.4",
            "188.114.96.1", "188.114.96.2", "188.114.97.1", "188.114.97.2",
            "188.114.98.1", "188.114.98.2", "188.114.99.1", "188.114.99.2"
        )
        val ports = listOf(2408, 500, 1701, 4500)
        hosts.forEach { h ->
            ports.forEach { p ->
                add(Endpoint(h, p, if (p == 2408) "Primary" else "Fallback $p"))
            }
        }
    }

    /** The default order: primary port first, then the fallbacks. */
    fun defaultOrder(): List<Endpoint> =
        endpoints.sortedWith(compareBy({ if (it.port == 2408) 0 else 1 }, { it.host }))
}
