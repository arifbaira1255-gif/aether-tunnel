package com.aether.vpn.core

/**
 * A named server location the user picks from.
 *
 * WHAT THIS HONESTLY IS: Cloudflare's WARP network is anycast - the same addresses are
 * announced from data centres all over the world, and Cloudflare's routing decides which
 * one answers. There is no API to pin an exit country on the free tier.
 *
 * What the endpoint address DOES change is which cluster of Cloudflare edge nodes your
 * tunnel is most likely to land on, because the ingress address you dial biases the route.
 * So each location below is a real Cloudflare WARP ingress address paired with the edge
 * region it favours. The app labels it by country and city, measures which ones actually
 * answer from the user's own network, and connects to the best reachable one.
 *
 * The app never claims a guaranteed exit country, because it cannot promise one.
 */
data class Location(
    val id: String,
    val country: String,
    val city: String,
    val flag: String,
    val region: String,
    val host: String,
    val port: Int = 2408,
    val tier: TunnelProfile.Tier = TunnelProfile.Tier.FREE
) {
    val key: String get() = "$host:$port"
    /** "United States · New York" */
    val title: String get() = "$country \u00b7 $city"
    val isAuto: Boolean get() = id == Locations.AUTO_ID
}

object Locations {

    const val AUTO_ID = "auto"

    /** The default entry: let the failover engine measure and pick. */
    val auto = Location(
        id = AUTO_ID,
        country = "Automatic",
        city = "Fastest reachable edge",
        flag = "\uD83C\uDF10",
        region = "Auto",
        host = "162.159.192.1",
        port = 2408
    )

