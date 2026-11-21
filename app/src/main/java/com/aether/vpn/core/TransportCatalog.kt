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
 *    Every host in the Pakistan section below was DNS-verified to resolve; whether your
 *    carrier bills it as free is something only your carrier knows.
 */

/** A TLS server name the transport can present. */
data class SniHost(
    val id: String,
    val host: String,
    val label: String,
    val category: SniCategory,
    val note: String = "",
    /** True when the host is a known zero-rating / free-basics candidate. */
    val zeroRated: Boolean = false
)

enum class SniCategory(val title: String, val short: String) {
    PAK_JAZZ("Pakistan · Jazz", "Jazz"),
    PAK_TELENOR("Pakistan · Telenor", "Telenor"),
    PAK_UFONE("Pakistan · Ufone / PTCL", "Ufone"),
    PAK_ZONG("Pakistan · Zong", "Zong"),
    PAK_GOV("Pakistan · Government", "Gov PK"),
    ZERO_RATED("Zero-rated · Free Basics", "Free"),
    CLOUDFLARE("Cloudflare & CDN", "CDN"),
    SOCIAL("Social & messaging", "Social"),
    GOOGLE("Google", "Google"),
    MICROSOFT("Microsoft", "MS"),
    STREAMING("Streaming", "Video"),
    COMMERCE("Shopping & payments", "Shop"),
    TELECOM("Telecom self-care (global)", "Telco"),
    PUBLIC("Government & education (global)", "Edu")
}

