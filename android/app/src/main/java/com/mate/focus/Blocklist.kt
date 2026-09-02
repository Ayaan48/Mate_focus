package com.mate.focus

/**
 * Hosts and packages Mate refuses to open.
 *
 * Generated from the shared list in windows/mate/blocklist.py - keep the two
 * in step when you add something.
 */
object Blocklist {

    /** Registrable domains. Matching is suffix based, so subdomains are covered. */
    val ADULT_DOMAINS: Set<String> = setOf(
        "18comix.com", "4tube.com", "8muses.com", "9hentai.to", "adultfriendfinder.com",
        "adulttime.com", "alphaporno.com", "analdin.com", "anysex.com", "ashleymadison.com",
        "asstr.org", "avgle.com", "babes.com", "bangbros.com", "bdsmlr.com", "beeg.com",
        "bitchute-nsfw.com", "blacked.com", "bongacams.com", "boyfriendtv.com", "brazzers.com",
        "cam4.com", "cambro.tv", "camsoda.com", "camster.com", "camvideos.tv", "camwhores.tv",
        "camwhores.video", "chan.sankakucomplex.com", "chaturbate.com", "clips4sale.com",
        "cockyboys.com", "comics-porn.com", "coomer.party", "coomer.su", "danbooru.donmai.us",
        "deeper.com", "definebabe.com", "digitalplayground.com", "drtuber.com", "e-hentai.org",
        "e621.net", "efukt.com", "elegantangel.com", "empflix.com", "eporner.com", "erome.com",
        "evilangel.com", "exhentai.org", "exploitedcollegegirls.com", "extremetube.com",
        "familystrokes.com", "fancentro.com", "fansly.com", "fapello.com", "faphouse.com",
        "fetlife.com", "flirt4free.com", "fux.com", "gayforit.eu", "gaymaletube.com",
        "gelbooru.com", "gifsauce.com", "gotporn.com", "hanime.tv", "hclips.com", "hdzog.com",
        "heavy-r.com", "hentai2read.com", "hentaifox.com", "hentaihaven.xxx", "hentaihere.com",
        "hentairead.com", "highporn.net", "hitomi.la", "hotmovies.com", "hotmovs.com",
        "hqporner.com", "imagefap.com", "imlive.com", "influencersgonewild.com", "iwantclips.com",
        "iwara.tv", "jable.tv", "javdoe.com", "javfinder.la", "javguru.com", "javhd.com",
        "javmost.com", "jerkmate.com", "justfor.fans", "katestube.com", "keezmovies.com",
        "kemono.party", "kemono.su", "leakedzone.com", "literotica.com", "livejasmin.com",
        "loyalfans.com", "luscious.net", "manyvids.com", "megatube.xxx", "men.com", "milftoon.com",
        "missav.com", "modelhub.com", "mofos.com", "motherless.com", "multporn.net",
        "myfreecams.com", "mylf.com", "myreadingmanga.info", "naughtyamerica.com", "netflav.com",
        "nhentai.net", "nhentai.xxx", "nifty.org", "noodlemagazine.com", "notfans.com",
        "nubilefilms.com", "nubiles.net", "nudevista.com", "nuvid.com", "onlyfans.com",
        "perfectgirls.net", "porn.com", "porn3dx.com", "pornbb.org", "pornburst.xxx",
        "porncomixonline.net", "porndig.com", "porndoe.com", "pornerbros.com", "porngo.com",
        "pornhat.com", "pornhd.com", "pornheed.com", "pornhits.com", "pornhub.com", "pornhub.org",
        "pornhubpremium.com", "pornid.xxx", "pornone.com", "pornoreino.com", "pornoxo.com",
        "pornpics.com", "pornpros.com", "pornrabbit.com", "porntn.com", "porntrex.com",
        "pornve.com", "pornzog.com", "pururin.to", "realitykings.com", "recurbate.com",
        "redgifs.com", "redtube.com", "rt.pornhub.com", "rule34.paheal.net", "rule34.xxx",
        "sankakucomplex.com", "scrolller.com", "seancody.com", "seeking.com", "sex.com",
        "sexcamsbay.com", "sexstoriespost.com", "sextb.net", "sextvx.com", "sexvid.xxx", "sexy.com",
        "sexyporn.tv", "shooshtime.com", "simpcity.su", "simply-hentai.com", "slutload.com",
        "smutty.com", "spankbang.com", "spankwire.com", "spicevids.com", "streamate.com",
        "stripchat.com", "sunporno.com", "supjav.com", "sxyprn.com", "teamskeet.com", "thisvid.com",
        "thothub.tv", "thotslife.com", "thumbzilla.com", "tnaflix.com", "tokyomotion.net",
        "tsumino.com", "tube8.com", "tubepornclassic.com", "tushy.com", "twistys.com", "txxx.com",
        "upornia.com", "veporno.com", "vixen.com", "vjav.com", "voyeurhit.com", "vporn.com",
        "watchmygf.me", "watchporn.to", "wetplace.com", "wicked.com", "xasiat.com", "xbabe.com",
        "xgroovy.com", "xhamster.com", "xhamster.desi", "xhamster1.desi", "xlovecam.com",
        "xnxx.com", "xvideos.com", "xxxbunker.com", "yespornplease.com", "youav.com", "youjizz.com",
        "youporn.com", "yourporn.sexy"
    )

