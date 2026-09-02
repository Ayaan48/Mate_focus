"""Domains Mate blocks at the hosts-file level.

Keep entries as bare registrable domains; hosts.py expands each to the
apex plus the `www.` and `m.` subdomains.
"""
from __future__ import annotations

ADULT_DOMAINS: list[str] = [
    # tube / streaming
    "pornhub.com", "rt.pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com",
    "xhamster.desi", "xhamster1.desi", "redtube.com", "youporn.com", "tube8.com",
    "spankbang.com", "eporner.com", "porntrex.com", "hclips.com", "upornia.com",
    "txxx.com", "hdzog.com", "vjav.com", "voyeurhit.com", "tubepornclassic.com",
    "beeg.com", "tnaflix.com", "empflix.com", "drtuber.com", "nuvid.com",
    "sunporno.com", "porn.com", "porndig.com", "pornone.com", "pornhat.com",
    "porntn.com", "yourporn.sexy", "sxyprn.com", "pornbb.org", "thumbzilla.com",
    "youjizz.com", "motherless.com", "spankwire.com", "keezmovies.com",
    "extremetube.com", "gotporn.com", "pornhd.com", "4tube.com", "fux.com",
    "pornerbros.com", "slutload.com", "hotmovs.com", "analdin.com", "xbabe.com",
    "megatube.xxx", "pornoxo.com", "pornrabbit.com", "watchmygf.me",
    # premium / studio
    "brazzers.com", "realitykings.com", "bangbros.com", "naughtyamerica.com",
    "digitalplayground.com", "mofos.com", "twistys.com", "babes.com",
    "blacked.com", "tushy.com", "vixen.com", "deeper.com", "evilangel.com",
    "adulttime.com", "wicked.com", "elegantangel.com", "pornpros.com",
    "teamskeet.com", "nubiles.net", "nubilefilms.com", "mylf.com",
    "familystrokes.com", "exploitedcollegegirls.com",
    # cams / live
    "chaturbate.com", "stripchat.com", "bongacams.com", "livejasmin.com",
    "cam4.com", "myfreecams.com", "camsoda.com", "flirt4free.com",
    "streamate.com", "imlive.com", "xlovecam.com", "camster.com",
    "jerkmate.com", "sexcamsbay.com", "camwhores.tv", "camwhores.video",
    # creator / paid
    "onlyfans.com", "fansly.com", "manyvids.com", "clips4sale.com",
    "iwantclips.com", "justfor.fans", "fancentro.com", "loyalfans.com",
    "modelhub.com", "pornhubpremium.com",
    # image boards / aggregators / social
    "rule34.xxx", "rule34.paheal.net", "e621.net", "e-hentai.org", "exhentai.org",
    "nhentai.net", "nhentai.xxx", "hanime.tv", "hentaihaven.xxx", "hitomi.la",
    "gelbooru.com", "danbooru.donmai.us", "sankakucomplex.com", "chan.sankakucomplex.com",
    "imagefap.com", "erome.com", "coomer.su", "kemono.su", "kemono.party",
    "coomer.party", "simpcity.su", "fapello.com", "influencersgonewild.com",
    "thothub.tv", "thotslife.com", "leakedzone.com", "notfans.com",
    "porn3dx.com", "multporn.net", "hentai2read.com", "luscious.net",
    "myreadingmanga.info", "tsumino.com", "9hentai.to", "hentaifox.com",
    "simply-hentai.com", "hentaihere.com",
    # story / forum / misc
    "literotica.com", "asstr.org", "bdsmlr.com", "fetlife.com", "adultfriendfinder.com",
    "ashleymadison.com", "seeking.com", "sexstoriespost.com", "nifty.org",
    "hqporner.com", "javhd.com", "javmost.com", "javguru.com", "jable.tv",
    "missav.com", "supjav.com", "avgle.com", "netflav.com", "javdoe.com",
    "iwara.tv", "spicevids.com", "watchporn.to", "noodlemagazine.com",
    "pornzog.com", "sexvid.xxx", "anysex.com", "alphaporno.com", "vporn.com",
    "definebabe.com", "yespornplease.com", "pornve.com", "camvideos.tv",
    "recurbate.com", "cambro.tv", "bitchute-nsfw.com", "sextb.net",
    "18comix.com", "8muses.com", "milftoon.com", "comics-porn.com",
    "porncomixonline.net", "hentairead.com", "pururin.to", "xasiat.com",
    "tokyomotion.net", "javfinder.la", "highporn.net", "porngo.com",
    "pornhits.com", "pornoreino.com", "sexyporn.tv", "shooshtime.com",
    "youav.com", "veporno.com", "perfectgirls.net", "pornburst.xxx",
    "sextvx.com", "faphouse.com", "boyfriendtv.com", "gaymaletube.com",
    "men.com", "seancody.com", "cockyboys.com", "gayforit.eu",
    "thisvid.com", "xgroovy.com", "pornhub.org", "redgifs.com",
    "gifsauce.com", "sexy.com", "nudevista.com", "porndoe.com",
    "pornheed.com", "wetplace.com", "katestube.com", "hotmovies.com",
    "pornid.xxx", "smutty.com", "scrolller.com", "pornpics.com",
    "sex.com", "xxxbunker.com", "heavy-r.com", "efukt.com",
]

# Redirecting these to Google's / Bing's SafeSearch VIPs forces filtered results.
SAFESEARCH_MAP: dict[str, str] = {
    "www.google.com": "216.239.38.120",
    "google.com": "216.239.38.120",
    "www.bing.com": "204.79.197.220",
    "bing.com": "204.79.197.220",
    "duckduckgo.com": "54.204.220.44",
    "www.duckduckgo.com": "54.204.220.44",
    "www.youtube.com": "216.239.38.120",
    "youtube.com": "216.239.38.120",
    "m.youtube.com": "216.239.38.120",
    "youtubei.googleapis.com": "216.239.38.120",
    "youtube.googleapis.com": "216.239.38.120",
    "www.youtube-nocookie.com": "216.239.38.120",
}

# Common social/doomscroll domains offered as a one-click preset.
DOOMSCROLL_PRESET: list[str] = [
    "instagram.com", "facebook.com", "tiktok.com", "x.com", "twitter.com",
    "reddit.com", "snapchat.com", "9gag.com", "pinterest.com", "tumblr.com",
    "twitch.tv", "netflix.com", "primevideo.com", "hotstar.com",
]