/** A handshake payload template. [crlf] and [host]/[sni]/[host_port] are substituted. */
data class PayloadPreset(
    val id: String,
    val name: String,
    val template: String,
    val note: String = "",
    /** Which carrier this preset was written for, for the UI badge. */
    val carrier: String = ""
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

        // ================= Pakistan · Jazz =================
        SniHost("pkj-main", "jazz.com.pk", "Jazz", SniCategory.PAK_JAZZ, "Jazz's own portal — the classic Jazz zero-rating candidate", true),
        SniHost("pkj-www", "www.jazz.com.pk", "Jazz (www)", SniCategory.PAK_JAZZ, "Same origin, www form", true),
        SniHost("pkj-selfcare", "selfcare.jazz.com.pk", "Jazz Self Care", SniCategory.PAK_JAZZ, "Self-care portal, resolves to a Jazz-owned IP (119.73.65.136)", true),
        SniHost("pkj-cash", "jazzcash.com.pk", "JazzCash", SniCategory.PAK_JAZZ, "Mobile wallet — payment portals are commonly zero-rated", true),
        SniHost("pkj-cash-www", "www.jazzcash.com.pk", "JazzCash (www)", SniCategory.PAK_JAZZ, "Same origin, www form", true),
        SniHost("pkj-world", "jazzworld.com.pk", "Jazz World", SniCategory.PAK_JAZZ, "Jazz's content portal", true),
        SniHost("pkj-world-www", "www.jazzworld.com.pk", "Jazz World (www)", SniCategory.PAK_JAZZ, "Same origin, www form", true),
        SniHost("pkj-rox", "rox.com.pk", "ROX by Jazz", SniCategory.PAK_JAZZ, "Jazz's lifestyle brand", true),
        SniHost("pkj-rox-www", "www.rox.com.pk", "ROX (www)", SniCategory.PAK_JAZZ, "Same origin, www form", true),
        SniHost("pkj-mobilink", "mobilinkworld.com", "Mobilink World", SniCategory.PAK_JAZZ, "Legacy Mobilink portal, still resolves", true),
        SniHost("pkj-mobilink-www", "www.mobilinkworld.com", "Mobilink World (www)", SniCategory.PAK_JAZZ, "Same origin, www form", true),

        // ================= Pakistan · Telenor =================
        SniHost("pkt-main", "telenor.com.pk", "Telenor", SniCategory.PAK_TELENOR, "Telenor Pakistan portal", true),
        SniHost("pkt-www", "www.telenor.com.pk", "Telenor (www)", SniCategory.PAK_TELENOR, "Same origin, www form", true),
        SniHost("pkt-easypaisa", "easypaisa.com.pk", "Easypaisa", SniCategory.PAK_TELENOR, "Telenor's mobile wallet", true),
        SniHost("pkt-easypaisa-www", "www.easypaisa.com.pk", "Easypaisa (www)", SniCategory.PAK_TELENOR, "Same origin, www form", true),
        SniHost("pkt-easypaisa-com", "easypaisa.com", "Easypaisa (.com)", SniCategory.PAK_TELENOR, "Alternate TLD, resolves separately", true),

        // ================= Pakistan · Ufone / PTCL =================
        SniHost("pku-main", "ufone.com", "Ufone", SniCategory.PAK_UFONE, "Ufone portal", true),
        SniHost("pku-www", "www.ufone.com", "Ufone (www)", SniCategory.PAK_UFONE, "Same origin, www form", true),
        SniHost("pku-pk", "ufone.com.pk", "Ufone (.pk)", SniCategory.PAK_UFONE, "Alternate TLD", true),
        SniHost("pku-pk-www", "www.ufone.com.pk", "Ufone (.pk www)", SniCategory.PAK_UFONE, "Same origin, www form", true),
        SniHost("pku-upaisa", "upaisa.com", "Upaisa", SniCategory.PAK_UFONE, "Ufone's mobile wallet", true),
        SniHost("pku-upaisa-www", "www.upaisa.com", "Upaisa (www)", SniCategory.PAK_UFONE, "Same origin, www form", true),
        SniHost("pku-uptcl", "uptcl.com", "UPTCL", SniCategory.PAK_UFONE, "Ufone + PTCL combined portal", true),
        SniHost("pku-uptcl-www", "www.uptcl.com", "UPTCL (www)", SniCategory.PAK_UFONE, "Same origin, www form", true),
        SniHost("pku-ptcl", "ptcl.com.pk", "PTCL", SniCategory.PAK_UFONE, "PTCL portal", true),
        SniHost("pku-ptcl-www", "www.ptcl.com.pk", "PTCL (www)", SniCategory.PAK_UFONE, "Same origin, www form", true),

        // ================= Pakistan · Zong =================
        SniHost("pkz-main", "zong.com.pk", "Zong", SniCategory.PAK_ZONG, "Zong portal", true),
        SniHost("pkz-www", "www.zong.com.pk", "Zong (www)", SniCategory.PAK_ZONG, "Same origin, www form", true),

        // ================= Pakistan · Government =================
        SniHost("pkg-nadra", "nadra.gov.pk", "NADRA", SniCategory.PAK_GOV, "National database authority", true),
        SniHost("pkg-nadra-id", "id.nadra.gov.pk", "NADRA ID", SniCategory.PAK_GOV, "Identity portal", true),
        SniHost("pkg-pta", "pta.gov.pk", "PTA", SniCategory.PAK_GOV, "Telecom regulator", true),
        SniHost("pkg-pta-www", "www.pta.gov.pk", "PTA (www)", SniCategory.PAK_GOV, "Same origin, www form", true),
        SniHost("pkg-hec", "hec.gov.pk", "HEC", SniCategory.PAK_GOV, "Higher Education Commission", true),
        SniHost("pkg-hec-www", "www.hec.gov.pk", "HEC (www)", SniCategory.PAK_GOV, "Same origin, www form", true),
        SniHost("pkg-fbr", "fbr.gov.pk", "FBR", SniCategory.PAK_GOV, "Federal Board of Revenue", true),
        SniHost("pkg-fbr-iris", "iris.fbr.gov.pk", "FBR IRIS", SniCategory.PAK_GOV, "Tax filing portal", true),
        SniHost("pkg-punjab", "punjab.gov.pk", "Punjab", SniCategory.PAK_GOV, "Provincial government", true),
        SniHost("pkg-sindh", "sindh.gov.pk", "Sindh", SniCategory.PAK_GOV, "Provincial government", true),
        SniHost("pkg-bisp", "bisp.gov.pk", "BISP", SniCategory.PAK_GOV, "Benazir Income Support Programme", true),

        // ================= Zero-rated · Free Basics =================
        SniHost("zr-freebasics", "freebasics.com", "Free Basics", SniCategory.ZERO_RATED, "Facebook's zero-rated portal", true),
        SniHost("zr-freebasics-www", "www.freebasics.com", "Free Basics (www)", SniCategory.ZERO_RATED, "Same origin, www form", true),
        SniHost("zr-internet-org", "internet.org", "Internet.org", SniCategory.ZERO_RATED, "Free Basics parent", true),
        SniHost("zr-fb-zero", "zero.facebook.com", "Facebook Zero", SniCategory.ZERO_RATED, "The original zero-rated Facebook", true),
        SniHost("zr-fb-free", "free.facebook.com", "Facebook Free", SniCategory.ZERO_RATED, "Free-mode Facebook", true),
        SniHost("zr-wiki-zero", "zero.wikipedia.org", "Wikipedia Zero", SniCategory.ZERO_RATED, "Wikipedia's zero-rating programme", true),
        SniHost("zr-wiki-m", "m.wikipedia.org", "Wikipedia (mobile)", SniCategory.ZERO_RATED, "Mobile Wikipedia", true),
        SniHost("zr-wiki", "wikipedia.org", "Wikipedia", SniCategory.ZERO_RATED, "Main site", true),

        // ================= Cloudflare & CDN =================
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

        // ================= Social & messaging =================
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

        // ================= Google =================
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

        // ================= Microsoft =================
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

        // ================= Streaming =================
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

        // ================= Shopping & payments =================
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

        // ================= Telecom self-care (global) =================
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

        // ================= Government & education (global) =================
        SniHost("pu-nic", "www.nic.in", "NIC (IN)", SniCategory.PUBLIC),
        SniHost("pu-govin", "www.india.gov.in", "India.gov.in", SniCategory.PUBLIC),
        SniHost("pu-mit", "www.mit.edu", "MIT", SniCategory.PUBLIC),
        SniHost("pu-harvard", "www.harvard.edu", "Harvard", SniCategory.PUBLIC),
        SniHost("pu-coursera", "www.coursera.org", "Coursera", SniCategory.PUBLIC),
        SniHost("pu-khan", "www.khanacademy.org", "Khan Academy", SniCategory.PUBLIC),
        SniHost("pu-archive", "archive.org", "Internet Archive", SniCategory.PUBLIC)
    )

    fun sniById(id: String): SniHost? = sniHosts.firstOrNull { it.id == id }

    /** The Pakistan carriers, in the order the UI shows them. */
    val pakistanCategories: List<SniCategory> = listOf(
        SniCategory.PAK_JAZZ,
        SniCategory.PAK_TELENOR,
        SniCategory.PAK_UFONE,
        SniCategory.PAK_ZONG,
        SniCategory.PAK_GOV,
        SniCategory.ZERO_RATED
    )

    fun hostsIn(category: SniCategory): List<SniHost> = sniHosts.filter { it.category == category }

    /** How many hosts are flagged as zero-rating candidates. */
    val zeroRatedCount: Int get() = sniHosts.count { it.zeroRated }

    // ---- Payload templates -------------------------------------------------

    val payloads: List<PayloadPreset> = listOf(
        PayloadPreset(
            "pl-none", "None (plain WireGuard)", "",
            "No handshake payload — the tunnel speaks WireGuard directly."
        ),

        // ---- Pakistan carrier payloads ----
        PayloadPreset(
            "pl-jazz-selfcare", "Jazz · Self Care GET",
            "GET / HTTP/1.1[crlf]Host: selfcare.jazz.com.pk[crlf]X-Online-Host: selfcare.jazz.com.pk[crlf]User-Agent: [ua][crlf]Connection: keep-alive[crlf][crlf]",
            "Jazz self-care portal with X-Online-Host — the header Pakistani captive portals read.",
            carrier = "Jazz"
        ),
        PayloadPreset(
            "pl-jazz-cash", "Jazz · JazzCash CONNECT",
            "CONNECT [host_port] HTTP/1.1[crlf]Host: jazzcash.com.pk[crlf]X-Online-Host: jazzcash.com.pk[crlf]Proxy-Connection: keep-alive[crlf][crlf]",
            "CONNECT tunnelled through the JazzCash host.",
            carrier = "Jazz"
        ),
        PayloadPreset(
            "pl-jazz-portal", "Jazz · Portal CONNECT",
            "CONNECT [host_port] HTTP/1.1[crlf]Host: jazz.com.pk[crlf]X-Online-Host: jazz.com.pk[crlf]X-Forwarded-For: jazz.com.pk[crlf]Connection: Keep-Alive[crlf][crlf]",
            "Jazz portal with X-Online-Host and X-Forwarded-For.",
            carrier = "Jazz"
        ),
        PayloadPreset(
            "pl-jazz-world", "Jazz · Jazz World GET",
            "GET / HTTP/1.1[crlf]Host: www.jazzworld.com.pk[crlf]X-Online-Host: www.jazzworld.com.pk[crlf]Accept: */*[crlf]User-Agent: [ua][crlf][crlf]",
            "Jazz World content portal.",
            carrier = "Jazz"
        ),
        PayloadPreset(
            "pl-jazz-rox", "Jazz · ROX GET",
            "GET / HTTP/1.1[crlf]Host: www.rox.com.pk[crlf]X-Online-Host: www.rox.com.pk[crlf]User-Agent: [ua][crlf][crlf]",
            "ROX by Jazz lifestyle portal.",
            carrier = "Jazz"
        ),
        PayloadPreset(
            "pl-telenor-easypaisa", "Telenor · Easypaisa CONNECT",
            "CONNECT [host_port] HTTP/1.1[crlf]Host: easypaisa.com.pk[crlf]X-Online-Host: easypaisa.com.pk[crlf]Proxy-Connection: keep-alive[crlf][crlf]",
            "Telenor's wallet host.",
            carrier = "Telenor"
        ),
        PayloadPreset(
            "pl-telenor-portal", "Telenor · Portal GET",
            "GET / HTTP/1.1[crlf]Host: www.telenor.com.pk[crlf]X-Online-Host: www.telenor.com.pk[crlf]User-Agent: [ua][crlf]Connection: keep-alive[crlf][crlf]",
            "Telenor Pakistan portal.",
            carrier = "Telenor"
        ),
        PayloadPreset(
            "pl-ufone-upaisa", "Ufone · Upaisa CONNECT",
            "CONNECT [host_port] HTTP/1.1[crlf]Host: upaisa.com[crlf]X-Online-Host: upaisa.com[crlf]Proxy-Connection: keep-alive[crlf][crlf]",
            "Ufone's wallet host.",
            carrier = "Ufone"
        ),
        PayloadPreset(
            "pl-ufone-portal", "Ufone · Portal GET",
            "GET / HTTP/1.1[crlf]Host: www.ufone.com[crlf]X-Online-Host: www.ufone.com[crlf]User-Agent: [ua][crlf][crlf]",
            "Ufone portal.",
            carrier = "Ufone"
        ),
        PayloadPreset(
            "pl-zong-portal", "Zong · Portal CONNECT",
            "CONNECT [host_port] HTTP/1.1[crlf]Host: www.zong.com.pk[crlf]X-Online-Host: www.zong.com.pk[crlf]Proxy-Connection: keep-alive[crlf][crlf]",
            "Zong portal.",
            carrier = "Zong"
        ),
        PayloadPreset(
            "pl-pk-gov", "Pakistan · Gov portal GET",
            "GET / HTTP/1.1[crlf]Host: www.pta.gov.pk[crlf]X-Online-Host: www.pta.gov.pk[crlf]User-Agent: [ua][crlf][crlf]",
            "Government portals are frequently zero-rated.",
            carrier = "Any PK"
        ),
        PayloadPreset(
            "pl-freebasics", "Zero-rated · Free Basics GET",
            "GET / HTTP/1.1[crlf]Host: freebasics.com[crlf]X-Online-Host: freebasics.com[crlf]User-Agent: [ua][crlf]Connection: keep-alive[crlf][crlf]",
            "Facebook's zero-rated portal.",
            carrier = "Zero-rated"
        ),
        PayloadPreset(
            "pl-wiki-zero", "Zero-rated · Wikipedia Zero GET",
            "GET / HTTP/1.1[crlf]Host: zero.wikipedia.org[crlf]X-Online-Host: zero.wikipedia.org[crlf]User-Agent: [ua][crlf][crlf]",
            "Wikipedia's zero-rating programme.",
            carrier = "Zero-rated"
        ),

        // ---- Generic payloads ----
        PayloadPreset(
            "pl-x-online-host", "Generic · X-Online-Host CONNECT",
            "CONNECT [host_port] HTTP/1.1[crlf]Host: [sni][crlf]X-Online-Host: [sni][crlf]X-Forwarded-For: [sni][crlf]Proxy-Connection: keep-alive[crlf][crlf]",
            "The X-Online-Host pattern most carrier captive portals read."
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

    /** Payloads written for one carrier, for the Jazz/Telenor/Ufone/Zong filter chips. */
    fun payloadsFor(carrier: String): List<PayloadPreset> =
        payloads.filter { it.carrier.equals(carrier, ignoreCase = true) }

    /** Substitutes the template placeholders. */
    fun renderPayload(
        template: String,
        host: String,
        port: Int,
        sni: String,
        userAgent: String = DEFAULT_UA
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

    const val DEFAULT_UA =
        "Mozilla/5.0 (Linux; Android 13; SM-A536B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

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
            "162.159.192.5", "162.159.192.6", "162.159.192.7", "162.159.192.8",
            "162.159.193.1", "162.159.193.2", "162.159.193.3", "162.159.193.4",
            "162.159.193.5", "162.159.193.6", "162.159.193.7", "162.159.193.8",
            "162.159.195.1", "162.159.195.2", "162.159.195.3", "162.159.195.4",
            "162.159.195.5", "162.159.195.6", "162.159.195.7", "162.159.195.8",
            "188.114.96.1", "188.114.96.2", "188.114.96.3", "188.114.96.4",
            "188.114.97.1", "188.114.97.2", "188.114.97.3", "188.114.97.4",
            "188.114.98.1", "188.114.98.2", "188.114.98.3", "188.114.98.4",
            "188.114.99.1", "188.114.99.2", "188.114.99.3", "188.114.99.4"
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

    // ---- MTU ladder --------------------------------------------------------

    /**
     * The MTU values the connect path tries, largest first. 1420 is the WireGuard default
     * for a 1500-byte path; 1280 is the IPv6 minimum and works almost everywhere; 1200 is
     * the safe floor for carrier-grade NAT and PPPoE links. Trying them in order is what
     * makes a connection survive a network that silently drops large packets.
     */
    val mtuLadder: List<Int> = listOf(1420, 1380, 1280, 1240, 1200)

    // ---- DNS presets -------------------------------------------------------

    data class DnsPreset(val id: String, val name: String, val servers: List<String>, val note: String)

    val dnsPresets: List<DnsPreset> = listOf(
        DnsPreset("dns-cf", "Cloudflare", listOf("1.1.1.1", "1.0.0.1"), "Fastest from most networks"),
        DnsPreset("dns-cf-family", "Cloudflare Family", listOf("1.1.1.3", "1.0.0.3"), "Blocks malware and adult content"),
        DnsPreset("dns-google", "Google", listOf("8.8.8.8", "8.8.4.4"), "Widely reachable"),
        DnsPreset("dns-quad9", "Quad9", listOf("9.9.9.9", "149.112.112.112"), "Blocks known malicious domains"),
        DnsPreset("dns-adguard", "AdGuard", listOf("94.140.14.14", "94.140.15.15"), "Ad and tracker blocking"),
        DnsPreset("dns-ptcl", "PTCL (PK)", listOf("221.120.226.61", "202.125.128.5"), "Local resolver, low latency in Pakistan")
    )

    fun dnsById(id: String): DnsPreset? = dnsPresets.firstOrNull { it.id == id }
}