    /** Words that betray an adult search even on a site that is not listed. */
    val ADULT_KEYWORDS: List<String> = listOf(
        "porn", "xxx", "hentai", "sexcam", "camgirl", "nsfw", "milf",
        "blowjob", "creampie", "onlyfans", "rule34", "nudes", "escort",
    )

    /** Known adult app package names. */
    val ADULT_PACKAGES: Set<String> = setOf(
        "com.pornhub.app",
        "com.xvideos.app",
        "com.xnxx.app",
        "com.xhamster.app",
        "com.brazzers.app",
        "com.onlyfans.app",
        "com.chaturbate.app",
        "com.stripchat.app",
        "com.bongacams.app",
        "com.adultapp.player",
    )

    /** Browsers whose address bar Mate can read through accessibility. */
    val BROWSERS: Map<String, List<String>> = mapOf(
        "com.android.chrome" to listOf("com.android.chrome:id/url_bar"),
        "com.chrome.beta" to listOf("com.chrome.beta:id/url_bar"),
        "com.chrome.dev" to listOf("com.chrome.dev:id/url_bar"),
        "com.chrome.canary" to listOf("com.chrome.canary:id/url_bar"),
        "com.brave.browser" to listOf("com.brave.browser:id/url_bar"),
        "com.microsoft.emmx" to listOf("com.microsoft.emmx:id/url_bar"),
        "com.opera.browser" to listOf("com.opera.browser:id/url_field"),
        "com.opera.mini.native" to listOf("com.opera.mini.native:id/url_field"),
        "com.sec.android.app.sbrowser" to listOf("com.sec.android.app.sbrowser:id/location_bar_edit_text"),
        "org.mozilla.firefox" to listOf("org.mozilla.firefox:id/mozac_browser_toolbar_url_view"),
        "org.mozilla.fenix" to listOf("org.mozilla.fenix:id/mozac_browser_toolbar_url_view"),
        "com.duckduckgo.mobile.android" to listOf("com.duckduckgo.mobile.android:id/omnibarTextInput"),
        "com.UCMobile.intl" to listOf("com.UCMobile.intl:id/address_bar_text"),
        "com.vivaldi.browser" to listOf("com.vivaldi.browser:id/url_bar"),
    )

    private fun host(url: String): String {
        var value = url.trim().lowercase()
        value = value.substringAfter("://")
        value = value.substringBefore('/').substringBefore('?').substringBefore('#')
        value = value.substringAfterLast('@').substringBefore(':')
        return value.removePrefix("www.").removePrefix("m.")
    }

    /** True when this address should be refused. */
    fun isBlockedUrl(url: String, extra: Collection<String> = emptyList()): Boolean {
        if (url.isBlank()) return false
        val h = host(url)
        if (h.isEmpty() || !h.contains('.')) return matchesKeyword(url)
        val all = ADULT_DOMAINS + extra.map { it.removePrefix("www.").removePrefix("m.") }
        if (all.any { h == it || h.endsWith(".$it") }) return true
        return matchesKeyword(url)
    }

    private fun matchesKeyword(url: String): Boolean {
        val value = url.lowercase()
        return ADULT_KEYWORDS.any { value.contains(it) }
    }
}

/**
 * Decides whether a Settings screen is about to end Mate.
 *
 * Kept free of Android types so it can be exercised directly. The rule is
 * deliberately two-tier: unmistakable actions ("uninstall", "deactivate") fire
 * when the screen names Mate at all, while generic ones ("turn off", "disable")
 * fire only when the screen names Mate precisely - otherwise every toggle in
 * Settings on a phone whose brand contains "mate" would trip the guard.
 */
object SelfDefence {

    /** Actions that only appear where an app is about to be removed or stopped. */
    val STRONG_ACTIONS = listOf(
        "uninstall", "deactivate", "force stop", "force-stop",
        "clear data", "clear storage", "remove app", "delete app",
    )

    /** Actions common all over Settings; only trusted next to an exact marker. */
    val WEAK_ACTIONS = listOf("turn off", "disable")

    /** Strings that can only be Mate. */
    val MARKERS = listOf("com.mate.focus", "mate focus guard", "mate protection")

    /** "mate" as its own word, so automate, estimate and teammate do not count. */
    private val WHOLE_WORD = Regex("(^|[^a-z])mate([^a-z]|$)")

    fun shouldIntercept(texts: List<String>): Boolean {
        val lower = texts.map { it.lowercase() }
        val exact = lower.any { text -> MARKERS.any { text.contains(it) } }
        val named = exact || lower.any { WHOLE_WORD.containsMatchIn(it) }

        val strong = lower.any { text -> STRONG_ACTIONS.any { text.contains(it) } }
        if (strong && named) return true

        val weak = lower.any { text -> WEAK_ACTIONS.any { text.contains(it) } }
        return weak && exact
    }
}