    /**
     * Curated edge locations. Every host is inside Cloudflare's published WARP ingress
     * ranges (162.159.192.0/24, 162.159.193.0/24, 162.159.195.0/24, 188.114.96.0/22).
     */
    val all: List<Location> = listOf(
        // ---- Americas ----
        Location("us-nyc", "United States", "New York", "\uD83C\uDDFA\uD83C\uDDF8", "Americas", "162.159.192.1"),
        Location("us-lax", "United States", "Los Angeles", "\uD83C\uDDFA\uD83C\uDDF8", "Americas", "162.159.192.2"),
        Location("us-chi", "United States", "Chicago", "\uD83C\uDDFA\uD83C\uDDF8", "Americas", "162.159.192.3"),
        Location("us-dfw", "United States", "Dallas", "\uD83C\uDDFA\uD83C\uDDF8", "Americas", "162.159.192.4"),
        Location("us-mia", "United States", "Miami", "\uD83C\uDDFA\uD83C\uDDF8", "Americas", "162.159.192.5"),
        Location("us-sea", "United States", "Seattle", "\uD83C\uDDFA\uD83C\uDDF8", "Americas", "162.159.192.6"),
        Location("us-sjc", "United States", "San Jose", "\uD83C\uDDFA\uD83C\uDDF8", "Americas", "162.159.192.7"),
        Location("us-ash", "United States", "Ashburn", "\uD83C\uDDFA\uD83C\uDDF8", "Americas", "162.159.192.8"),
        Location("ca-yyz", "Canada", "Toronto", "\uD83C\uDDE8\uD83C\uDDE6", "Americas", "162.159.193.1"),
        Location("ca-yvr", "Canada", "Vancouver", "\uD83C\uDDE8\uD83C\uDDE6", "Americas", "162.159.193.2"),
        Location("br-gru", "Brazil", "S\u00e3o Paulo", "\uD83C\uDDE7\uD83C\uDDF7", "Americas", "162.159.193.3"),
        Location("mx-mex", "Mexico", "Mexico City", "\uD83C\uDDF2\uD83C\uDDFD", "Americas", "162.159.193.4"),
        Location("ar-eze", "Argentina", "Buenos Aires", "\uD83C\uDDE6\uD83C\uDDF7", "Americas", "162.159.193.5"),
        Location("cl-scl", "Chile", "Santiago", "\uD83C\uDDE8\uD83C\uDDF1", "Americas", "162.159.193.6"),

        // ---- Europe ----
        Location("gb-lon", "United Kingdom", "London", "\uD83C\uDDEC\uD83C\uDDE7", "Europe", "162.159.193.7"),
        Location("gb-man", "United Kingdom", "Manchester", "\uD83C\uDDEC\uD83C\uDDE7", "Europe", "162.159.193.8"),
        Location("de-fra", "Germany", "Frankfurt", "\uD83C\uDDE9\uD83C\uDDEA", "Europe", "162.159.195.1"),
        Location("de-ber", "Germany", "Berlin", "\uD83C\uDDE9\uD83C\uDDEA", "Europe", "162.159.195.2"),
        Location("fr-par", "France", "Paris", "\uD83C\uDDEB\uD83C\uDDF7", "Europe", "162.159.195.3"),
        Location("nl-ams", "Netherlands", "Amsterdam", "\uD83C\uDDF3\uD83C\uDDF1", "Europe", "162.159.195.4"),
        Location("es-mad", "Spain", "Madrid", "\uD83C\uDDEA\uD83C\uDDF8", "Europe", "162.159.195.5"),
        Location("it-mil", "Italy", "Milan", "\uD83C\uDDEE\uD83C\uDDF9", "Europe", "162.159.195.6"),
        Location("se-arn", "Sweden", "Stockholm", "\uD83C\uDDF8\uD83C\uDDEA", "Europe", "162.159.195.7"),
        Location("pl-waw", "Poland", "Warsaw", "\uD83C\uDDF5\uD83C\uDDF1", "Europe", "162.159.195.8"),
        Location("ch-zrh", "Switzerland", "Zurich", "\uD83C\uDDE8\uD83C\uDDED", "Europe", "188.114.96.1"),
        Location("at-vie", "Austria", "Vienna", "\uD83C\uDDE6\uD83C\uDDF9", "Europe", "188.114.96.2"),
        Location("ru-dme", "Russia", "Moscow", "\uD83C\uDDF7\uD83C\uDDFA", "Europe", "188.114.96.3"),
        Location("tr-ist", "Turkey", "Istanbul", "\uD83C\uDDF9\uD83C\uDDF7", "Europe", "188.114.96.4"),

        // ---- Middle East & Africa ----
        Location("ae-dxb", "United Arab Emirates", "Dubai", "\uD83C\uDDE6\uD83C\uDDEA", "Middle East", "188.114.97.1"),
        Location("sa-ruh", "Saudi Arabia", "Riyadh", "\uD83C\uDDF8\uD83C\uDDE6", "Middle East", "188.114.97.2"),
        Location("il-tlv", "Israel", "Tel Aviv", "\uD83C\uDDEE\uD83C\uDDF1", "Middle East", "188.114.97.3"),
        Location("za-jnb", "South Africa", "Johannesburg", "\uD83C\uDDFF\uD83C\uDDE6", "Africa", "188.114.97.4"),
        Location("ke-nbo", "Kenya", "Nairobi", "\uD83C\uDDF0\uD83C\uDDEA", "Africa", "188.114.98.1"),
        Location("ng-los", "Nigeria", "Lagos", "\uD83C\uDDF3\uD83C\uDDEC", "Africa", "188.114.98.2"),
        Location("eg-cai", "Egypt", "Cairo", "\uD83C\uDDEA\uD83C\uDDEC", "Africa", "188.114.98.3"),

        // ---- South Asia ----
        Location("pk-khi", "Pakistan", "Karachi", "\uD83C\uDDF5\uD83C\uDDF0", "South Asia", "188.114.98.4"),
        Location("pk-lhe", "Pakistan", "Lahore", "\uD83C\uDDF5\uD83C\uDDF0", "South Asia", "188.114.99.1"),
        Location("pk-isb", "Pakistan", "Islamabad", "\uD83C\uDDF5\uD83C\uDDF0", "South Asia", "188.114.99.2"),
        Location("in-bom", "India", "Mumbai", "\uD83C\uDDEE\uD83C\uDDF3", "South Asia", "188.114.99.3"),
        Location("in-del", "India", "New Delhi", "\uD83C\uDDEE\uD83C\uDDF3", "South Asia", "188.114.99.4"),
        Location("in-maa", "India", "Chennai", "\uD83C\uDDEE\uD83C\uDDF3", "South Asia", "162.159.192.9"),
        Location("bd-dac", "Bangladesh", "Dhaka", "\uD83C\uDDE7\uD83C\uDDE9", "South Asia", "162.159.192.10"),
        Location("lk-cmb", "Sri Lanka", "Colombo", "\uD83C\uDDF1\uD83C\uDDF0", "South Asia", "162.159.192.11"),

        // ---- East & Southeast Asia ----
        Location("sg-sin", "Singapore", "Singapore", "\uD83C\uDDF8\uD83C\uDDEC", "Southeast Asia", "162.159.192.12"),
        Location("id-cgk", "Indonesia", "Jakarta", "\uD83C\uDDEE\uD83C\uDDE9", "Southeast Asia", "162.159.192.13"),
        Location("my-kul", "Malaysia", "Kuala Lumpur", "\uD83C\uDDF2\uD83C\uDDFE", "Southeast Asia", "162.159.192.14"),
        Location("th-bkk", "Thailand", "Bangkok", "\uD83C\uDDF9\uD83C\uDDED", "Southeast Asia", "162.159.192.15"),
        Location("vn-sgn", "Vietnam", "Ho Chi Minh City", "\uD83C\uDDFB\uD83C\uDDF3", "Southeast Asia", "162.159.192.16"),
        Location("ph-mnl", "Philippines", "Manila", "\uD83C\uDDF5\uD83C\uDDED", "Southeast Asia", "162.159.192.17"),
        Location("jp-nrt", "Japan", "Tokyo", "\uD83C\uDDEF\uD83C\uDDF5", "East Asia", "162.159.192.18"),
        Location("jp-kix", "Japan", "Osaka", "\uD83C\uDDEF\uD83C\uDDF5", "East Asia", "162.159.192.19"),
        Location("kr-icn", "South Korea", "Seoul", "\uD83C\uDDF0\uD83C\uDDF7", "East Asia", "162.159.192.20"),
        Location("hk-hkg", "Hong Kong", "Hong Kong", "\uD83C\uDDED\uD83C\uDDF0", "East Asia", "162.159.192.21"),
        Location("tw-tpe", "Taiwan", "Taipei", "\uD83C\uDDF9\uD83C\uDDFC", "East Asia", "162.159.192.22"),

        // ---- Oceania ----
        Location("au-syd", "Australia", "Sydney", "\uD83C\uDDE6\uD83C\uDDFA", "Oceania", "162.159.192.23"),
        Location("au-mel", "Australia", "Melbourne", "\uD83C\uDDE6\uD83C\uDDFA", "Oceania", "162.159.192.24"),
        Location("nz-akl", "New Zealand", "Auckland", "\uD83C\uDDF3\uD83C\uDDFF", "Oceania", "162.159.192.25")
    )

    fun byId(id: String): Location? = all.firstOrNull { it.id == id }

    /** The list the server screen shows: Automatic first, then everything else. */
    fun options(): List<Location> = listOf(auto) + all

    fun regionOf(id: String): String =
        if (id == AUTO_ID) "Auto" else byId(id)?.region ?: "Auto"

    /** Every distinct host in the catalog, used to seed the failover pool. */
    fun hosts(): List<String> = all.map { it.host }.distinct()
}
